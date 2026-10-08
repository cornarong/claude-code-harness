#!/usr/bin/env bash
# PreToolUse 훅 — follow 모듈의 Java 파일에 user/post 패키지를 직접 import 하는
# Edit/Write/MultiEdit 를 파일이 써지기 전에 막는다. (모듈 간 직접 의존 금지)
# 위반이면 안내를 stderr로 내보내고 exit 2 로 신호한다.
# (PreToolUse 훅이 exit 2 면 도구 호출이 차단되고 stderr 내용이 Claude에게 피드백된다.)
# jq 가 없는 환경에서도 동작하도록 grep/sed 만 사용한다.
set -uo pipefail

input="$(cat)"

# JSON 문자열 값 하나 — 이스케이프(\" 등)를 건너뛰며 닫는 따옴표까지 매칭
json_str='"(\\.|[^"\\])*"'
sep='[[:space:]]*:[[:space:]]*'

# 1) 훅 입력(JSON, stdin)에서 대상 파일 경로 추출 (Windows 역슬래시는 / 로 정규화)
file_path="$(printf '%s' "$input" \
  | grep -oE "\"file_path\"${sep}${json_str}" | head -n 1 \
  | sed -E "s/^\"file_path\"${sep}\"//; s/\"\$//" \
  | sed -e 's/\\\\/\//g')"

# 2) follow 모듈의 Java 파일이 아니면 통과
case "$file_path" in
  */modules/follow/*.java) ;;
  *) exit 0 ;;
esac

# 3) 새로 써질 내용만 추출 — Write 의 content, Edit/MultiEdit 의 new_string.
#    (old_string 은 보지 않는다: 금지된 import 를 지우는 수정은 허용해야 하므로)
new_text="$(printf '%s' "$input" \
  | grep -oE "\"(content|new_string)\"${sep}${json_str}" \
  | sed -E "s/^\"[a-z_]+\"${sep}\"//; s/\"\$//" \
  | sed -e 's/\\[nrt]/\n/g')"

# 4) user/post 패키지 import 탐지 (import static 포함)
violations="$(printf '%s\n' "$new_text" \
  | grep -E '^[[:space:]]*import[[:space:]]+(static[[:space:]]+)?com\.echo\.(user|post)\.' \
  | sed -E 's/^[[:space:]]+//' | sort -u)"

[ -z "$violations" ] && exit 0

{
  printf '❌ 차단: follow 모듈에서 user/post 패키지를 직접 import 할 수 없다. (모듈 간 직접 의존 금지)\n\n'
  printf '대상 파일: %s\n' "$file_path"
  printf '금지된 import:\n'
  printf '%s\n' "$violations" | sed -e 's/^/  - /'
  printf '\n대신 common 모듈을 경유해줘:\n'
  printf '  1. follow 가 필요로 하는 계약(인터페이스)·공유 타입(record)을 common(com.echo.common) 에 정의한다.\n'
  printf '  2. follow 는 com.echo.common.* 만 import 한다. (follow → common 의존은 허용)\n'
  printf '  3. 그 계약의 구현은 해당 타입을 가진 모듈(user/post)에 두고, 조립은 apps/customer-api 가 한다.\n'
  printf '  ※ ID 값(Long userId 등)만 필요하면 import 없이 원시 값으로 주고받으면 된다.\n'
} >&2
exit 2

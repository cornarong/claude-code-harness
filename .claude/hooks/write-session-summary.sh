#!/usr/bin/env bash
# Stop 훅 — Claude 가 응답을 마칠 때마다 이번 세션의 요약을 .claude/session-summary.md 에 덮어쓴다.
# (세션의 마지막 Stop 이 남긴 내용이 곧 "세션 종료 시점" 요약이 된다.)
# 내용: 이번 세션의 요청 목록, Edit/Write 로 바뀐 파일, 마지막 응답, git 작업 트리 상태.
# 세션을 막지 않는다 — 무슨 일이 있어도 조용히 exit 0.
# jq 가 없는 환경에서도 동작하도록 grep/sed 만 사용한다.
set -uo pipefail
export LC_ALL=C.UTF-8 2>/dev/null

input="$(cat)"

# JSON 문자열 값 하나 — 이스케이프(\" 등)를 건너뛰며 닫는 따옴표까지 매칭
json_str='"(\\.|[^"\\])*"'
sep='[[:space:]]*:[[:space:]]*'

# stdin(또는 파일)에서 "key":"value" 의 value 를 원본 이스케이프 그대로 한 줄씩 뽑는다
json_values() { # $1 = key
  grep -oE "\"$1\"${sep}${json_str}" | sed -E "s/^\"$1\"${sep}\"//; s/\"\$//"
}
to_slashes() { sed -e 's/\\\\/\//g'; }
# JSON 이스케이프를 사람이 읽는 글로 (줄바꿈 유지)
unescape() { sed -e 's/\\r//g; s/\\n/\n/g; s/\\t/  /g; s/\\"/"/g; s/\\\\/\\/g'; }
# JSON 이스케이프를 한 줄짜리 글로 (줄바꿈은 공백)
one_line() { sed -e 's/\\r//g; s/\\n/ /g; s/\\t/ /g; s/\\"/"/g; s/\\\\/\\/g' -e 's/  */ /g'; }

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)" || exit 0
out="$PROJECT_ROOT/.claude/session-summary.md"

transcript="$(printf '%s' "$input" | json_values transcript_path | head -n 1 | to_slashes)"
session_id="$(printf '%s' "$input" | json_values session_id | head -n 1)"
[ -n "$transcript" ] && [ -f "$transcript" ] || exit 0

# 1) 이번 세션의 요청 — 사용자가 직접 입력한 프롬프트만 (도구 결과·메타·슬래시 명령 제외)
requests="$(grep -E '"type":"user"' "$transcript" | grep -v '"isMeta":true' \
  | grep -oE "\"message\"${sep}\\{\"role\"${sep}\"user\",\"content\"${sep}${json_str}" \
  | sed -E "s/^.*\"content\"${sep}\"//; s/\"\$//" \
  | grep -v '^<' | one_line \
  | while IFS= read -r line; do
      [ "${#line}" -gt 160 ] && line="${line:0:160}…"
      printf '%s\n' "$line"
    done)"

# 2) 이번 세션에서 Edit/Write 가 실제로 성공한 파일 (프로젝트 안의 것만, 처음 바뀐 순서대로)
#    Write 결과는 {"type":"create|update","filePath":…}, Edit 결과는 {"filePath":…} 로 기록된다.
root_lc="$(printf '%s' "$PROJECT_ROOT" | sed -E 's#^/([a-zA-Z])/#\1:/#' | tr '[:upper:]' '[:lower:]')"
changed="$(grep -oE "\"toolUseResult\"${sep}\\{(\"type\"${sep}\"(create|update)\",)?\"filePath\"${sep}${json_str}" "$transcript" \
  | sed -E "s/^\"toolUseResult\"${sep}\\{//; s/\"\$//" \
  | sed -E "s/^\"type\"${sep}\"create\",\"filePath\"${sep}\"/신규\t/; s/^(\"type\"${sep}\"update\",)?\"filePath\"${sep}\"/수정\t/" \
  | to_slashes \
  | awk -F '\t' -v root="$root_lc/" '
      index(tolower($2), root) == 1 {
        rel = substr($2, length(root) + 1)
        if (!seen[rel]++) print $1 "\t" rel
      }')"

# 바뀐 파일이 없는 세션이면, 다른 세션이 남긴 기존 요약을 지우지 않는다
if [ -z "$changed" ] && [ -f "$out" ] && ! grep -q "session: $session_id" "$out" 2>/dev/null; then
  exit 0
fi

# 3) 마지막 응답 (Stop 입력에 들어오면 사용 — 보통 그 턴에 한 일을 가장 잘 요약한다)
last_reply="$(printf '%s' "$input" | json_values last_assistant_message | head -n 1 | unescape)"
if [ "${#last_reply}" -gt 2000 ]; then
  last_reply="${last_reply:0:2000}
…(이하 생략)"
fi

git_status="$(cd "$PROJECT_ROOT" && git status --short 2>/dev/null)"

{
  printf '# 지난 세션 요약\n\n'
  printf '> Stop 훅(`.claude/hooks/write-session-summary.sh`)이 자동으로 쓴 파일이다. 직접 고치지 말 것 — 다음 응답이 끝나면 덮어쓴다.\n'
  printf '> 새 세션은 이 내용을 이어받아 작업한다. 단, 기록 시점 이후 코드가 바뀌었을 수 있으니 실제 파일을 확인할 것.\n\n'
  printf -- '- 기록 시각: %s\n' "$(date '+%Y-%m-%d %H:%M')"
  printf -- '- 브랜치: %s\n' "$(cd "$PROJECT_ROOT" && git rev-parse --abbrev-ref HEAD 2>/dev/null)"
  printf -- '- session: %s\n\n' "$session_id"

  printf '## 요청한 작업 (시간순)\n\n'
  if [ -n "$requests" ]; then
    printf '%s\n' "$requests" | awk '{ printf "%d. %s\n", NR, $0 }'
  else
    printf '(없음)\n'
  fi

  printf '\n## 이번 세션에서 바뀐 파일\n\n'
  if [ -n "$changed" ]; then
    printf '%s\n' "$changed" | while IFS="$(printf '\t')" read -r kind path; do
      if [ -e "$PROJECT_ROOT/$path" ]; then
        printf -- '- `%s` (%s)\n' "$path" "$kind"
      else
        printf -- '- `%s` (만들었다가 삭제됨)\n' "$path"
      fi
    done
  else
    printf '(없음)\n'
  fi

  if [ -n "$last_reply" ]; then
    printf '\n## 마지막 응답\n\n%s\n' "$last_reply"
  fi

  printf '\n## 커밋되지 않은 변경 (git status)\n\n'
  if [ -n "$git_status" ]; then
    printf '```\n%s\n```\n' "$git_status"
  else
    printf '(깨끗함)\n'
  fi
} > "$out.tmp" 2>/dev/null && mv -f "$out.tmp" "$out" 2>/dev/null

exit 0

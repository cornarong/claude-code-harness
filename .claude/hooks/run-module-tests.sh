#!/usr/bin/env bash
# PostToolUse 훅 — Java 파일이 Edit/Write/MultiEdit로 수정되면
#   (a) 새로 만든 파일인데 짝 테스트가 없으면 경고하고,
#   (b) 그 파일이 속한 모듈의 테스트만 실행한다.
# 성공은 조용히(출력 없음, exit 0). 문제가 있을 때만 stderr로 내보내고 exit 2 로 신호한다.
# (PostToolUse 훅이 exit 2 면 stderr 내용이 Claude에게 피드백되어 자동 수정을 유도한다.)
# jq 가 없는 환경에서도 동작하도록 grep/sed 만 사용한다.
set -uo pipefail

input="$(cat)"

# JSON 문자열 값 하나 — 이스케이프(\" 등)를 건너뛰며 닫는 따옴표까지 매칭
json_str='"(\\.|[^"\\])*"'
sep='[[:space:]]*:[[:space:]]*'

# 1) 훅 입력(JSON, stdin)에서 수정된 파일 경로 추출 (Windows 역슬래시는 / 로 정규화)
file_path="$(printf '%s' "$input" \
  | grep -oE "\"file_path\"${sep}${json_str}" | head -n 1 \
  | sed -E "s/^\"file_path\"${sep}\"//; s/\"\$//" \
  | sed -e 's/\\\\/\//g')"

# 경로가 없으면(해당 없음) 조용히 통과
[ -z "$file_path" ] && exit 0

# 2) Java 파일이 아니면 통과 (build.gradle.kts, 설정 파일 등은 무시)
case "$file_path" in
  *.java) ;;
  *) exit 0 ;;
esac

problems=""

# 3) 새로 만든 프로덕션 Java 파일이면 짝 테스트가 있는지 확인
#    (Write 가 파일을 새로 만들면 tool_response.type 이 "create" 로 온다)
if printf '%s' "$input" | grep -qE "\"tool_response\"${sep}\\{[[:space:]]*\"type\"${sep}\"create\""; then
  case "$file_path" in
    */src/main/java/*)
      module_dir="${file_path%%/src/main/java/*}"
      rel="${file_path#*/src/main/java/}"
      name="$(basename "$file_path" .java)"

      # 테스트 대상이 아닌 것은 건너뛴다: 예외·부트스트랩 클래스, interface/record/enum(DTO·리포지토리 등)
      skip=""
      case "$name" in
        *Exception|*Application|package-info|module-info) skip=1 ;;
      esac
      if [ -z "$skip" ] && grep -qE '^[[:space:]]*(public[[:space:]]+)?(sealed[[:space:]]+)?(@?interface|record|enum)[[:space:]]' "$file_path" 2>/dev/null; then
        skip=1
      fi

      if [ -z "$skip" ]; then
        # 짝 테스트: 같은 모듈 src/test 아래의 {Name}Test / {Name}IntegrationTest,
        # 컨트롤러는 {Feature}IntegrationTest 도 인정 (FollowController → FollowIntegrationTest)
        found=""
        if [ -d "$module_dir/src/test" ]; then
          found="$(find "$module_dir/src/test" -type f \
            \( -name "${name}Test.java" -o -name "${name}IntegrationTest.java" \
               -o -name "${name%Controller}IntegrationTest.java" \) 2>/dev/null | head -n 1)"
        fi
        if [ -z "$found" ]; then
          problems="$(printf '⚠️ 짝 테스트 없음 — %s 를 새로 만들었는데 테스트가 없다.\n   테스트를 추가해줘: %s/src/test/java/%s' \
            "$file_path" "$module_dir" "${rel%.java}Test.java")"
        fi
      fi
      ;;
  esac
fi

# 4) 파일 경로로 모듈(Gradle 테스트 태스크)을 판별
task=""
case "$file_path" in
  */modules/post/*)      task=":modules:post:test" ;;
  */modules/timeline/*)  task=":modules:timeline:test" ;;
  */modules/user/*)      task=":modules:user:test" ;;
  */modules/follow/*)    task=":modules:follow:test" ;;
  */apps/customer-api/*) task=":apps:customer-api:test" ;;
esac

# 5) 프로젝트 루트에서 해당 모듈 테스트만 실행 (매핑되는 모듈이 없으면 건너뜀)
if [ -n "$task" ]; then
  PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
  if cd "$PROJECT_ROOT"; then
    output="$(./gradlew "$task" 2>&1)"
    status=$?
    if [ "$status" -ne 0 ]; then
      failure="$(printf '%s\n\n❌ %s 실패 — 위 테스트 오류를 확인하고 코드를 고쳐줘.' "$output" "$task")"
      problems="${problems:+$problems

}$failure"
    fi
  fi
fi

# 6) 문제가 없으면 조용히 끝낸다
[ -z "$problems" ] && exit 0

printf '%s\n' "$problems" >&2
exit 2

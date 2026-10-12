@echo off
rem echo-db MCP server launcher (Windows).
rem Reads DB connection values from the project root .env (git-ignored)
rem so that no password is stored in .mcp.json.
setlocal
set "ENV_FILE=%~dp0..\..\.env"
if not exist "%ENV_FILE%" (
  echo [echo-db] .env not found: %ENV_FILE% 1>&2
  exit /b 1
)
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ENV_FILE%") do set "%%a=%%b"
if not defined POSTGRES_PASSWORD (
  echo [echo-db] POSTGRES_PASSWORD is not set in .env 1>&2
  exit /b 1
)
if not defined POSTGRES_USER set "POSTGRES_USER=echo"
if not defined POSTGRES_DB set "POSTGRES_DB=echo"
if not defined POSTGRES_PORT set "POSTGRES_PORT=5432"
rem Fall back to the default Node.js install dir when npx is not on PATH
rem (e.g. the parent process started before Node.js was installed).
where npx >nul 2>&1 || set "PATH=%ProgramFiles%\nodejs;%PATH%"
where npx >nul 2>&1 || (
  echo [echo-db] npx not found. Install Node.js LTS. 1>&2
  exit /b 1
)
npx -y @modelcontextprotocol/server-postgres postgresql://%POSTGRES_USER%:%POSTGRES_PASSWORD%@localhost:%POSTGRES_PORT%/%POSTGRES_DB%

#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER="$ROOT/gradle/wrapper/gradle-wrapper.jar"
EXPECTED='2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046'
URL='https://raw.githubusercontent.com/gradle/gradle/v8.10.2/gradle/wrapper/gradle-wrapper.jar'

checksum() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d ' ' -f 1
  else
    shasum -a 256 "$1" | cut -d ' ' -f 1
  fi
}

if [ -f "$WRAPPER" ] && [ "$(checksum "$WRAPPER")" != "$EXPECTED" ]; then
  echo 'Wrapper JAR existant invalide : remplacement.' >&2
  rm -f "$WRAPPER"
fi
if [ ! -f "$WRAPPER" ]; then
  echo 'Installation du wrapper Gradle 8.10.2...'
  TEMP="$WRAPPER.download"
  trap 'rm -f "$TEMP"' EXIT HUP INT TERM
  curl --fail --location --silent --show-error --user-agent agent-local \
    "$URL" -o "$TEMP"
  ACTUAL="$(checksum "$TEMP")"
  if [ "$ACTUAL" != "$EXPECTED" ]; then
    echo "SHA-256 inattendu pour gradle-wrapper.jar: $ACTUAL" >&2
    exit 1
  fi
  mv -f "$TEMP" "$WRAPPER"
  trap - EXIT HUP INT TERM
  echo 'Wrapper Gradle verifie.'
fi

if [ -n "${JAVA_HOME:-}" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi
exec "$JAVA" -cp "$WRAPPER" org.gradle.wrapper.GradleWrapperMain "$@"

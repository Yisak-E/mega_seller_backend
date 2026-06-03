#!/bin/sh
# Mega Seller Maven Wrapper
# Falls back to system 'mvn' if wrapper jar is unavailable.
# On most developer machines, 'mvn' is on the PATH.

set -e
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

if command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
elif [ -f "$BASE_DIR/.mvn/wrapper/maven-wrapper.jar" ] && command -v java >/dev/null 2>&1; then
    exec java -classpath "$BASE_DIR/.mvn/wrapper/maven-wrapper.jar" \
        -Dmaven.multiModuleProjectDirectory="$BASE_DIR" \
        org.apache.maven.wrapper.MavenWrapperMain "$@"
else
    echo "ERROR: Maven not found. Install Maven 3.9+ or add it to your PATH."
    echo "  Download: https://maven.apache.org/download.cgi"
    exit 1
fi

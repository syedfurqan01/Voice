#!/bin/sh
# Gradle start-up script (simplified for CI; full wrapper binary will be generated on first run if needed)
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"

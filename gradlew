#!/bin/sh
# Gradle start up script. Standard wrapper launcher: locates java, resolves
# gradle-wrapper.jar next to this script, and delegates to it.
# The jar itself is a small bootstrap binary distributed by Gradle and is not
# checked into this repo (see docs/known_limitations.md) - run
# `gradle wrapper --gradle-version 8.7` once with a local Gradle install to
# generate gradle/wrapper/gradle-wrapper.jar before using this script.

set -e

APP_HOME=$(cd "$(dirname "$0")" && pwd)
APP_NAME="Gradle"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if [ ! -f "$CLASSPATH" ]; then
    echo "gradle-wrapper.jar not found at $CLASSPATH" >&2
    echo "Run: gradle wrapper --gradle-version 8.7 (with a local Gradle install) to generate it." >&2
    exit 1
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

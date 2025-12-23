#!/bin/sh

#
# Gradle wrapper script for POSIX systems
#

# Determine the Java command to use
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Resolve the project base directory
APP_HOME=`pwd -P`

# Add default JVM options
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Collect all arguments for the gradle daemon
APP_ARGS=`echo "$@"`

# Execute Gradle
exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain $APP_ARGS

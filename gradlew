#!/bin/sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or at least 512M, for a 64-bit JVM.
if [ -n "$JAVA_HOME" ] ; then
    case "`\"$JAVA_HOME/bin/java\" -version 2>&1`" in
      *64-Bit*)
        DEFAULT_JVM_OPTS='"-Xmx512m" "-Xms512m"'
        ;;
    esac
fi

# Warn if no JAVA_HOME is set
if [ -z "$JAVA_HOME" ] ; then
    echo "Warning: JAVA_HOME is not set. Using java from PATH."
fi

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        echo "Error: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
        echo "Please set the JAVA_HOME variable in your environment to match the"
        echo "location of your Java installation."
        exit 1
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || (echo "Error: java not found in PATH. Please install Java." && exit 1)
fi

# Collect all arguments for the java command, properly handling white space
set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

# Execute Gradle
exec "$JAVACMD" "$@"

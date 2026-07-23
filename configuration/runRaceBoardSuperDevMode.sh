#!/usr/bin/env bash
set -euo pipefail

USER_HOME=${HOME:?HOME must be set}
PROJECT_HOME=${PROJECT_HOME:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}
SAILING_BUILD_CACHE=${SAILING_BUILD_CACHE:-/Volumes/INSTALL/sailing-build-cache}
JAVA_HOME=${JAVA_HOME:-$USER_HOME/.asdf/installs/java/sapmachine-18}
MAVEN_HOME=${MAVEN_HOME:-$USER_HOME/.asdf/installs/maven/3.9.1}
CODE_SERVER_HOST=${CODE_SERVER_HOST:-127.0.0.1}
CODE_SERVER_PORT=${CODE_SERVER_PORT:-9876}
GWT_JVM_ARGS=${GWT_JVM_ARGS:--Xmx4g -Dgwt.rpc.version=9 -Djava.io.tmpdir=$SAILING_BUILD_CACHE/tmp}

fail_patched_gwt_release() {
  echo "Missing SAP-patched GWT release in $HOME/.m2/repository; install the SAP-patched GWT 2.12.4 release before running this wrapper." >&2
  exit 1
}

[[ -d "$SAILING_BUILD_CACHE" ]] || { echo "Missing build cache: $SAILING_BUILD_CACHE" >&2; exit 1; }
[[ -d "$PROJECT_HOME/java/com.sap.sailing.gwt.ui" ]] || { echo "Missing Sailing checkout: $PROJECT_HOME" >&2; exit 1; }
[[ -x "$JAVA_HOME/bin/java" ]] || { echo "Missing Java: $JAVA_HOME/bin/java" >&2; exit 1; }
[[ -x "$MAVEN_HOME/bin/mvn" ]] || { echo "Missing Maven: $MAVEN_HOME/bin/mvn" >&2; exit 1; }

export HOME="$SAILING_BUILD_CACHE/home"
export TMP="$SAILING_BUILD_CACHE/tmp"
export TMPDIR="$TMP/"
export JAVA_HOME MAVEN_HOME
export JAVA_TOOL_OPTIONS="-Duser.home=$HOME${JAVA_TOOL_OPTIONS:+ $JAVA_TOOL_OPTIONS}"
export PATH="$MAVEN_HOME/bin:$JAVA_HOME/bin:$PATH"
mkdir -p "$HOME" "$TMP"

PLUGIN_JAR="$HOME/.m2/repository/org/codehaus/mojo/gwt-maven-plugin/2.12.4/gwt-maven-plugin-2.12.4.jar"
GWT_USER_JAR="$HOME/.m2/repository/org/gwtproject/gwt-user/2.12.4/gwt-user-2.12.4.jar"
[[ -f "$PLUGIN_JAR" ]] || fail_patched_gwt_release
[[ -f "$GWT_USER_JAR" ]] || fail_patched_gwt_release
"$JAVA_HOME/bin/jar" tf "$GWT_USER_JAR" | grep -Fxq 'com/google/gwt/user/server/rpc/TeeWriter.class' || fail_patched_gwt_release

cd "$PROJECT_HOME/java/com.sap.sailing.gwt.ui"
exec "$MAVEN_HOME/bin/mvn" \
  org.codehaus.mojo:gwt-maven-plugin:2.12.4:run-codeserver \
  -Dgwt.module=com.sap.sailing.gwt.ui.RaceBoard \
  -Dgwt.bindAddress="$CODE_SERVER_HOST" \
  -Dgwt.codeServerPort="$CODE_SERVER_PORT" \
  -Dgwt.style=PRETTY \
  -Dgwt.compiler.methodNameDisplayMode=FULL \
  -Dgwt.extraJvmArgs="$GWT_JVM_ARGS"

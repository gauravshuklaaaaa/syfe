#!/bin/sh
# Maven Wrapper Script
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.

if [ -z "$JAVA_HOME" ] ; then
  JAVACMD=`which java`
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(pwd)}"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

DOWNLOAD_URL=$(grep distributionUrl "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2)
MAVEN_DIST_DIR="$HOME/.m2/wrapper/dists"

DIST_NAME=$(basename "$DOWNLOAD_URL" .zip)
DIST_PATH="$MAVEN_DIST_DIR/$DIST_NAME"

if [ ! -d "$DIST_PATH" ]; then
  echo "Downloading Maven from $DOWNLOAD_URL"
  mkdir -p "$DIST_PATH"
  TMP_DOWNLOAD="$DIST_PATH/download.zip"
  if command -v wget > /dev/null; then
    wget -q "$DOWNLOAD_URL" -O "$TMP_DOWNLOAD"
  elif command -v curl > /dev/null; then
    curl -sL "$DOWNLOAD_URL" -o "$TMP_DOWNLOAD"
  else
    echo "ERROR: Neither wget nor curl is available."
    exit 1
  fi
  unzip -q "$TMP_DOWNLOAD" -d "$DIST_PATH"
  rm -f "$TMP_DOWNLOAD"
fi

MVN_BIN=$(find "$DIST_PATH" -name "mvn" -type f | head -1)

if [ -z "$MVN_BIN" ]; then
  echo "ERROR: Could not find mvn binary in $DIST_PATH"
  exit 1
fi

exec "$MVN_BIN" "$@"

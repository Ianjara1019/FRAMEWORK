#!/bin/bash

set -e

TOMCAT_LIB="/home/itu/tomcat/lib/servlet-api.jar"
JACKSON_DATABIND="/home/itu/.m2/repository/tools/jackson/core/jackson-databind/3.1.4/jackson-databind-3.1.4.jar"
JACKSON_CORE="/home/itu/.m2/repository/tools/jackson/core/jackson-core/3.1.4/jackson-core-3.1.4.jar"
JACKSON_ANNOTATIONS="/home/itu/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.21/jackson-annotations-2.21.jar"

echo "=== [FRAMEWORK] 1. Nettoyage ==="
rm -rf bin
rm -f framework.jar
mkdir -p bin

echo "=== [FRAMEWORK] 2. Compilation ==="
find src -name "*.java" > sources.txt
javac -parameters -cp "$TOMCAT_LIB:$JACKSON_DATABIND:$JACKSON_CORE:$JACKSON_ANNOTATIONS" -d bin @sources.txt
rm sources.txt

echo "=== [FRAMEWORK] 3. Création du JAR ==="
jar cf framework.jar -C bin .
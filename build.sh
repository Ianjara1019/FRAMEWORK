#!/bin/bash

set -e

TOMCAT_LIB="/home/itu/apache-tomcat-10.1.34/lib/servlet-api.jar"

echo "=== [FRAMEWORK] 1. Nettoyage ==="
rm -rf bin
rm -f framework.jar
mkdir -p bin

echo "=== [FRAMEWORK] 2. Compilation ==="
find src -name "*.java" > sources.txt
javac -cp "$TOMCAT_LIB" -d bin @sources.txt
rm sources.txt

echo "=== [FRAMEWORK] 3. Création du JAR ==="
jar cf framework.jar -C bin .
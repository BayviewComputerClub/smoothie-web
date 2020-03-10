#!/bin/bash

mkdir -p test
./gradlew build
mv build/libs/smoothie-web* test/smoothie-web.jar
cd test
java -jar $1 smoothie-web.jar

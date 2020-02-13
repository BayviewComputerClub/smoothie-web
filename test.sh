#!/bin/sh

mkdir test
./gradlew build &&
cd test &&
mv ../build/libs/smoothie-web* ./smoothie-web.jar &&
java -jar smoothie-web.jar # --debug
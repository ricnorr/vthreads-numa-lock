#!/bin/bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk use java 19.0.2-open
cd bench
gradle shadowJar
cp ./build/libs/bench-1.0-SNAPSHOT-all.jar ../fatjar/bench.jar
cd ..
git add fatjar/bench.jar
git add bench/settings/settings.json
git commit -m "Build fatjar"
git push
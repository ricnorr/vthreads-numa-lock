gradle shadowJar
cp ./build/libs/NUMA-aware-locks-1.0-SNAPSHOT-all.jar ./fatjar/NUMA-aware-locks.jar
git add fatjar/NUMA-aware-locks.jar
git add settings/settings.json
git commit -m "Build fatjar"
git push
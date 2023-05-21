# run from NUMA-aware-locks directory
#!/bin/bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
DIR=${CONDA_PREFIX:-$HOME/miniconda3}
CONDA_PROFILE=$DIR/etc/profile.d/conda.sh
source "$CONDA_PROFILE"
conda activate pip3
sdk use java 19.0.2-open

cp bench/settings/settings.json fatjar/settings/settings.json
cd fatjar
java -XX:+UseParallelGC -XX:+UseNUMA -XX:-RestrictContended --add-opens java.base/java.lang=ALL-UNNAMED  --enable-preview -Djdk.trackAllThreads=true -Djdk.tracePinnedThreads=true  -Djna.library.path=../libs/ -jar bench.jar
cp ./results/benchmark_results.csv ../results/benchmark_results.csv
rm -rf ./results/benchmark_results.csv
cd ../scripts/
python3 picture_creator
cd ..

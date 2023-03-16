if [ `uname -s` == "Darwin"  ]; then
      LINK="async-profiler-2.9-macos.zip"
elif [ `uname -s` == "Linux"  ] && [ `uname -m` == "aarch64"  ]; then
      LINK="async-profiler-2.9-linux-arm64.tar.gz"
elif [ `uname -s` == "Linux"  ] && [ `uname -m` == "x86_64"  ]; then
      LINK="async-profiler-2.9-linux-x64.tar.gz"
fi
wget https://github.com/jvm-profiling-tools/async-profiler/releases/download/v2.9/$LINK
tar -xvzf async-profiler-2.9-linux-arm64.tar.gz -C ~
rm -rf async-profiler-2.9-linux-arm64.tar.gz

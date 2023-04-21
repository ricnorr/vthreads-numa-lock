curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk version
sdk install gradle 7.6
sdk install java 19.0.2-open
sdk default gradle 7.6
sdk default java 19.0.2-open
cd ~
if [[ "$1" == http ]]; then
git clone https://github.com/ricnorr/jdk19.git
else
git clone git@github.com:ricnorr/jdk19.git
fi
cd ~/jdk19
git checkout enhancement
bash configure --disable-warnings-as-errors
make images
if [ `uname -s` == "Darwin"  ] && [ `uname -m` == "x86_64"  ]; then
  INST="macosx-x86_64-server-release"
elif [ `uname -s` == "Darwin"  ] && [ `uname -m` == "arm64"  ]; then
  INST="macosx-aarch64-server-release"
elif [ `uname -s` == "Linux"  ] && [ `uname -m` == "aarch64"  ]; then
  INST="linux-aarch64-server-release"
elif [ `uname -s` == "Linux"  ] && [ `uname -m` == "x86_64"  ]; then
  INST="linux-x86_64-server-server-release"
fi
sdk uninstall java custom-19
sdk install java custom-19 ~/jdk19/build/$INST/jdk/


curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk version
sdk install gradle 7.6
sdk install java 19.0.2-open
sdk default gradle 7.6
sdk default java 19.0.2-open
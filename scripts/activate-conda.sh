#!/bin/bash -x
# Copyright (C) 2017-2018 Intel Corporation
#
# SPDX-License-Identifier: MIT

DIR=${CONDA_PREFIX:-$HOME/miniconda3}
CONDA_PROFILE=$DIR/etc/profile.d/conda.sh
[ -f $CONDA_PROFILE ] || { # install it
    if [ `uname -s` == "Darwin"  ] && [ `uname -m` == "x86_64"  ]; then
      INST="Miniconda3-latest-MacOSX-x86_64.sh"
    elif [ `uname -s` == "Darwin"  ] && [ `uname -m` == "arm64"  ]; then
        INST="Miniconda3-latest-MacOSX-arm64.sh"
    elif [ `uname -s` == "Linux"  ] && [ `uname -m` == "aarch64"  ]; then
      INST="Miniconda3-latest-Linux-aarch64.sh"
    elif [ `uname -s` == "Linux"  ] && [ `uname -m` == "x86_64"  ]; then
      INST="Miniconda3-latest-Linux-x86_64.sh"
    fi
    mkdir -p $DIR; cd $DIR/..
    [ -f $INST ] || curl -O https://repo.anaconda.com/miniconda/$INST
    bash $INST -b -p $DIR -f
    unset INST; cd -
    [ -x $CONDA ] || exit 1
}
[ x$DIR/bin/conda == x`which conda` ] || { # initialize
    . $CONDA_PROFILE
}
[ x$DIR/bin/python == x`which python` ] || exit 1 # check


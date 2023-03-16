#!/bin/bash -x
# Copyright (C) 2017-2018 Intel Corporation
#
# SPDX-License-Identifier: MIT

DIR=${CONDA_PREFIX:-$HOME/miniconda3}
CONDA_PROFILE=$DIR/etc/profile.d/conda.sh

source "$CONDA_PROFILE"
conda activate base

# Input arguments
envs=${RUNENVS:-`cd conda-configs; ls *_env.yml`}         # Python versions to install, 3 is default

[ -x "$CONDA_EXE" -a -d "$CONDA_PREFIX" ] || exit 1       # Sanity check

# Update conda package itself
$CONDA_EXE update -n base -c defaults -y conda

# For all channels, all python versions, install all packages
for e in $envs; do
    $CONDA_EXE env create --force -f conda-configs/$e* || exit 1
done

# run from NUMA-aware-locks directory
#!/bin/bash
DIR=${CONDA_PREFIX:-$HOME/miniconda3}
CONDA_PROFILE=$DIR/etc/profile.d/conda.sh

source "$CONDA_PROFILE"
conda activate pip3
gradle run
cd scripts
python3 picture_creator
cd ..

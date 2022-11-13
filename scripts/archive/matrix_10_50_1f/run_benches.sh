#!/bin/sh
for thread in 1 2 4 8 16 32
do
  echo $thread
  ../gradle/gradle-7.3/bin/gradle jmh -Pthread=$thread -PfileName=matrix_10_50_1f_ -Pinclude=MatrixMultiplicationBenchmark -PlockType=REENTRANT,MCS,TEST_SET,TEST_TEST_SET,TICKET -Pfork=1 -PinSectionMatrixSize=10 -PafterSectionMatrixSize=50
done
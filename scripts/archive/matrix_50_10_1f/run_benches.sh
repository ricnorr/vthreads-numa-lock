#!/bin/sh
for thread in 1 2 4 8 16 32
do
  echo $thread
  ../gradle/gradle-7.3/bin/gradle jmh -Pthread=$thread -PfileName=matrix_50_10_1f_ -Pinclude=MatrixMultiplicationBenchmark -PlockType=REENTRANT,MCS,TEST_SET,TEST_TEST_SET,TICKET -Pfork=1 -PinSectionMatrixSize=50 -PafterSectionMatrixSize=10
done
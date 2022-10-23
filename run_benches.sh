#!/bin/sh
for thread in 32
do
  echo $thread
  gradle jmh -Pthread=$thread -Pprofiler=perfnorm
done
# NUMA-aware-locks
Repository for my bachelor's diploma, implementing and benchmarking NUMA aware locks.
# Install instruments
* download openjdk 17 (```sudo apt-get install openjdk-17-jdk```)
* set JAVA_HOME
* download gradle 7.3 
```
  VERSION=7.3  
  wget https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip -P /tmp
  sudo unzip -d /opt/gradle /tmp/gradle-${VERSION}-bin.zip
  ```
* ```gradle wrapper``` in project dir
* install python libraries
  * sudo apt-get install python3-pip python3-setuptools python3-dev python3-wheel
  * pip3 install -vv wheel
  * pip3 install -vv numpy
  * pip3 install -vv pandas
  * pip3 install -vv matplotlib
  * pip3 install -vv pillow
# How to run benchmarks
* ```run_bench.sh``` from the project's root
* Results are saved in ***results/benchmark_results.csv***
* Pictures saved in ***results/pictures***
* Pdf saved in ***results/result.pdf***
# How to run jcstress
* ```gradle jcstress``` to run all tests
* ```gradle jcstress --tests JcStressMCS|SomeAnotherStress``` to run specific tests

[comment]: <> (# How to enable perfnorm)

[comment]: <> (* ```sudo sysctl -w kernel.perf_event_paranoid=1```)
## Useful links
https://github.com/Valloric/jmh-playground
https://github.com/openjdk/jmh
https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples
https://github.com/deepu105/java-loom-benchmarks/blob/main/src/main/java/org/sample/LoomBenchmark.java
https://mail.openjdk.org/pipermail/jmh-dev/2015-May/001901.html
https://github.com/openjdk/jcstress  
https://www.javacodegeeks.com/2016/04/jlbh-introducing-java-latency-benchmarking-harness.html  
http://psy-lob-saw.blogspot.com/2013/05/using-jmh-to-benchmark-multi-threaded.html  
http://psy-lob-saw.blogspot.com/2013/12/jaq-spsc-latency-benchmarks1.html
https://hal.inria.fr/tel-01263203v2/document  
https://www.oracle.com/technical-resources/articles/java/architect-benchmarking.html  
https://shipilev.net/talks/jvmls-July2013-benchmarking.pdf  
https://www.baeldung.com/java-testing-multithreaded  



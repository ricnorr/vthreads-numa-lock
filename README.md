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
# How to run benchmarks
* ```./run_benches.sh```, you can edit number of threads in script cycle. It runs benchmarks on fixed number of threads.
* Results are saved in ***build/results/jmh/threads/{threads_cnt}/results.csv***
# How to run jcstress
* ```gradle jcstress``` to run all tests
* ```gradle jcstress --tests JcStressMCS|SomeAnotherStress``` to run specific tests
# How to enable perfnorm
* ```sudo sysctl -w kernel.perf_event_paranoid=1```
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



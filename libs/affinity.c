#define _GNU_SOURCE

#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <pthread.h>


int pinToCore(int cpuId) {
  pthread_t current_thread = pthread_self();
  //int num_cores = sysconf(_SC_NPROCESSORS_ONLN);
  cpu_set_t cpuset;
  CPU_ZERO(&cpuset);
  CPU_SET(cpuId, &cpuset);
  return pthread_setaffinity_np(current_thread, sizeof(cpu_set_t), &cpuset);
}

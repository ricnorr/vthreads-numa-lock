package ru.ricnorr.benchmarks;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    public static void main(String[] args) {
        final Lock lock = new ReentrantLock();

        Random random = new Random();
        int[][] matrixA = new int[300][300];
        for (int i = 0; i < matrixA.length; i++) {
            for (int j = 0; j < matrixA[0].length; j++) {
                matrixA[i][j] = random.nextInt();
            }
        }

        int[][] matrixB = new int[300][300];
        for (int i = 0; i < matrixB.length; i++) {
            for (int j = 0; j < matrixB[0].length; j++) {
                matrixB[i][j] = random.nextInt();
            }
        }

        class MyRunnable implements Runnable {

            @Override
            public void run() {
                lock.lock();
                int[][] res = new int[matrixA.length][matrixB[0].length];
                for (int i = 0; i < matrixA.length; i++) {
                    for (int j = 0; j < matrixB[0].length; j++) {
                        for (int k = 0; k < matrixA[0].length; k++) {
                            res[i][j] = matrixA[i][k] * matrixB[k][j];
                        }
                    }
                }
                lock.unlock();
            }
        }
        var bechmarkResult = new Benchmark().benchmark(4, new MyRunnable(), 10000, 2, 0, 99);
        System.out.println("Throughput: " + bechmarkResult.throughput());
        System.out.println("Latency: " + bechmarkResult.latency());
    }
}

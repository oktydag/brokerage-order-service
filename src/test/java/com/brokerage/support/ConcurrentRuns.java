package com.brokerage.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ConcurrentRuns {

    private ConcurrentRuns() {
    }

    public static <T> List<Outcome<T>> race(int threads, Callable<T> action) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startLine = new CountDownLatch(1);
        List<Future<Outcome<T>>> futures = new ArrayList<>(threads);
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    startLine.await();
                    try {
                        return new Outcome<>(action.call(), null);
                    } catch (Exception e) {
                        return new Outcome<T>(null, e);
                    }
                }));
            }
            startLine.countDown();
            pool.shutdown();
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("concurrent run did not finish in time");
            }
            List<Outcome<T>> outcomes = new ArrayList<>(threads);
            for (Future<Outcome<T>> future : futures) {
                outcomes.add(future.get());
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    public record Outcome<T>(T value, Exception failure) {

        public boolean succeeded() {
            return failure == null;
        }
    }
}

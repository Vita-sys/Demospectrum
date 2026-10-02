package ru.miet.demospectrum.service;

import java.util.ArrayList;
import java.util.List;

public class OperationLogger {

    private final String operationName;
    private final List<Long> times = new ArrayList<>();
    private long totalTime;
    private long startTime;

    public OperationLogger(String operationName) {
        this.operationName = operationName;
    }

    public void start() {
        startTime = System.nanoTime();
    }

    public void stop() {
        long elapsed = System.nanoTime() - startTime;
        times.add(elapsed);
        totalTime += elapsed;
    }

    public void stopAndLog(int itemId) {
        long elapsed = System.nanoTime() - startTime;
        times.add(elapsed);
        totalTime += elapsed;
        System.out.printf("%s, ID = %d, %d%n", operationName, itemId, elapsed);
    }

    public void printSummary() {
        System.out.println(operationName + "TotalCount = " + times.size());
        System.out.println(operationName + "TotalTime = " + totalTime);
        if (!times.isEmpty()) {
            System.out.println(operationName + "MedianTime = " + (totalTime / times.size()));
        }
    }

    public int getCount() {
        return times.size();
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getMedianTime() {
        return times.isEmpty() ? 0 : totalTime / times.size();
    }

    public static void main(String[] args) {
        OperationLogger logger = new OperationLogger("add");

        for (int i = 1; i <= 10; i++) {
            logger.start();
            // имитация работы — задержка 1 мс
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logger.stopAndLog(i);
        }

        logger.printSummary();
    }

}
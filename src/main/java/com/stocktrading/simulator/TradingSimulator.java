package com.stocktrading.simulator;

import com.stocktrading.engine.TradingEngine;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulates stock trading by generating random orders.
 * Used to test the trading engine under load and verify its functionality.
 */
public class TradingSimulator {
    private static final Logger logger = LoggerFactory.getLogger(TradingSimulator.class);
    private final TradingEngine engine;
    private final Random random = new Random();
    private final String[] tickers;

    /**
     * Constructor
     *
     * @param engine the trading engine to use
     * @param tickerCount the number of different ticker symbols to simulate
     */
    public TradingSimulator(TradingEngine engine, int tickerCount) {
        this.engine = engine;
        this.tickers = generateTickers(tickerCount);
    }

    /**
     * Generates an array of ticker symbols.
     *
     * @param count the number of ticker symbols to generate
     * @return an array of ticker symbols
     */
    private String[] generateTickers(int count) {
        String[] result = new String[Math.min(count, 1024)];
        for (int i = 0; i < result.length; i++) {
            result[i] = "STOCK" + i;
        }
        return result;
    }

    /**
     * Simulates trading by generating random buy and sell orders.
     * Uses multiple threads to simulate concurrent order submission.
     *
     * @param count the number of orders to generate
     * @param threadCount the number of threads to use
     * @throws InterruptedException if interrupted while waiting for completion
     */
    public void simulateTrades(int count, int threadCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        logger.info("Starting simulation with {} orders on {} threads", count, threadCount);

        for (int i = 0; i < count; i++) {
            final int orderIndex = i;
            executor.submit(() -> {
                try {
                    // Generate random order parameters
                    boolean isBuy = random.nextBoolean();
                    String ticker = tickers[random.nextInt(tickers.length)];
                    int quantity = 100 * (random.nextInt(10) + 1); // 100-1000
                    double price = 10.0 + random.nextDouble() * 90.0; // 10.0-100.0

                    // Add the order
                    engine.addOrder(isBuy, ticker, quantity, price);

                    // Log progress periodically
                    if (orderIndex % 1000 == 0) {
                        logger.debug("Generated {} orders", orderIndex);
                    }
                } catch (Exception e) {
                    logger.error("Error generating order", e);
                }
            });
        }

        // Wait for all orders to complete
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        logger.info("Completed simulation of {} orders", count);
    }
}

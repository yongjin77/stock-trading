package com.stocktrading.engine;

import com.stocktrading.structure.OrderList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentOrderTest {

    @Test
    @DisplayName("Should handle concurrent adding of orders")
    public void testConcurrentOrderAdding() throws InterruptedException {
        final TradingEngine engine = new TradingEngine();
        final int threads = 10;
        final int ordersPerThread = 100;
        final String orderSymbol = "ORDER1";

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ordersPerThread; j++) {
                        // Alternate buy/sell based on thread number (even/odd)
                        boolean isBuy = threadNum % 2 == 0;
                        double price = 100.0 + (j % 10);
                        engine.addOrder(isBuy, orderSymbol, 100, price);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // One additional match attempt to clean up
        engine.matchOrder(orderSymbol);
    }

    @RepeatedTest(5) // Run this test 5 times to increase chance of catching race conditions
    @DisplayName("Should correctly match orders in concurrent environment")
    public void testConcurrentMatching() throws InterruptedException {
        final TradingEngine engine = new TradingEngine();
        final int threadCount = 4;
        final int ordersPerThread = 50;
        final String orderSymbol = "ORDER2";

        // Access orderBook using reflection for testing
        OrderBook orderBook;
        try {
            java.lang.reflect.Field field = TradingEngine.class.getDeclaredField("orderBook");
            field.setAccessible(true);
            orderBook = (OrderBook) field.get(engine);
        } catch (Exception e) {
            fail("Could not access orderBook: " + e.getMessage());
            return;
        }

        // Track totals for verification
        AtomicInteger totalBuyQuantity = new AtomicInteger(0);
        AtomicInteger totalSellQuantity = new AtomicInteger(0);

        // Create threads for buy orders
        Thread[] buyThreads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            buyThreads[i] = new Thread(() -> {
                for (int j = 0; j < ordersPerThread; j++) {
                    int quantity = 10 + (j % 10) * 10; // 10, 20, ..., 100
                    double price = 90.0 + j;  // 90, 91, ..., 139
                    totalBuyQuantity.addAndGet(quantity);
                    engine.addOrder(true, orderSymbol, quantity, price);

                    // Small delay to mix with sell orders
                    try { Thread.sleep(1); } catch (InterruptedException e) { }
                }
            });
        }

        // Create threads for sell orders
        Thread[] sellThreads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            sellThreads[i] = new Thread(() -> {
                for (int j = 0; j < ordersPerThread; j++) {
                    int quantity = 10 + (j % 10) * 10; // 10, 20, ..., 100
                    double price = 80.0 + j;  // 80, 81, ..., 129
                    totalSellQuantity.addAndGet(quantity);
                    engine.addOrder(false, orderSymbol, quantity, price);

                    // Small delay to mix with buy orders
                    try { Thread.sleep(1); } catch (InterruptedException e) { }
                }
            });
        }

        // Start all threads
        for (Thread t : buyThreads) t.start();
        for (Thread t : sellThreads) t.start();

        // Wait for all threads to complete
        for (Thread t : buyThreads) t.join();
        for (Thread t : sellThreads) t.join();

        // Force one final match
        engine.matchOrder(orderSymbol);

        // Calculate what should be matched
        OrderList buyList = orderBook.getBuyOrders(orderSymbol);
        OrderList sellList = orderBook.getSellOrders(orderSymbol);

        int remainingBuyQty = 0;
        com.stocktrading.model.Order buyOrder = buyList.peek();
        while (buyOrder != null) {
            remainingBuyQty += buyOrder.getQuantity();
            buyOrder = buyOrder.getNext();
        }

        int remainingSellQty = 0;
        com.stocktrading.model.Order sellOrder = sellList.peek();
        while (sellOrder != null) {
            remainingSellQty += sellOrder.getQuantity();
            sellOrder = sellOrder.getNext();
        }

        int matchedQty = Math.min(totalBuyQuantity.get(), totalSellQuantity.get()) - Math.max(remainingBuyQty, remainingSellQty);

        // Verify substantial matching occurred
        assertTrue(matchedQty > 0, "Some orders should have been matched");
        assertTrue(matchedQty >= Math.min(totalBuyQuantity.get(), totalSellQuantity.get()) / 2,
                "At least half of possible matches should have occurred");
    }

    @Test
    @DisplayName("Should handle concurrent modification of the same order")
    public void testConcurrentOrderModification() throws InterruptedException {
        final TradingEngine engine = new TradingEngine();
        final String orderSymbol = "ORDER3";

        // Add one large buy order
        engine.addOrder(true, orderSymbol, 1000, 100.0);

        // Add multiple matching sell orders concurrently
        final int concurrentSellers = 10;
        final int quantityPerSell = 100;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(concurrentSellers);

        for (int i = 0; i < concurrentSellers; i++) {
            Thread t = new Thread(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Add sell order that will match with the same buy order
                    engine.addOrder(false, orderSymbol, quantityPerSell, 100.0);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completeLatch.countDown();
                }
            });
            t.start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion
        completeLatch.await(10, TimeUnit.SECONDS);

        // Access orderBook using reflection for testing
        OrderBook orderBook;
        try {
            java.lang.reflect.Field field = TradingEngine.class.getDeclaredField("orderBook");
            field.setAccessible(true);
            orderBook = (OrderBook) field.get(engine);
        } catch (Exception e) {
            fail("Could not access orderBook: " + e.getMessage());
            return;
        }

        // Check results
        OrderList buyList = orderBook.getBuyOrders(orderSymbol);
        OrderList sellList = orderBook.getSellOrders(orderSymbol);

        // Should be no remaining sell orders
        assertNull(sellList.peek(), "All sell orders should be matched");

        // Buy order should have quantity reduced by the total sell quantity
        com.stocktrading.model.Order remainingBuy = buyList.peek();
        if (remainingBuy != null) {
            int expectedRemaining = 1000 - (concurrentSellers * quantityPerSell);
            assertTrue(remainingBuy.getQuantity() <= expectedRemaining,
                    "Remaining quantity should be at most " + expectedRemaining);
        }
    }
}
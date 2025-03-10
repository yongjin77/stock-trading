package com.stocktrading;

import com.stocktrading.engine.TradingEngine;
import com.stocktrading.simulator.TradingSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for the stock trading engine.
 * Initializes the engine and runs a simulation.
 */
public class TradingApp {
    private static final Logger logger = LoggerFactory.getLogger(TradingApp.class);

    public static void main(String[] args) {
        try {
            logger.info("Starting Stock Trading Engine");

            // Create the trading engine
            TradingEngine engine = new TradingEngine();

            // Create a simulator with 100 different stocks
            TradingSimulator simulator = new TradingSimulator(engine, 100);

            // Simulation parameters
            int orderCount = 100000;  // Total number of orders
            int threadCount = 8;      // Number of threads to use

            logger.info("Starting simulation with {} orders on {} threads",
                    orderCount, threadCount);

            // Record start time
            long startTime = System.currentTimeMillis();

            // Run the simulation
            simulator.simulateTrades(orderCount, threadCount);

            // Calculate execution time
            long endTime = System.currentTimeMillis();
            double seconds = (endTime - startTime) / 1000.0;

            logger.info("Simulation completed in {} seconds", seconds);
            logger.info("Average throughput: {} orders/second", orderCount / seconds);

        } catch (Exception e) {
            logger.error("Error running trading simulation", e);
        }
    }
}
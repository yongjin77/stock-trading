package com.stocktrading.engine;

import com.stocktrading.model.Order;
import com.stocktrading.structure.OrderList;
import com.stocktrading.util.TickerHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main trading engine that handles order addition and matching.
 * Provides thread-safe operations for concurrent order processing.
 */
public class TradingEngine {
    private static final Logger logger = LoggerFactory.getLogger(TradingEngine.class);
    private final OrderBook orderBook;

    /**
     * Constructor
     */
    public TradingEngine() {
        this.orderBook = new OrderBook();
    }

    /**
     * Adds a new order to the trading system.
     *
     * @param isBuy    true for buy orders, false for sell orders
     * @param ticker   the stock ticker symbol
     * @param quantity the number of shares
     * @param price    the price per share
     */
    public void addOrder(boolean isBuy, String ticker, int quantity, double price) {
        // Validation
        if (quantity <= 0 || price <= 0) {
            logger.warn("Invalid order: quantity and price must be positive");
            return;
        }

        // Create a new order
        Order order = new Order(isBuy, ticker, quantity, price);
        int tickerIndex = TickerHasher.hash(ticker);

        logger.debug("Adding order: {}", order);

        // Add to appropriate order list
        if (isBuy) {
            orderBook.getBuyOrdersByIndex(tickerIndex).add(order);
        } else {
            orderBook.getSellOrdersByIndex(tickerIndex).add(order);
        }

        // Try to match orders
        matchOrder(ticker);
    }

    /**
     * Attempts to match buy and sell orders for a specific ticker.
     *
     * @param ticker the stock ticker symbol
     */
    public void matchOrder(String ticker) {
        int tickerIndex = TickerHasher.hash(ticker);
        matchOrderByIndex(tickerIndex);
    }

    /**
     * Implementation of order matching for a specific ticker index.
     * Matches buy and sell orders according to price priority.
     * Improved to handle concurrent modifications more robustly.
     * Time complexity: O(n) where n is the number of orders.
     *
     * @param tickerIndex the hashed ticker index
     */
    private void matchOrderByIndex(int tickerIndex) {
        OrderList buyList = orderBook.getBuyOrdersByIndex(tickerIndex);
        OrderList sellList = orderBook.getSellOrdersByIndex(tickerIndex);

        int maxIterations = 100; // Prevent potential infinite loops
        int iteration = 0;

        while (iteration < maxIterations) {
            iteration++;

            Order topBuy = buyList.peek();
            Order topSell = sellList.peek();

            if (topBuy == null || topSell == null) {
                break;
            }

            if (topBuy.getPrice() < topSell.getPrice()) {
                break; // No match possible
            }

            int buyQty = topBuy.getQuantity();
            int sellQty = topSell.getQuantity();

            if (buyQty == 0 || sellQty == 0) {
                if (buyQty == 0) buyList.removeHead();
                if (sellQty == 0) sellList.removeHead();
                continue;
            }

            int matchQty = Math.min(buyQty, sellQty);

            boolean buyUpdated = topBuy.updateQuantity(buyQty, buyQty - matchQty);
            boolean sellUpdated = topSell.updateQuantity(sellQty, sellQty - matchQty);

            if (!buyUpdated || !sellUpdated) {
                // Give other threads a chance if CAS fails
                Thread.yield();
                continue;
            }

            if (buyQty - matchQty == 0) {
                buyList.removeHead();
            }

            if (sellQty - matchQty == 0) {
                sellList.removeHead();
            }
        }
    }
}
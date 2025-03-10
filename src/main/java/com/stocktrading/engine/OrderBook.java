package com.stocktrading.engine;

import com.stocktrading.model.Order;
import com.stocktrading.structure.OrderList;
import com.stocktrading.util.TickerHasher;

/**
 * Maintains separate buy and sell order lists for each stock.
 * Uses arrays of OrderLists to store orders by ticker hash.
 */
public class OrderBook {
    // Array of order lists for buy orders, indexed by ticker hash
    private final OrderList[] buyOrders;

    // Array of order lists for sell orders, indexed by ticker hash
    private final OrderList[] sellOrders;

    /**
     * Creates a new OrderBook with capacity for 1024 ticker symbols.
     */
    public OrderBook() {
        // Initialize arrays for buy and sell orders
        buyOrders = new OrderList[1024];
        sellOrders = new OrderList[1024];

        // Create order lists for each possible ticker
        for (int i = 0; i < 1024; i++) {
            buyOrders[i] = new OrderList(true);
            sellOrders[i] = new OrderList(false);
        }
    }

    /**
     * Gets the buy order list for a specific ticker symbol.
     *
     * @param ticker the ticker symbol
     * @return the buy order list
     */
    public OrderList getBuyOrders(String ticker) {
        int index = TickerHasher.hash(ticker);
        return buyOrders[index];
    }

    /**
     * Gets the sell order list for a specific ticker symbol.
     *
     * @param ticker the ticker symbol
     * @return the sell order list
     */
    public OrderList getSellOrders(String ticker) {
        int index = TickerHasher.hash(ticker);
        return sellOrders[index];
    }

    /**
     * Gets the buy order list for a ticker at the specified hash index.
     *
     * @param index the ticker hash index
     * @return the buy order list
     */
    public OrderList getBuyOrdersByIndex(int index) {
        return buyOrders[index];
    }

    /**
     * Gets the sell order list for a ticker at the specified hash index.
     *
     * @param index the ticker hash index
     * @return the sell order list
     */
    public OrderList getSellOrdersByIndex(int index) {
        return sellOrders[index];
    }
}
package com.stocktrading.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a stock order (buy or sell) in the trading system.
 * atomic variables are used for lock free operation.
 */
public class Order {
    // add padding to avoid false sharing during multiple thread operation
    private long p1, p2, p3, p4, p5, p6, p7;  // Padding before fields
    private long q1, q2, q3, q4, q5, q6, q7;     // Padding after fields
    private final boolean isBuy;              // True for buy, false for sell
    private final String tickerSymbol;        // Stock ticker symbol
    private final double price;               // Order price
    private final AtomicInteger quantity;     // Order quantity,atomic for thread-safe updates
    private final AtomicReference<Order> nextRef = new AtomicReference<>(null); // Next order in the linked list, atomic for safety
    private final AtomicInteger version;      // Version for ABA problem prevention


    /**
     * Constructor
     *
     * @param isBuy true if this is a buy order, false if it's a sell order
     * @param tickerSymbol the stock ticker symbol
     * @param quantity the number of shares to buy or sell
     * @param price the price per share
     */
    public Order(boolean isBuy, String tickerSymbol, int quantity, double price) {
        this.isBuy = isBuy;
        this.tickerSymbol = tickerSymbol;
        this.price = price;
        this.quantity = new AtomicInteger(quantity);
        this.version = new AtomicInteger(0);
    }

    // getter and setter
    public boolean isBuy() {
        return isBuy;
    }

    public String getTickerSymbol() {
        return tickerSymbol;
    }

    public double getPrice() {
        return price;
    }


    public int getQuantity() {
        return quantity.get();
    }

    public Order getNext() {
        return nextRef.get();
    }

    public void setNext(Order next) {
        nextRef.set(next);
    }

    public boolean compareAndSetNext(Order expect, Order update) {
        return nextRef.compareAndSet(expect, update);
    }

    /**
     * Atomically updates the quantity of this order if the current value
     * matches the expected value.
     *
     * @param expectedQuantity the expected current quantity
     * @param newQuantity the new quantity to set
     * @return true if successful, false otherwise
     */
    public boolean updateQuantity(int expectedQuantity, int newQuantity) {
        return quantity.compareAndSet(expectedQuantity, newQuantity);
    }

    /**
     * Gets the current version of this order.
     * Used for ABA problem prevention in lock-free operations.
     *
     * @return the current version
     */
    public int getVersion() {
        return version.get();
    }

    /**
     * Increments the version if it matches the expected version.
     *
     * @param expectedVersion the expected version
     * @return true if successful, false otherwise
     */
    public boolean incrementVersion(int expectedVersion) {
        return version.compareAndSet(expectedVersion, expectedVersion + 1);
    }

    @Override
    public String toString() {
        return String.format("Order{%s %s, qty=%d, price=%.2f}",
                (isBuy ? "BUY" : "SELL"),
                tickerSymbol,
                quantity.get(),
                price);
    }
}
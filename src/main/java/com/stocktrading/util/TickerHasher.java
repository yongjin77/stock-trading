package com.stocktrading.util;

/**
 * Utility for hashing ticker symbols to array indices.
 * Provides a consistent mapping from ticker symbols to array positions.
 */
public class TickerHasher {
    // Total capacity for different ticker symbols
    private static final int TICKER_CAPACITY = 1024;

    /**
     * Hashes a ticker symbol to an array index between 0 and TICKER_CAPACITY-1.
     * simplified FNV-1a hash algorithm.
     *
     * @param tickerSymbol the ticker symbol to hash
     * @return an index between 0 and 1023
     */
    public static int hash(String tickerSymbol) {
        // FNV-1a hash algorithm implementation
        int h = 0x811c9dc5; // FNV offset basis
        for (int i = 0; i < tickerSymbol.length(); i++) {
            h ^= tickerSymbol.charAt(i);
            h *= 0x01000193; // FNV prime
        }
        return Math.abs(h % TICKER_CAPACITY);
    }
}
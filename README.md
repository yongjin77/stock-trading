# Lock-Free Stock Trading Engine

A high-performance, real-time stock trading engine that matches buy and sell orders using lock-free data structures. This implementation features concurrent processing of orders with thread-safe operations, designed to simulate a production trading environment.

## Features

- Support for 1,024 different stock ticker symbols
- Lock-free data structures for high-throughput, low-latency trading
- Thread-safe order operations without traditional locks
- Efficient O(n) order matching algorithm
- Price-time priority matching (best prices matched first)

## Implementation Details

The trading engine consists of several key components:

### Core Components

- **TradingEngine**: Main entry point that handles order addition and matching
- **OrderBook**: Maintains separate buy and sell order lists for each stock
- **OrderList**: Lock-free linked list implementation for storing orders in price order
- **Order**: Represents an individual buy or sell order with atomic operations

### Technical Highlights

- **Lock-Free Algorithm**: Uses Compare-and-Swap (CAS) operations throughout for thread safety
- **Order Priority**: Buy orders sorted by descending price, sell orders by ascending price
- **Atomic Operations**: Uses Java's atomic variables for concurrent modifications
- **False Sharing Prevention**: Includes cache-line padding to prevent CPU cache contention

## Usage

### Adding an Order

```java
// Create a new trading engine
TradingEngine engine = new TradingEngine();

// Add a buy order
// Parameters: isBuy, tickerSymbol, quantity, price
engine.addOrder(true, "AAPL", 100, 150.0);

// Add a sell order
engine.addOrder(false, "AAPL", 50, 151.5);

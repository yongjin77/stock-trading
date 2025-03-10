package com.stocktrading.engine;

import com.stocktrading.model.Order;
import com.stocktrading.structure.OrderList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for the TradingEngine's functionality.
 */
public class TradingEngineTest {
    private TradingEngine engine;
    private OrderBook orderBook;

    @BeforeEach
    public void setup() {
        engine = new TradingEngine();
        // Access the internal orderBook using reflection for testing
        try {
            java.lang.reflect.Field field = TradingEngine.class.getDeclaredField("orderBook");
            field.setAccessible(true);
            orderBook = (OrderBook) field.get(engine);
        } catch (Exception e) {
            fail("Could not access orderBook: " + e.getMessage());
        }
    }

    @Nested
    @DisplayName("Basic Order Adding Tests")
    class BasicOrderTests {
        @Test
        @DisplayName("Should add buy and sell orders")
        public void testAddOrders() {
            // Add a buy order
            engine.addOrder(true, "ORDER1", 100, 150.0);

            // Get the buy orders for ORDER1
            OrderList buyList = orderBook.getBuyOrders("ORDER1");
            Order buyOrder = buyList.peek();

            // Verify buy order
            assertNotNull(buyOrder, "Buy order should exist");
            assertTrue(buyOrder.isBuy(), "Should be a buy order");
            assertEquals("ORDER1", buyOrder.getTickerSymbol(), "Ticker should be ORDER1");
            assertEquals(100, buyOrder.getQuantity(), "Quantity should be 100");
            assertEquals(150.0, buyOrder.getPrice(), "Price should be 150.0");

            // Add a sell order
            engine.addOrder(false, "ORDER1", 50, 155.0);

            // Get the sell orders for ORDER1
            OrderList sellList = orderBook.getSellOrders("ORDER1");
            Order sellOrder = sellList.peek();

            // Verify sell order
            assertNotNull(sellOrder, "Sell order should exist");
            assertFalse(sellOrder.isBuy(), "Should be a sell order");
            assertEquals("ORDER1", sellOrder.getTickerSymbol(), "Ticker should be ORDER1");
            assertEquals(50, sellOrder.getQuantity(), "Quantity should be 50");
            assertEquals(155.0, sellOrder.getPrice(), "Price should be 155.0");
        }

        @Test
        @DisplayName("Should maintain price order for buy orders")
        public void testBuyOrderPriceOrdering() {
            // Add buy orders in random price order
            engine.addOrder(true, "ORDER1", 100, 150.0);
            engine.addOrder(true, "ORDER1", 100, 152.0);
            engine.addOrder(true, "ORDER1", 100, 151.0);

            // Get the buy orders
            OrderList buyList = orderBook.getBuyOrders("ORDER1");

            // Verify order is by descending price
            Order first = buyList.peek();
            assertNotNull(first);
            assertEquals(152.0, first.getPrice(), "Highest price should be first");

            Order second = first.getNext();
            assertNotNull(second);
            assertEquals(151.0, second.getPrice(), "Second highest price should be second");

            Order third = second.getNext();
            assertNotNull(third);
            assertEquals(150.0, third.getPrice(), "Lowest price should be third");
        }

        @Test
        @DisplayName("Should maintain price order for sell orders")
        public void testSellOrderPriceOrdering() {
            // Add sell orders in random price order
            engine.addOrder(false, "ORDER1", 100, 150.0);
            engine.addOrder(false, "ORDER1", 100, 152.0);
            engine.addOrder(false, "ORDER1", 100, 151.0);

            // Get the sell orders
            OrderList sellList = orderBook.getSellOrders("ORDER1");

            // Verify order is by ascending price
            Order first = sellList.peek();
            assertNotNull(first);
            assertEquals(150.0, first.getPrice(), "Lowest price should be first");

            Order second = first.getNext();
            assertNotNull(second);
            assertEquals(151.0, second.getPrice(), "Second lowest price should be second");

            Order third = second.getNext();
            assertNotNull(third);
            assertEquals(152.0, third.getPrice(), "Highest price should be third");
        }
    }

    @Nested
    @DisplayName("Order Matching Tests")
    class OrderMatchingTests {
        @Test
        @DisplayName("Should match orders when buy price >= sell price")
        public void testBasicMatching() {
            // Add buy order
            engine.addOrder(true, "ORDER1", 100, 250.0);

            // Add sell order that matches
            engine.addOrder(false, "ORDER1", 100, 245.0);

            // Both orders should be fully matched and removed
            OrderList buyList = orderBook.getBuyOrders("ORDER1");
            OrderList sellList = orderBook.getSellOrders("ORDER1");

            assertNull(buyList.peek(), "Buy order should be fully matched and removed");
            assertNull(sellList.peek(), "Sell order should be fully matched and removed");
        }

        @Test
        @DisplayName("Should handle partial matching correctly")
        public void testPartialMatching() {
            // Add buy order
            engine.addOrder(true, "ORDER2", 100, 1000.0);

            // Add sell order with smaller quantity
            engine.addOrder(false, "ORDER2", 60, 990.0);

            // Buy order should be partially matched
            OrderList buyList = orderBook.getBuyOrders("ORDER2");
            OrderList sellList = orderBook.getSellOrders("ORDER2");

            assertNotNull(buyList.peek(), "Buy order should still exist");
            assertEquals(40, buyList.peek().getQuantity(), "Buy order should have 40 remaining");
            assertNull(sellList.peek(), "Sell order should be fully matched and removed");

            // Add another sell order
            engine.addOrder(false, "ORDER2", 50, 995.0);

            // Buy order should be fully matched and removed, sell order partially matched
            assertNull(buyList.peek(), "Buy order should be fully matched and removed");
            assertNotNull(sellList.peek(), "Sell order should still exist");
            assertEquals(10, sellList.peek().getQuantity(), "Sell order should have 10 remaining");
        }

        @Test
        @DisplayName("Should not match orders when buy price < sell price")
        public void testNoMatching() {
            // Add buy order
            engine.addOrder(true, "ORDER3", 100, 800.0);

            // Add sell order with higher price
            engine.addOrder(false, "ORDER3", 100, 805.0);

            // No matching should occur
            OrderList buyList = orderBook.getBuyOrders("ORDER3");
            OrderList sellList = orderBook.getSellOrders("ORDER3");

            assertNotNull(buyList.peek(), "Buy order should still exist");
            assertEquals(100, buyList.peek().getQuantity(), "Buy order should have full quantity");
            assertNotNull(sellList.peek(), "Sell order should still exist");
            assertEquals(100, sellList.peek().getQuantity(), "Sell order should have full quantity");
        }

        @Test
        @DisplayName("Should match orders in the correct price order")
        public void testMatchingPriceOrder() {
            // Add multiple buy orders at different prices
            engine.addOrder(true, "ORDER4", 100, 3100.0);
            engine.addOrder(true, "ORDER4", 100, 3050.0);
            engine.addOrder(true, "ORDER4", 100, 3000.0);

            // Add a sell order that matches all buys
            engine.addOrder(false, "ORDER4", 300, 2990.0);

            // All buy orders should be matched
            OrderList buyList = orderBook.getBuyOrders("ORDER4");
            OrderList sellList = orderBook.getSellOrders("ORDER4");

            assertNull(buyList.peek(), "All buy orders should be matched");
            assertNull(sellList.peek(), "Sell order should be fully matched");

            // Now test the reverse
            engine.addOrder(false, "ORDER4", 100, 3000.0);
            engine.addOrder(false, "ORDER4", 100, 3050.0);
            engine.addOrder(false, "ORDER4", 100, 3100.0);

            // Add a buy order that matches all sells
            engine.addOrder(true, "ORDER4", 300, 3110.0);

            // All sell orders should be matched
            assertNull(buyList.peek(), "Buy order should be fully matched");
            assertNull(sellList.peek(), "All sell orders should be matched");
        }

        @Test
        @DisplayName("Should not cross-match between different orders")
        public void testNoOrderCrossMatching() {
            // Add buy order for ORDER5
            engine.addOrder(true, "ORDER5", 100, 150.0);

            // Add sell order for ORDER6 (different ticker)
            engine.addOrder(false, "ORDER6", 100, 145.0);

            // No matching should occur despite price compatibility
            OrderList order5BuyList = orderBook.getBuyOrders("ORDER5");
            OrderList order6SellList = orderBook.getSellOrders("ORDER6");

            assertNotNull(order5BuyList.peek(), "ORDER5 buy order should still exist");
            assertEquals(100, order5BuyList.peek().getQuantity(), "ORDER5 buy order should have full quantity");
            assertNotNull(order6SellList.peek(), "ORDER6 sell order should still exist");
            assertEquals(100, order6SellList.peek().getQuantity(), "ORDER6 sell order should have full quantity");
        }

        @Test
        @DisplayName("Should handle complex matching scenario")
        public void testComplexMatching() {
            // Add multiple buy orders
            engine.addOrder(true, "ORDER7", 100, 300.0);  // Buy Order 1
            engine.addOrder(true, "ORDER7", 200, 305.0);  // Buy Order 2
            engine.addOrder(true, "ORDER7", 150, 302.0);  // Buy Order 3

            // Add multiple sell orders
            engine.addOrder(false, "ORDER7", 120, 301.0); // Sell Order 1
            engine.addOrder(false, "ORDER7", 180, 304.0); // Sell Order 2
            engine.addOrder(false, "ORDER7", 100, 306.0); // Sell Order 3

            // Check results
            OrderList buyList = orderBook.getBuyOrders("ORDER7");
            OrderList sellList = orderBook.getSellOrders("ORDER7");


            assertNotNull(buyList.peek(), "Some buy orders should remain");
            assertEquals(302.0, buyList.peek().getPrice(), "Remaining buy should be at price 302.0");
            assertEquals(150, buyList.peek().getQuantity(), "Remaining buy should have quantity 150");

            Order secondBuy = buyList.peek().getNext();
            assertNotNull(secondBuy, "Second buy order should remain");
            assertEquals(300.0, secondBuy.getPrice(), "Second remaining buy should be at price 300.0");
            assertEquals(100, secondBuy.getQuantity(), "Second remaining buy should have quantity 100");
        }
    }
}
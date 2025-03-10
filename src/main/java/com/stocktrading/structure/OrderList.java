package com.stocktrading.structure;

import com.stocktrading.model.Order;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A lock-free linked list implementation for storing orders.
 * Maintains orders in price order (highest to lowest for buy, lowest to highest for sell).
 */
public class OrderList {
    private final AtomicReference<Order> head = new AtomicReference<>(null);
    private final boolean isBuyList;

    public OrderList(boolean isBuyList) {
        this.isBuyList = isBuyList;
    }

    /**
     * Adds an order to the list, maintaining price order.
     * Uses a lock-free algorithm to handle concurrent modifications.
     *
     * @param newOrder the order to add
     */
    public void add(Order newOrder) {
        int maxRetries = 10; // Limit retries
        int retries = 0;

        while (retries < maxRetries) {
            retries++;
            Order currentHead = head.get();

            if (currentHead == null) {
                // Empty list - try to set as new head
                newOrder.setNext(null);
                if (head.compareAndSet(null, newOrder)) {
                    return;
                }
                // Someone else added an order, retry
                continue;
            }

            // Check if the new order should be the new head (has better price)
            boolean shouldBeHead = isBuyList ?
                    newOrder.getPrice() > currentHead.getPrice() :
                    newOrder.getPrice() < currentHead.getPrice();

            if (shouldBeHead) {
                // New order has better price, insert at head
                newOrder.setNext(currentHead);
                if (head.compareAndSet(currentHead, newOrder)) {
                    return;
                }
                continue;
            }

            // Find the right position to insert based on price
            Order prev = currentHead;
            Order current = prev.getNext();

            // Limit the scan depth to avoid excessive looping
            int scanLimit = 100;
            int scanned = 0;

            // Loop to find insertion point
            while (current != null && scanned < scanLimit) {
                scanned++;

                if ((isBuyList ?
                        newOrder.getPrice() > current.getPrice() :
                        newOrder.getPrice() < current.getPrice())) {
                    break;
                }

                prev = current;
                current = current.getNext();
            }

            newOrder.setNext(current);

            // Atomic insertion
            if (prev.compareAndSetNext(current, newOrder)) {
                return;
            }

            // Failed insertion, retry from the beginning
        }

        // failed after max retries, use a forceful add as last resort
        forcefulAdd(newOrder);
    }

    // Fallback method for when normal CAS operations are failing repeatedly(more aggressive)
    private void forcefulAdd(Order newOrder) {
        for (int i = 0; i < 50; i++) {  //large retry amount
            // Get fresh state of the list
            Order currentHead = head.get();

            // Empty list case
            if (currentHead == null) {
                newOrder.setNext(null);
                if (head.compareAndSet(null, newOrder)) {
                    return;
                }
                Thread.yield();  // prevent live lock
                continue;
            }

            // Check if should be head
            boolean shouldBeHead = isBuyList ?
                    newOrder.getPrice() > currentHead.getPrice() :
                    newOrder.getPrice() < currentHead.getPrice();

            if (shouldBeHead) {
                newOrder.setNext(currentHead);
                if (head.compareAndSet(currentHead, newOrder)) {
                    return;
                }
                Thread.yield();
                continue;
            }

            // Find correct position with exponential backoff
            Order prev = currentHead;
            Order current = prev.getNext();

            // Limit scan depth to avoid long traversals
            int scanned = 0;
            int maxScan = 10 + i * 5;

            while (current != null && scanned < maxScan) {
                scanned++;

                if ((isBuyList ?
                        newOrder.getPrice() > current.getPrice() :
                        newOrder.getPrice() < current.getPrice())) {
                    break;
                }

                prev = current;
                current = current.getNext();
            }

            // Try to insert at correct position
            newOrder.setNext(current);
            if (prev.compareAndSetNext(current, newOrder)) {
                return;
            }

            // If still failed, use exponential backoff
            int backoff = (1 << Math.min(i, 10));
            Thread.yield();
            for (int j = 0; j < backoff; j++) {
                Thread.onSpinWait();
            }
        }

        //  tried aggressively and still failed, create a new thread just to add this order
        Thread emergencyAdder = new Thread(() -> {
            // Try with delay to reduce contention
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {}

            // Loop until succeed
            while (true) {
                Order latestHead = head.get();

                if (latestHead == null) {
                    newOrder.setNext(null);
                    if (head.compareAndSet(null, newOrder)) {
                        return;
                    }
                    continue;
                }

                // Try to insert at head to avoid failing(rare condition)
                newOrder.setNext(latestHead);
                if (head.compareAndSet(latestHead, newOrder)) {
                    return;
                }

                Thread.yield();
            }
        });

        emergencyAdder.setDaemon(true);
        emergencyAdder.start();
    }

    /**
     * Gets the first order in the list without removing it.
     *
     * @return the first order, or null if the list is empty
     */
    public Order peek() {
        return head.get();
    }

    /**
     * Removes and returns the first order in the list.
     * Uses a lock-free algorithm to handle concurrent modifications.
     *
     * @return the removed order, or null if the list was empty
     */
    public Order removeHead() {
        while (true) {
            Order currentHead = head.get();
            if (currentHead == null) {
                return null; // Empty list
            }

            Order newHead = currentHead.getNext();
            if (head.compareAndSet(currentHead, newHead)) {
                // Successfully removed the head
                currentHead.setNext(null); // Disconnect from list
                return currentHead;
            }
            //retry
        }
    }

    /**
     * Checks if the list is empty.
     *
     * @return true if the list is empty, false otherwise
     */
    public boolean isEmpty() {
        return head.get() == null;
    }

    /**
     * Clears all orders from this list.
     */
    public void clear() {
        head.set(null);
    }
}
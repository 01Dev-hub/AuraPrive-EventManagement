package com.auraprive.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thread-safe asynchronous notification service backed by a fixed ExecutorService thread pool.
 */
public class DiscreetNotificationService {
    private static final Object lock = new Object();
    private static int dispatchedCount = 0;

    private static final ExecutorService NOTIFICATION_POOL = Executors.newFixedThreadPool(3, runnable -> {
        Thread t = new Thread(runnable, "AuraPrive-Dispatch-Worker");
        t.setDaemon(true);
        return t;
    });

    public static void sendEncryptedDispatch(String eventTitle, String recipient) {
        NOTIFICATION_POOL.submit(() -> {
            try {
                // Simulate external encrypted transmission outside the lock so all 3 threads run in parallel
                Thread.sleep(250);
                int currentTotal;
                synchronized (lock) {
                    dispatchedCount++;
                    currentTotal = dispatchedCount;
                }
                System.out.println("[DiscreetNotification] Encrypted packet for '"
                    + eventTitle + "' sent to " + recipient
                    + " | Total Dispatches: " + currentTotal);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[DiscreetNotification] Worker interrupted: " + e.getMessage());
            }
        });
    }

    public static int getDispatchedCount() {
        synchronized (lock) {
            return dispatchedCount;
        }
    }
}
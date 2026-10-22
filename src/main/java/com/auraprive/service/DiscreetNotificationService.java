package com.auraprive.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thread-safe asynchronous notification service backed by a fixed ExecutorService thread pool.
 */
public class DiscreetNotificationService {
    private static final Object lock = new Object();
    private static int dispatchedCount = 0;

    // Managed thread pool preventing unbounded thread creation per HTTP request
    private static final ExecutorService NOTIFICATION_POOL = Executors.newFixedThreadPool(3, runnable -> {
        Thread t = new Thread(runnable, "AuraPrive-Dispatch-Worker");
        t.setDaemon(true);
        return t;
    });

    public static void sendEncryptedDispatch(String eventTitle, String recipient) {
        NOTIFICATION_POOL.submit(() -> {
            synchronized (lock) {
                try {
                    Thread.sleep(250);
                    dispatchedCount++;
                    System.out.println("[DiscreetNotification] Encrypted packet for '"
                        + eventTitle + "' sent to " + recipient
                        + " | Total Dispatches: " + dispatchedCount);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("[DiscreetNotification] Worker interrupted: " + e.getMessage());
                }
            }
        });
    }

    public static int getDispatchedCount() {
        synchronized (lock) {
            return dispatchedCount;
        }
    }
}
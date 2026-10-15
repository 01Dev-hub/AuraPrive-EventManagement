package com.auraprive.service;

/**
 * Thread-safe asynchronous service dispatching notifications via worker threads.
 */
public class DiscreetNotificationService {
    private static final Object lock = new Object();
    private static int dispatchedCount = 0;

    public static void sendEncryptedDispatch(String eventTitle, String recipient) {
        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    try {
                        // Simulating discreet, high-encryption network transmission delay
                        Thread.sleep(400);
                        dispatchedCount++;
                        System.out.println("[DiscreetNotification] Encrypted packet for '" 
                            + eventTitle + "' sent to " + recipient 
                            + " | Total Dispatches: " + dispatchedCount);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("[DiscreetNotification] Thread interrupted: " + e.getMessage());
                    }
                }
            }
        });
        workerThread.start();
    }
}
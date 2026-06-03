package com.megaseller.service;

import com.megaseller.dto.MetricsDto;
import com.megaseller.dto.MetricsDto.ThreadInfo;
import com.megaseller.dto.MetricsDto.ThreadState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton service tracking live thread states for the Admin dashboard.
 * Uses ConcurrentHashMap for thread-safe state management.
 * Must be injected as a singleton — do NOT use @Scope("prototype").
 */
@Service
public class ThreadMetricsService {

    // ConcurrentHashMap<threadId, ThreadInfo> — FR4
    private final ConcurrentHashMap<String, ThreadInfo> activeThreads = new ConcurrentHashMap<>();

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulPurchases = new AtomicLong(0);
    private final AtomicLong rejectedRequests = new AtomicLong(0);

    /**
     * Register a new thread entering the buy-ticket flow.
     */
    public void registerThread(String threadId, String username, Long showtimeId) {
        totalRequests.incrementAndGet();
        ThreadInfo info = new ThreadInfo(
            threadId,
            ThreadState.WAITING_FOR_LOCK,
            username,
            showtimeId,
            System.currentTimeMillis(),
            0L
        );
        activeThreads.put(threadId, info);
    }

    /**
     * Update state of a tracked thread.
     */
    public void updateState(String threadId, ThreadState state) {
        activeThreads.computeIfPresent(threadId, (id, info) -> {
            info.setState(state);
            info.setDuration(System.currentTimeMillis() - info.getStartedAt());
            return info;
        });
    }

    /**
     * Remove a thread from tracking map — MUST be called in finally block to prevent memory leaks.
     */
    public void removeThread(String threadId) {
        activeThreads.remove(threadId);
    }

    public void recordSuccess() {
        successfulPurchases.incrementAndGet();
    }

    public void recordRejection() {
        rejectedRequests.incrementAndGet();
    }

    /**
     * Build a full metrics snapshot for SSE push — FR5.
     */
    public MetricsDto.MetricsSnapshot getSnapshot() {
        List<ThreadInfo> threads = new ArrayList<>(activeThreads.values());
        return new MetricsDto.MetricsSnapshot(
            threads,
            totalRequests.get(),
            successfulPurchases.get(),
            rejectedRequests.get()
        );
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getSuccessfulPurchases() {
        return successfulPurchases.get();
    }

    public long getRejectedRequests() {
        return rejectedRequests.get();
    }
}

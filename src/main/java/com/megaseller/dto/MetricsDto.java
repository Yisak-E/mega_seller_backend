package com.megaseller.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MetricsDto {

    public enum ThreadState {
        WAITING_FOR_LOCK,
        ACQUIRED_LOCK,
        PROCESSING_DB_TRANSACTION,
        DONE,
        TIMEOUT
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThreadInfo {
        private String threadId;
        private ThreadState state;
        private String username;
        private Long showtimeId;
        private long startedAt;
        private long duration;
    }

    @Data
    @NoArgsConstructor
    public static class MetricsSnapshot {
        private List<ThreadInfo> threads;
        private int activeThreads;
        private int waitingForLock;
        private int processingDb;
        private int completed;
        private int timedOut;
        private long totalRequests;
        private long successfulPurchases;
        private long rejectedRequests;
        private LocalDateTime timestamp;

        public MetricsSnapshot(List<ThreadInfo> threads, long totalRequests,
                               long successfulPurchases, long rejectedRequests) {
            this.threads = threads;
            this.activeThreads = threads.size();
            this.waitingForLock = (int) threads.stream()
                .filter(t -> t.getState() == ThreadState.WAITING_FOR_LOCK).count();
            this.processingDb = (int) threads.stream()
                .filter(t -> t.getState() == ThreadState.PROCESSING_DB_TRANSACTION
                         || t.getState() == ThreadState.ACQUIRED_LOCK).count();
            this.completed = (int) threads.stream()
                .filter(t -> t.getState() == ThreadState.DONE).count();
            this.timedOut = (int) threads.stream()
                .filter(t -> t.getState() == ThreadState.TIMEOUT).count();
            this.totalRequests = totalRequests;
            this.successfulPurchases = successfulPurchases;
            this.rejectedRequests = rejectedRequests;
            this.timestamp = LocalDateTime.now();
        }
    }
}

package com.megaseller.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megaseller.dto.MetricsDto;
import com.megaseller.dto.TicketDto;
import com.megaseller.service.ThreadMetricsService;
import com.megaseller.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Admin SSE endpoints — FR5.
 * Pushes thread state and inventory snapshots to connected admin clients every 200ms.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final ThreadMetricsService metricsService;
    private final TicketService ticketService;
    private final ObjectMapper objectMapper;

    // SSE emitter registries
    private final CopyOnWriteArrayList<SseEmitter> metricsEmitters = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SseEmitter> inventoryEmitters = new CopyOnWriteArrayList<>();

    @Value("${megaseller.sse.push-interval-ms:200}")
    private long pushIntervalMs;

    public AdminController(ThreadMetricsService metricsService,
                           TicketService ticketService,
                           ObjectMapper objectMapper) {
        this.metricsService = metricsService;
        this.ticketService = ticketService;
        this.objectMapper = objectMapper;
    }

    /**
     * SSE endpoint: streams thread state snapshots to admin clients — FR5.
     */
    @GetMapping(value = "/metrics-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        metricsEmitters.add(emitter);
        emitter.onCompletion(() -> metricsEmitters.remove(emitter));
        emitter.onTimeout(() -> metricsEmitters.remove(emitter));
        emitter.onError(e -> metricsEmitters.remove(emitter));

        // Send initial snapshot immediately
        try {
            emitter.send(SseEmitter.event()
                .name("metrics")
                .data(objectMapper.writeValueAsString(metricsService.getSnapshot())));
        } catch (IOException e) {
            logger.warn("Failed to send initial metrics: {}", e.getMessage());
        }
        return emitter;
    }

    /**
     * SSE endpoint: streams inventory counts to admin clients.
     */
    @GetMapping(value = "/inventory-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamInventory() {
        SseEmitter emitter = new SseEmitter(0L);
        inventoryEmitters.add(emitter);
        emitter.onCompletion(() -> inventoryEmitters.remove(emitter));
        emitter.onTimeout(() -> inventoryEmitters.remove(emitter));
        emitter.onError(e -> inventoryEmitters.remove(emitter));

        // Send initial data
        try {
            emitter.send(SseEmitter.event()
                .name("inventory")
                .data(objectMapper.writeValueAsString(ticketService.getInventorySnapshots())));
        } catch (IOException e) {
            logger.warn("Failed to send initial inventory: {}", e.getMessage());
        }
        return emitter;
    }

    /**
     * Scheduled push to all connected admin clients every 200ms — FR5, NFR2.
     */
    @Scheduled(fixedDelayString = "${megaseller.sse.push-interval-ms:200}")
    public void pushMetrics() {
        if (metricsEmitters.isEmpty() && inventoryEmitters.isEmpty()) return;

        // Push thread metrics
        if (!metricsEmitters.isEmpty()) {
            MetricsDto.MetricsSnapshot snapshot = metricsService.getSnapshot();
            pushToEmitters(metricsEmitters, "metrics", snapshot);
        }

        // Push inventory
        if (!inventoryEmitters.isEmpty()) {
            List<TicketDto.InventorySnapshot> inventory = ticketService.getInventorySnapshots();
            pushToEmitters(inventoryEmitters, "inventory", inventory);
        }
    }

    private void pushToEmitters(CopyOnWriteArrayList<SseEmitter> emitters, String eventName, Object data) {
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            logger.error("Failed to serialize SSE data: {}", e.getMessage());
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(json));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    // REST endpoint for one-shot metrics (non-SSE)
    @GetMapping("/metrics")
    public MetricsDto.MetricsSnapshot getMetrics() {
        return metricsService.getSnapshot();
    }

    @GetMapping("/inventory")
    public List<TicketDto.InventorySnapshot> getInventory() {
        return ticketService.getInventorySnapshots();
    }
}

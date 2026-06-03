package com.megaseller.service;

import com.megaseller.dto.MetricsDto.ThreadState;
import com.megaseller.dto.TicketDto;
import com.megaseller.entity.*;
import com.megaseller.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core Mega Seller ticket purchase service.
 *
 * Concurrency model:
 * - Application-level: ReentrantLock per showtime (prevents overselling at app layer)
 * - Database level:    PESSIMISTIC_WRITE lock on ticket row (ACID guarantee)
 *
 * This dual-locking approach satisfies FR2, FR3, NFR1.
 */
@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final ShowtimeRepository showtimeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ThreadMetricsService metricsService;

    @Autowired
    @Lazy
    private TicketService self;

    @Value("${megaseller.ticket.lock-timeout-ms:2000}")
    private long lockTimeoutMs;

    // One ReentrantLock per showtime — prevents thundering-herd oversell at app layer
    private final java.util.concurrent.ConcurrentHashMap<Long, ReentrantLock> showtimeLocks =
        new java.util.concurrent.ConcurrentHashMap<>();

    public TicketService(TicketRepository ticketRepository,
                         ShowtimeRepository showtimeRepository,
                         OrderRepository orderRepository,
                         UserRepository userRepository,
                         ThreadMetricsService metricsService) {
        this.ticketRepository = ticketRepository;
        this.showtimeRepository = showtimeRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.metricsService = metricsService;
    }

    /**
     * Core Mega Seller endpoint logic — FR2, FR3, FR6.
     *
     * @return BuyResponse with success/failure details
     */
    public TicketDto.BuyResponse buyTicket(Long userId, TicketDto.BuyRequest request) {
        String threadId = Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
        String username = userRepository.findById(userId)
            .map(u -> u.getUsername()).orElse("unknown");

        // Register thread for live monitoring — FR4
        metricsService.registerThread(threadId, username, request.getShowtimeId());

        // Get or create the per-showtime lock
        ReentrantLock lock = showtimeLocks.computeIfAbsent(request.getShowtimeId(), id -> new ReentrantLock(true));

        boolean lockAcquired = false;
        try {
            metricsService.updateState(threadId, ThreadState.WAITING_FOR_LOCK);

            // FR6: Reject if lock not acquired within 2 seconds
            lockAcquired = lock.tryLock(lockTimeoutMs, TimeUnit.MILLISECONDS);
            if (!lockAcquired) {
                metricsService.updateState(threadId, ThreadState.TIMEOUT);
                metricsService.recordRejection();
                logger.warn("Thread {} timed out waiting for lock on showtime {}", threadId, request.getShowtimeId());
                return TicketDto.BuyResponse.failure("System is busy. Please try again (lock timeout).");
            }

            metricsService.updateState(threadId, ThreadState.ACQUIRED_LOCK);

            // Check sale window
            Showtime showtime = showtimeRepository.findByIdWithTickets(request.getShowtimeId())
                .orElseThrow(() -> new IllegalArgumentException("Showtime not found"));

            if (showtime.getSaleOpensAt() != null && LocalDateTime.now().isBefore(showtime.getSaleOpensAt())) {
                return TicketDto.BuyResponse.failure("Tickets are not on sale yet.");
            }

            // Execute the DB transaction under the application lock
            metricsService.updateState(threadId, ThreadState.PROCESSING_DB_TRANSACTION);
            TicketDto.BuyResponse result = self.executeTicketPurchase(userId, request.getShowtimeId(),
                request.getPreferredSeatRow(), request.getPreferredSeatNumber());

            if (result.isSuccess()) {
                metricsService.recordSuccess();
            } else {
                metricsService.recordRejection();
            }

            metricsService.updateState(threadId, ThreadState.DONE);
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            metricsService.updateState(threadId, ThreadState.TIMEOUT);
            metricsService.recordRejection();
            return TicketDto.BuyResponse.failure("Request interrupted.");
        } finally {
            // CRITICAL: always release lock and remove thread tracking — prevents memory leaks
            if (lockAcquired) {
                lock.unlock();
            }
            metricsService.removeThread(threadId);
        }
    }

    /**
     * DB transaction: find available ticket, lock row, mark SOLD, create Order.
     * FR3: Updates specific ticket row from AVAILABLE -> SOLD.
     */
    @Transactional
    public TicketDto.BuyResponse executeTicketPurchase(Long userId, Long showtimeId,
                                                           String preferredRow, Integer preferredSeat) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Showtime showtime = showtimeRepository.findByIdWithTickets(showtimeId)
            .orElseThrow(() -> new IllegalArgumentException("Showtime not found"));

        // Find available ticket with DB-level row lock (PESSIMISTIC_WRITE)
        List<Ticket> availableTickets = ticketRepository.findAvailableByShowtimeIdWithLock(showtimeId);

        if (availableTickets.isEmpty()) {
            return TicketDto.BuyResponse.failure("No seats available — sold out!");
        }

        // Try preferred seat first, fall back to first available
        Ticket selectedTicket = availableTickets.stream()
            .filter(t -> preferredRow != null && preferredSeat != null
                && t.getSeatRow().equalsIgnoreCase(preferredRow)
                && t.getSeatNumber().equals(preferredSeat))
            .findFirst()
            .orElse(availableTickets.get(0));

        // Double-check it's still AVAILABLE under the lock
        if (selectedTicket.getStatus() != Ticket.TicketStatus.AVAILABLE) {
            return TicketDto.BuyResponse.failure("That seat was just taken. Please try again.");
        }

        // Create Order
        Order order = Order.builder()
            .user(user)
            .totalPrice(showtime.getTicketPrice())
            .status(Order.OrderStatus.CONFIRMED)
            .build();
        order = orderRepository.save(order);

        // FR3: Update specific ticket row to SOLD
        selectedTicket.setStatus(Ticket.TicketStatus.SOLD);
        selectedTicket.setOrder(order);
        ticketRepository.save(selectedTicket);

        logger.info("User {} purchased ticket {} (seat {}) for showtime {}",
            user.getUsername(), selectedTicket.getId(), selectedTicket.getSeatLabel(), showtimeId);

        return TicketDto.BuyResponse.success(
            order.getId(),
            selectedTicket.getSeatLabel(),
            showtime.getTicketPrice(),
            showtime.getMovie().getTitle(),
            showtime.getStartTime(),
            showtime.getHallName()
        );
    }

    public List<TicketDto.InventorySnapshot> getInventorySnapshots() {
        return showtimeRepository.findAll().stream()
            .map(s -> {
                long available = ticketRepository.countByShowtimeIdAndStatus(s.getId(), Ticket.TicketStatus.AVAILABLE);
                long locked = ticketRepository.countByShowtimeIdAndStatus(s.getId(), Ticket.TicketStatus.LOCKED);
                long sold = ticketRepository.countByShowtimeIdAndStatus(s.getId(), Ticket.TicketStatus.SOLD);
                long total = available + locked + sold;
                return new TicketDto.InventorySnapshot(s.getId(), s.getMovie().getTitle(),
                    available, locked, sold, total);
            })
            .toList();
    }
}

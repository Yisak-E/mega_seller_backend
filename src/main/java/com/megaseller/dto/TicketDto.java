package com.megaseller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TicketDto {

    @Data
    public static class BuyRequest {
        @NotNull(message = "Showtime ID is required")
        private Long showtimeId;

        // Optional: specific seat request
        private String preferredSeatRow;
        private Integer preferredSeatNumber;
    }

    @Data
    @NoArgsConstructor
    public static class BuyResponse {
        private boolean success;
        private String message;
        private Long orderId;
        private String seatLabel;
        private BigDecimal price;
        private String movieTitle;
        private LocalDateTime showtime;
        private String hall;

        public static BuyResponse success(Long orderId, String seatLabel, BigDecimal price,
                                          String movieTitle, LocalDateTime showtime, String hall) {
            BuyResponse r = new BuyResponse();
            r.success = true;
            r.message = "Ticket purchased successfully!";
            r.orderId = orderId;
            r.seatLabel = seatLabel;
            r.price = price;
            r.movieTitle = movieTitle;
            r.showtime = showtime;
            r.hall = hall;
            return r;
        }

        public static BuyResponse failure(String message) {
            BuyResponse r = new BuyResponse();
            r.success = false;
            r.message = message;
            return r;
        }
    }

    @Data
    @NoArgsConstructor
    public static class OrderSummary {
        private Long orderId;
        private String movieTitle;
        private String seatLabel;
        private String hall;
        private LocalDateTime showtime;
        private BigDecimal price;
        private LocalDateTime purchasedAt;
        private String status;
    }

    @Data
    @NoArgsConstructor
    public static class InventorySnapshot {
        private Long showtimeId;
        private String movieTitle;
        private long available;
        private long locked;
        private long sold;
        private long total;
        private LocalDateTime timestamp;

        public InventorySnapshot(Long showtimeId, String movieTitle,
                                 long available, long locked, long sold, long total) {
            this.showtimeId = showtimeId;
            this.movieTitle = movieTitle;
            this.available = available;
            this.locked = locked;
            this.sold = sold;
            this.total = total;
            this.timestamp = LocalDateTime.now();
        }
    }
}

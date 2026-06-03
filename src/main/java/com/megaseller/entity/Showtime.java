package com.megaseller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "showtimes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "hall_name")
    private String hallName;

    @Column(name = "ticket_price", precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @Column(name = "sale_opens_at")
    private LocalDateTime saleOpensAt;

    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ticket> tickets;

    public long getAvailableCount() {
        if (tickets == null) return 0;
        return tickets.stream().filter(t -> t.getStatus() == Ticket.TicketStatus.AVAILABLE).count();
    }

    public long getSoldCount() {
        if (tickets == null) return 0;
        return tickets.stream().filter(t -> t.getStatus() == Ticket.TicketStatus.SOLD).count();
    }

    public long getLockedCount() {
        if (tickets == null) return 0;
        return tickets.stream().filter(t -> t.getStatus() == Ticket.TicketStatus.LOCKED).count();
    }
}

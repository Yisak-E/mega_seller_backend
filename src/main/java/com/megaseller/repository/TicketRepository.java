package com.megaseller.repository;

import com.megaseller.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT t FROM Ticket t WHERE t.showtime.id = :showtimeId AND t.status = 'AVAILABLE' ORDER BY t.seatRow, t.seatNumber")
    List<Ticket> findAvailableByShowtimeId(@Param("showtimeId") Long showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.showtime.id = :showtimeId AND t.status = 'AVAILABLE' ORDER BY t.seatRow, t.seatNumber")
    List<Ticket> findAvailableByShowtimeIdWithLock(@Param("showtimeId") Long showtimeId);

    long countByShowtimeIdAndStatus(Long showtimeId, Ticket.TicketStatus status);
}

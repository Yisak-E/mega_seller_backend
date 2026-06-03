package com.megaseller.repository;

import com.megaseller.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    List<Showtime> findByMovieId(Long movieId);

    @Query("SELECT s FROM Showtime s LEFT JOIN FETCH s.tickets WHERE s.id = :id")
    Optional<Showtime> findByIdWithTickets(@Param("id") Long id);
}

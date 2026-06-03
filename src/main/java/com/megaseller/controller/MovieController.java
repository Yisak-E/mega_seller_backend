package com.megaseller.controller;

import com.megaseller.dto.MovieDto;
import com.megaseller.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<List<MovieDto.MovieSummary>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/{id}/showtimes")
    public ResponseEntity<List<MovieDto.ShowtimeSummary>> getShowtimes(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getShowtimesByMovie(id));
    }

    @GetMapping("/showtimes/{showtimeId}")
    public ResponseEntity<MovieDto.ShowtimeSummary> getShowtime(@PathVariable Long showtimeId) {
        try {
            return ResponseEntity.ok(movieService.getShowtime(showtimeId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

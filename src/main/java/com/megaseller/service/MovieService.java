package com.megaseller.service;

import com.megaseller.dto.MovieDto;
import com.megaseller.repository.MovieRepository;
import com.megaseller.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public MovieService(MovieRepository movieRepository, ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    public List<MovieDto.MovieSummary> getAllMovies() {
        return movieRepository.findAllWithShowtimes().stream()
            .map(MovieDto.MovieSummary::from)
            .collect(Collectors.toList());
    }

    public MovieDto.ShowtimeSummary getShowtime(Long showtimeId) {
        return showtimeRepository.findByIdWithTickets(showtimeId)
            .map(MovieDto.ShowtimeSummary::from)
            .orElseThrow(() -> new IllegalArgumentException("Showtime not found: " + showtimeId));
    }

    public List<MovieDto.ShowtimeSummary> getShowtimesByMovie(Long movieId) {
        return showtimeRepository.findByMovieId(movieId).stream()
            .map(s -> {
                return showtimeRepository.findByIdWithTickets(s.getId())
                    .map(MovieDto.ShowtimeSummary::from)
                    .orElse(MovieDto.ShowtimeSummary.from(s));
            })
            .collect(Collectors.toList());
    }
}

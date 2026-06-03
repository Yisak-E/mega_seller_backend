package com.megaseller.dto;

import com.megaseller.entity.Movie;
import com.megaseller.entity.Showtime;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class MovieDto {

    @Data
    @NoArgsConstructor
    public static class MovieSummary {
        private Long id;
        private String title;
        private String description;
        private String genre;
        private String director;
        private Integer durationMinutes;
        private String posterUrl;
        private String rating;
        private LocalDateTime releaseDate;
        private List<ShowtimeSummary> showtimes;

        public static MovieSummary from(Movie movie) {
            MovieSummary dto = new MovieSummary();
            dto.id = movie.getId();
            dto.title = movie.getTitle();
            dto.description = movie.getDescription();
            dto.genre = movie.getGenre();
            dto.director = movie.getDirector();
            dto.durationMinutes = movie.getDurationMinutes();
            dto.posterUrl = movie.getPosterUrl();
            dto.rating = movie.getRating();
            dto.releaseDate = movie.getReleaseDate();
            if (movie.getShowtimes() != null) {
                dto.showtimes = movie.getShowtimes().stream()
                    .map(ShowtimeSummary::from)
                    .collect(Collectors.toList());
            }
            return dto;
        }
    }

    @Data
    @NoArgsConstructor
    public static class ShowtimeSummary {
        private Long id;
        private Long movieId;
        private String movieTitle;
        private LocalDateTime startTime;
        private String hallName;
        private BigDecimal ticketPrice;
        private LocalDateTime saleOpensAt;
        private long availableSeats;
        private long soldSeats;
        private long lockedSeats;
        private long totalSeats;

        public static ShowtimeSummary from(Showtime showtime) {
            ShowtimeSummary dto = new ShowtimeSummary();
            dto.id = showtime.getId();
            dto.movieId = showtime.getMovie().getId();
            dto.movieTitle = showtime.getMovie().getTitle();
            dto.startTime = showtime.getStartTime();
            dto.hallName = showtime.getHallName();
            dto.ticketPrice = showtime.getTicketPrice();
            dto.saleOpensAt = showtime.getSaleOpensAt();
            if (showtime.getTickets() != null) {
                dto.availableSeats = showtime.getAvailableCount();
                dto.soldSeats = showtime.getSoldCount();
                dto.lockedSeats = showtime.getLockedCount();
                dto.totalSeats = showtime.getTickets().size();
            }
            return dto;
        }
    }
}

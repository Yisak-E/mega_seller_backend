package com.megaseller.config;

import com.megaseller.entity.*;
import com.megaseller.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, MovieRepository movieRepository,
                      ShowtimeRepository showtimeRepository, TicketRepository ticketRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            logger.info("Data already seeded, skipping.");
            return;
        }

        logger.info("Seeding database with demo data...");

        // --- Users ---
        User admin = User.builder()
            .username("admin")
            .email("admin@megaseller.com")
            .password(passwordEncoder.encode("admin123"))
            .role(User.Role.ROLE_ADMIN)
            .build();

        User user1 = User.builder()
            .username("alice")
            .email("alice@example.com")
            .password(passwordEncoder.encode("password123"))
            .role(User.Role.ROLE_USER)
            .build();

        User user2 = User.builder()
            .username("bob")
            .email("bob@example.com")
            .password(passwordEncoder.encode("password123"))
            .role(User.Role.ROLE_USER)
            .build();

        userRepository.saveAll(List.of(admin, user1, user2));

        // --- Movies ---
        Movie movie1 = Movie.builder()
            .title("NEXUS: The Final Protocol")
            .description("A rogue AI has infiltrated every network on Earth. One team of specialists must navigate a fractured digital reality to stop the extinction event before the clock hits zero.")
            .genre("Sci-Fi / Thriller")
            .director("Zara Chen")
            .durationMinutes(142)
            .posterUrl("/posters/nexus.jpg")
            .rating("PG-13")
            .releaseDate(LocalDateTime.now().plusDays(3))
            .build();

        Movie movie2 = Movie.builder()
            .title("SHADOW EMPIRE")
            .description("In a world where shadows are sentient, a young cartographer discovers a map that could end the thousand-year war—or start an even greater one.")
            .genre("Fantasy / Adventure")
            .director("Marcus Okonkwo")
            .durationMinutes(158)
            .posterUrl("/posters/shadow-empire.jpg")
            .rating("PG-13")
            .releaseDate(LocalDateTime.now().plusDays(7))
            .build();

        Movie movie3 = Movie.builder()
            .title("VELOCITY")
            .description("The world's fastest racing driver uncovers a conspiracy that puts every Formula circuit on the planet in the crosshairs of a shadowy syndicate.")
            .genre("Action / Drama")
            .director("Isabella Rossi")
            .durationMinutes(118)
            .posterUrl("/posters/velocity.jpg")
            .rating("PG")
            .releaseDate(LocalDateTime.now().minusDays(1)) // already released
            .build();

        movieRepository.saveAll(List.of(movie1, movie2, movie3));

        // --- Showtimes ---
        // Nexus - premiere in 5 minutes for demo purposes
        Showtime nexus1 = Showtime.builder()
            .movie(movie1)
            .startTime(LocalDateTime.now().plusDays(3).withHour(19).withMinute(0))
            .hallName("IMAX Hall 1")
            .ticketPrice(new BigDecimal("29.99"))
            .saleOpensAt(LocalDateTime.now().plusMinutes(5))
            .build();
        nexus1 = showtimeRepository.save(nexus1);
        seedSeats(nexus1, "ABCDE", 10);

        Showtime nexus2 = Showtime.builder()
            .movie(movie1)
            .startTime(LocalDateTime.now().plusDays(3).withHour(22).withMinute(0))
            .hallName("Screen 2")
            .ticketPrice(new BigDecimal("19.99"))
            .saleOpensAt(LocalDateTime.now().minusMinutes(1)) // already open
            .build();
        nexus2 = showtimeRepository.save(nexus2);
        seedSeats(nexus2, "ABCDEFGH", 12);

        // Shadow Empire
        Showtime shadow1 = Showtime.builder()
            .movie(movie2)
            .startTime(LocalDateTime.now().plusDays(7).withHour(20).withMinute(30))
            .hallName("IMAX Hall 1")
            .ticketPrice(new BigDecimal("29.99"))
            .saleOpensAt(LocalDateTime.now().plusDays(2))
            .build();
        shadow1 = showtimeRepository.save(shadow1);
        seedSeats(shadow1, "ABCDE", 8);

        // Velocity
        Showtime velocity1 = Showtime.builder()
            .movie(movie3)
            .startTime(LocalDateTime.now().plusHours(2))
            .hallName("Screen 3")
            .ticketPrice(new BigDecimal("15.99"))
            .saleOpensAt(LocalDateTime.now().minusDays(1))
            .build();
        velocity1 = showtimeRepository.save(velocity1);
        seedSeats(velocity1, "ABCDEF", 8);

        logger.info("Seeding complete. Users: admin/admin123, alice/password123, bob/password123");
    }

    private void seedSeats(Showtime showtime, String rows, int seatsPerRow) {
        List<Ticket> tickets = new ArrayList<>();
        for (char row : rows.toCharArray()) {
            for (int seat = 1; seat <= seatsPerRow; seat++) {
                tickets.add(Ticket.builder()
                    .showtime(showtime)
                    .seatRow(String.valueOf(row))
                    .seatNumber(seat)
                    .status(Ticket.TicketStatus.AVAILABLE)
                    .build());
            }
        }
        ticketRepository.saveAll(tickets);
        logger.info("Seeded {} seats for showtime {} ({})",
            tickets.size(), showtime.getId(), showtime.getMovie().getTitle());
    }
}

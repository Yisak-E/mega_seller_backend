package com.megaseller.controller;

import com.megaseller.dto.TicketDto;
import com.megaseller.repository.UserRepository;
import com.megaseller.security.JwtUtil;
import com.megaseller.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public TicketController(TicketService ticketService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.ticketService = ticketService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    /**
     * Core Mega Seller Endpoint — FR2, FR3, FR6.
     * Returns HTTP 429 on lock timeout.
     */
    @PostMapping("/buy")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TicketDto.BuyResponse> buyTicket(
            @Valid @RequestBody TicketDto.BuyRequest request,
            HttpServletRequest httpRequest) {

        Long userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TicketDto.BuyResponse response = ticketService.buyTicket(userId, request);

        if (!response.isSuccess()) {
            // FR6: return 429 for lock timeout, 409 for sold out
            String msg = response.getMessage();
            if (msg != null && (msg.contains("busy") || msg.contains("timeout") || msg.contains("interrupted"))) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.ok(response);
    }

    private Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                return jwtUtil.extractUserId(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}

package com.megaseller.controller;

import com.megaseller.dto.TicketDto;
import com.megaseller.security.JwtUtil;
import com.megaseller.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    public UserController(OrderService orderService, JwtUtil jwtUtil) {
        this.orderService = orderService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<TicketDto.OrderSummary>> getMyOrders(HttpServletRequest request) {
        Long userId = extractUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    private Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractUserId(header.substring(7));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}

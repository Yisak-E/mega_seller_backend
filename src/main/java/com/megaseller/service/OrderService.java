package com.megaseller.service;

import com.megaseller.dto.TicketDto;
import com.megaseller.entity.Order;
import com.megaseller.entity.Ticket;
import com.megaseller.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<TicketDto.OrderSummary> getUserOrders(Long userId) {
        return orderRepository.findByUserIdWithDetails(userId).stream()
            .flatMap(order -> order.getTickets().stream()
                .map(ticket -> mapToSummary(order, ticket)))
            .collect(Collectors.toList());
    }

    private TicketDto.OrderSummary mapToSummary(Order order, Ticket ticket) {
        TicketDto.OrderSummary summary = new TicketDto.OrderSummary();
        summary.setOrderId(order.getId());
        summary.setMovieTitle(ticket.getShowtime().getMovie().getTitle());
        summary.setSeatLabel(ticket.getSeatLabel());
        summary.setHall(ticket.getShowtime().getHallName());
        summary.setShowtime(ticket.getShowtime().getStartTime());
        summary.setPrice(order.getTotalPrice());
        summary.setPurchasedAt(order.getCreatedAt());
        summary.setStatus(order.getStatus().name());
        return summary;
    }
}

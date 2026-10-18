package com.ankit.orderservice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderRepository repository;
    private final OrderEventProducer producer;
    private final OrderService service;
    private final AdminSessionService adminSessions;

    public OrderController(OrderRepository repository, OrderEventProducer producer,
                           OrderService service, AdminSessionService adminSessions) {
        this.repository = repository;
        this.producer = producer;
        this.service = service;
        this.adminSessions = adminSessions;
    }

    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody Order order) {
        order.setId(null);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        Order saved = repository.save(order);
        producer.sendOrderCreated(saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<Order> getAll() { return repository.findAllByOrderByIdDesc(); }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/admin/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        String csrf = adminSessions.login(body.username(), body.password(), request);
        return Map.of("username", body.username(), "csrfToken", csrf);
    }

    @GetMapping("/admin/session")
    public ResponseEntity<Map<String, String>> session(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (!adminSessions.isAuthenticated(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(Map.of("username", String.valueOf(session.getAttribute(AdminSessionService.USER_ATTR)), "csrfToken", String.valueOf(session.getAttribute(AdminSessionService.CSRF_ATTR))));
    }

    @PostMapping("/admin/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       @RequestHeader(value = "X-CSRF-Token", required = false) String csrf) {
        HttpSession session = request.getSession(false);
        adminSessions.requireAdmin(session, csrf);
        adminSessions.logout(session);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ship")
    public Order ship(@PathVariable Long id, HttpServletRequest request,
                      @RequestHeader(value = "X-CSRF-Token", required = false) String csrf) {
        adminSessions.requireAdmin(request.getSession(false), csrf);
        return service.ship(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request,
                                       @RequestHeader(value = "X-CSRF-Token", required = false) String csrf) {
        adminSessions.requireAdmin(request.getSession(false), csrf);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/ship-bulk")
    public Map<String, Object> shipBulk(@RequestBody BulkRequest body, HttpServletRequest request,
                                        @RequestHeader(value = "X-CSRF-Token", required = false) String csrf) {
        adminSessions.requireAdmin(request.getSession(false), csrf);
        return bulk(body, true);
    }

    @PostMapping("/admin/delete-bulk")
    public Map<String, Object> deleteBulk(@RequestBody BulkRequest body, HttpServletRequest request,
                                          @RequestHeader(value = "X-CSRF-Token", required = false) String csrf) {
        adminSessions.requireAdmin(request.getSession(false), csrf);
        return bulk(body, false);
    }

    private Map<String, Object> bulk(BulkRequest body, boolean ship) {
        if (body == null || body.ids() == null || body.ids().isEmpty() || body.ids().size() > 500)
            throw new OrderExceptions.InvalidBulkRequest();
        List<Long> ids = body.ids().stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) throw new OrderExceptions.InvalidBulkRequest();
        List<Map<String, Object>> results = new ArrayList<>();
        int succeeded = 0;
        for (Long id : ids) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            try {
                if (ship) service.ship(id); else service.delete(id);
                item.put("success", true);
                item.put("message", ship ? "Shipped" : "Deleted");
                succeeded++;
            } catch (OrderExceptions.NotFound | OrderExceptions.InvalidTransition ex) {
                item.put("success", false);
                item.put("message", ex.getMessage());
            }
            results.add(item);
        }
        return Map.of("succeeded", succeeded, "failed", results.size() - succeeded, "results", results);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record BulkRequest(List<Long> ids) {}
}

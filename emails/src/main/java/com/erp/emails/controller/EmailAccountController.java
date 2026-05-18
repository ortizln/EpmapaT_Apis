package com.erp.emails.controller;

import com.erp.emails.dtos.EmailAccountRequest;
import com.erp.emails.dtos.EmailAccountResponse;
import com.erp.emails.service.EmailAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/email-accounts")
public class EmailAccountController {

    private final EmailAccountService accountService;

    public EmailAccountController(EmailAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<EmailAccountResponse> list(@RequestParam(required = false) Boolean active) {
        return accountService.list(active);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailAccountResponse> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(accountService.get(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody EmailAccountRequest req) {
        try {
            return ResponseEntity.ok(accountService.create(req));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody EmailAccountRequest req) {
        try {
            return ResponseEntity.ok(accountService.update(id, req));
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("id " + id)) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(msg);
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        return updateActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        return updateActive(id, false);
    }

    private ResponseEntity<?> updateActive(Long id, boolean active) {
        try {
            return ResponseEntity.ok(accountService.activate(id, active));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}

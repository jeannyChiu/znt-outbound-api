package com.znt.outbound.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/simple")
public class SimpleTestController {

    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from Simple Test Controller");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}

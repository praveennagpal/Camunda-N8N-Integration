package com.example.demo;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoController {
    private final ProcessStarterService processStarterService;

    public DemoController(ProcessStarterService processStarterService) {
        this.processStarterService = processStarterService;
    }

    @PostMapping("/start")
    public ResponseEntity<Object> startProcess(@RequestBody(required = false) Map<String, Object> variables) {
        String businessKey = "demo-" + System.currentTimeMillis();
        Object n8nResult = processStarterService.startProcessAndGetN8nResult("process_call_n8n", businessKey, variables == null ? Map.of() : variables);
        return ResponseEntity.ok(n8nResult);
    }
}

package com.example.demo;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private final RuntimeService runtimeService;

    public DemoController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @PostMapping("/start")
    public ResponseEntity<String> startProcess(@RequestBody(required = false) Map<String, Object> variables) {
        String businessKey = "demo-" + System.currentTimeMillis();
        runtimeService.startProcessInstanceByKey("process_call_n8n", businessKey, variables == null ? Map.of() : variables);
        return ResponseEntity.ok("started: " + businessKey);
    }
}

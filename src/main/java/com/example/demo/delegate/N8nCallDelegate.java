package com.example.demo.delegate;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class N8nCallDelegate implements JavaDelegate {

    private final WebClient webClient;
    private final Duration timeout;

    public N8nCallDelegate(@Value("${n8n.webhook.url:http://localhost:5678/webhook/camunda-trigger}") String n8nWebhookUrl) {
        this.webClient = WebClient.builder().baseUrl(n8nWebhookUrl).build();
        this.timeout = Duration.ofSeconds(20);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Object policyId = execution.getVariable("policyId");
        Map<String, Object> payload = new HashMap<>();
        payload.put("processInstanceId", execution.getProcessInstanceId());
        payload.put("businessKey", execution.getBusinessKey());
        payload.put("policyId", policyId);
        payload.put("timestamp", System.currentTimeMillis());

        String idempotencyKey = execution.getProcessInstanceId() + "-" + UUID.randomUUID();

        try {
            Map response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", idempotencyKey)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(timeout)
                    .onErrorMap(throwable -> new RuntimeException("n8n call failed: " + throwable.getMessage(), throwable))
                    .block();

            if (response != null && response.containsKey("result")) {
                execution.setVariable("n8nResult", response.get("result"));
            } else {
                execution.setVariable("n8nResult", response);
            }

        } catch (RuntimeException ex) {
            throw new BpmnError("N8N_CALL_FAILED", "Call to n8n failed: " + ex.getMessage());
        }
    }
}

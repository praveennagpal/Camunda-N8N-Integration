package com.example.demo.external;

import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class N8nExternalTaskWorker {

    public static void main(String[] args) {
        // Configure the client
        ExternalTaskClient client = ExternalTaskClient.create()
                .baseUrl("http://localhost:8080/engine-rest") // Camunda REST API
                .asyncResponseTimeout(10000)
                .build();

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:5678/webhook/camunda-trigger") // n8n webhook URL
                .build();

        client.subscribe("call-n8n")
                .lockDuration(20000)
                .handler((externalTask, externalTaskService) -> {
                    // Prepare payload
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("processInstanceId", externalTask.getProcessInstanceId());
                    payload.put("businessKey", externalTask.getBusinessKey());
                    payload.put("policyId", externalTask.getVariable("policyId"));
                    payload.put("timestamp", System.currentTimeMillis());

                    // Call n8n webhook
                    Map response = webClient.post()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(payload)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .timeout(Duration.ofSeconds(20))
                            .onErrorMap(throwable -> new RuntimeException("n8n call failed: " + throwable.getMessage(), throwable))
                            .block();

                    // Complete the external task with the n8n result
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("n8nResult", response != null && response.containsKey("result") ? response.get("result") : response);

                    externalTaskService.complete(externalTask, variables);
                    System.out.println("Completed external task for processInstanceId: " + externalTask.getProcessInstanceId());
                })
                .open();
    }
}

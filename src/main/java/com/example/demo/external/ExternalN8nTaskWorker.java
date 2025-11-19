package com.example.demo.external;

import com.example.demo.external.AbstractExternalTaskWorker;
import io.holunda.camunda.bpm.data.CamundaBpmData;
import io.holunda.camunda.bpm.data.factory.VariableFactory;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static io.holunda.camunda.bpm.data.CamundaBpmData.customVariable;

@Slf4j
@Component
public class ExternalN8nTaskWorker extends AbstractExternalTaskWorker {

    public static final String WORKER_ID = "externalN8nTaskWorker";
    public static final String TOPIC = "call-n8n";
    
    // Define the variable factory at class level
    public static final VariableFactory<Object> N8N_RESULT = customVariable("n8nResult", Object.class);

    private final WebClient webClient;

    @Autowired
    public ExternalN8nTaskWorker(ExternalTaskService externalTaskService, WebClient.Builder webClientBuilder) {
        super(externalTaskService, WORKER_ID, TOPIC);
        this.webClient = webClientBuilder.baseUrl("http://localhost:5678/webhook/camunda-trigger").build();
    }

    @Override
    protected void perform(LockedExternalTask externalTask) {
        long startTime = System.currentTimeMillis();
        try {
            // Prepare payload from process variables
            Map<String, Object> payload = new HashMap<>();
            payload.put("processInstanceId", externalTask.getProcessInstanceId());
            payload.put("businessKey", externalTask.getBusinessKey());
            payload.put("policyId", externalTask.getVariables().get("policyId"));
            payload.put("timestamp", System.currentTimeMillis());

            // Call n8n webhook
            Map response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            log.info("N8n response: {}", response);

            // Complete the external task with the n8n result
            getExternalTaskService().complete(
                externalTask.getId(),
                getWorkerId(),
                CamundaBpmData.builder()
                    .set(N8N_RESULT, response != null && response.containsKey("result") ? response.get("result") : response)
                    .build()
            );

            log.info("TOTAL_TIME_TAKEN method=externalN8nTaskWorker process=bpmn processInstanceId={} responseTime={}",
                externalTask.getProcessInstanceId(),
                System.currentTimeMillis() - startTime);
        } catch (Exception ex) {
            log.error("Error in ExternalN8nTaskWorker", ex);
        }
    }
}
package com.example.demo;

import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.springframework.stereotype.Service;

@Service
public class ProcessStarterService {
    private final RuntimeService runtimeService;

    public ProcessStarterService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public Object startProcessAndGetN8nResult(String processKey, String businessKey, Map<String, Object> variables) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(processKey, businessKey, variables);
        // Wait for delegate to complete and variable to be set (simple polling)
        Object n8nResult = null;
        int retries = 20;
        while (retries-- > 0) {
            VariableInstance var = runtimeService.createVariableInstanceQuery()
                .processInstanceIdIn(instance.getProcessInstanceId())
                .variableName("n8nResult")
                .singleResult();
            if (var != null) {
                n8nResult = var.getValue();
                break;
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return n8nResult;
    }
}
package com.example.demo.external;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractExternalTaskWorker implements ExternalTaskWorker {


    @VisibleForTesting
    @Getter
    @Value("${externalTask.defaultLockDurationInMillis:5000}")
    public Long defaultLockDurationInMillis;
    @Value("${externalTask.retryCount:3}")
    public int retryCount;
    @Value("${externalTask.maxTask:5}")
    public int maxTaskCount;
    @Value("${externalTask.retryTimeOut:10}")
    public Long retryTimeout;
    @Getter
    private final ExternalTaskService externalTaskService;

    @Getter
    private final String workerId;

    @Getter
    private final String topic;

    @Scheduled(initialDelayString = "${externalTask.workerInitialDelayInMillis}", fixedDelayString = "${externalTask.workerFixedDelayInMillis:500}")
    public void run() {
        fetchAndLock().forEach(it -> {
            try {
                perform(it);
            } catch (Exception e) {

                getExternalTaskService().handleFailure(
                        it.getId(),
                        getWorkerId(),
                        e.getMessage(),
                        Arrays.toString(e.getStackTrace()),
                        calculateRetries(it),
                        retryTimeout
                );
            }
        });

    }

    protected Integer calculateRetries(LockedExternalTask externalTask) {
        if (externalTask.getRetries() == null) {
            return retryCount;
        } else {
            return externalTask.getRetries() - 1;
        }
    }

    protected abstract void perform(LockedExternalTask externalTask);

    protected List<LockedExternalTask> fetchAndLock() {
        return getExternalTaskService().fetchAndLock(maxTaskCount, getWorkerId())
                .topic(getTopic(), getDefaultLockDurationInMillis())
                .enableCustomObjectDeserialization()
                .execute();
    }
}

# Spring Boot + Camunda BPM + n8n Integration (Gradle)

This repo contains a minimal Spring Boot application embedding the Camunda BPM
process engine. A simple BPMN process calls out to an n8n workflow via a synchronous
service task implemented as a JavaDelegate.

## Features
- Spring Boot 3.x
- Camunda BPM 7.19
- Gradle build tool
- Embedded H2 database
- BPMN process with diagram coordinates
- JavaDelegate that calls an n8n webhook using WebClient
- REST endpoint to start process instances

## Start the application


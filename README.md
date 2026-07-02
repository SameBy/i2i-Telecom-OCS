## 🌐 [LIVE DEMO CONSOLE](http://136.115.252.54:8080/index.html)

## 🌐 [LIVE DEMO CONSOLE](http://136.115.252.54:8080/index.html)

Markdown
# Distributed, Event-Driven Online Charging System (OCS) Simulation

A production-grade, asynchronous Online Charging System (OCS) simulation built with **Java 17**, **Spring Boot 3.x**, and **Apache Kafka**. The entire production pipeline is deployed across a multi-instance virtualized infrastructure on **Google Cloud Platform (GCP)** to achieve strict physical process isolation, high availability, and resilient fault tolerance.

---

## 🎯 The Core Telecom Problem & Architectural Solution

### The Problem
Traditional telecommunications networks require real-time call authorization and sub-millisecond charging checks. Relying on synchronous HTTP/REST communication between an API Gateway and a core Charging Engine introduces massive bottlenecks, high latency, tight coupling, and a single point of failure (SPOF) if the charging backend undergoes downtime.

### The Solution
This project implements an **Event-Driven Microservices Architecture** using a publish-subscribe model backed by a managed **Apache Kafka** cluster. By transforming synchronous service calls into asynchronous event streams, the system guarantees that the API Gateway can process ingress traffic instantly while data consistency and charging updates are processed reliably out-of-band by an isolated worker node.

---

## 🏗️ Multi-VM System Architecture & Topology

The deployment environment utilizes a strict **Process & Codebase Isolation** methodology across two distinct Google Cloud Compute Engine virtual machines (Ubuntu 24.04 LTS):

   [ Client / Frontend Triggers ]
                 │
                 ▼ (HTTP POST /authorize-call)
┌─────────────────────────────────────────────────────────┐
│ VM-1: cpsservercallauthserver                           │
│ (Authorization Server / API Gateway - Port 8080)        │
│                                                         │
│  - Processes Ingress Network Traffic                    │
│  - In-Memory Validation (CustomerDB Engine)            │
│  - Enforces Business Rules (Barring, Active Checks)     │
│  - Acts as Apache Kafka PRODUCER                        │
└───────────────────────────┬─────────────────────────────┘
│
▼ (Asynchronous Event Stream)
🚀 [ Apache Kafka Cluster Backplane ]
🌐 IP: 136.115.252.54:9092
📋 Topic: telecom-charging-logs
│
▼ (Reliable Message Delivery)
┌─────────────────────────────────────────────────────────┐
│ VM-2: gcpserver2chargingsmsservice                      │
│ (Real-Time Charging Engine - Port 8081)                 │
│                                                         │
│  - Acts as Apache Kafka CONSUMER                        │
│  - Handles Asynchronous Real-Time Balance Deductions    │
│  - Evaluates Threshold Alarms (Critical Balance)        │
│  - Dispatches SMS Alerts via Telegram Bot API Proxy     │
└─────────────────────────────────────────────────────────┘


### 📶 Service 1: Authorization Server (VM-1)
* **Ingress Boundary:** Intercepts live call network requests via a secure REST endpoint.
* **Stateless Rule Engine:** Evaluates subscriber metadata instantly from an optimized, in-memory state repository. It determines whether a subscriber exists, checks if domestic/international restrictions apply, and verifies if the current balance is non-zero.
* **Event Dispatcher:** Instead of altering data records directly, it translates the call state into an immutable `CallEvent` packet and publishes it instantaneously to the Kafka grid.

### 💰 Service 2: Charging Engine & Notification Subsystem (VM-2)
* **Ingress Boundary:** Deployed in an isolated environment with zero external HTTP exposure. It continuously polls the cloud-managed Kafka cluster.
* **Deterministic Charging State:** Consumes `CallEvent` structures, computes precise balance updates, updates the isolated internal state tracking, and monitors package exhaustion thresholds.
* **SMS Gateway Proxy:** Dynamically formats telco notification payloads and dispatches immediate structural alerts to the customer via a secure, asynchronous multi-threaded integration with the Telegram Bot API.

---

## ⚙️ Key Software Engineering Decisions & Resilience Mechanics

### 1. Zero-Leak Codebase & Physical Package Isolation
During deployment optimization, a critical implicit behavior of Spring Boot's implicit application scanning context was observed: pooling all source files within a monolithic directory structure caused the API node to implicitly scan and activate the Kafka listener components (`@Component` / `@Service`). 
* **The Fix:** The source modules were strictly segregated. The `service2` package structure was completely removed from the physical storage layer of VM-1, and `service1` was completely excised from VM-2. This guarantees absolute runtime isolation, eliminating any risk of split-brain executions or phantom listeners.

### 2. High-Availability Messaging Defends System Failures
The decoupling power of this state machine was validated through a controlled infrastructure stress-test:
* **The Experiment:** VM-2 (Charging Engine) was entirely shut down using a SIGKILL signal while VM-1 was subjected to heavy ingress load.
* **The Result:** VM-1 successfully accepted incoming traffic and safely archived the execution records directly into the persistence buffers of the Apache Kafka cluster. Upon the complete cold boot and initialization of VM-2, the state engine pulled the backlogged queue, dynamically updated current customer balance limits, and fired all pending notification triggers without a single packet dropping.

---

## 🚀 API Specification & Testing Endpoints

### Authorization Request
* **Endpoint:** `POST http://<VM-1_EXTERNAL_IP>:8080/authorize-call`
* **Content-Type:** `application/x-www-form-urlencoded`

**Parameters:**
| Key | Type | Description |
| :--- | :--- | :--- |
| `msisdn` | `String` | Unique Subscriber Mobile Number |
| `destination` | `String` | Target Receiving Number |
| `duration` | `int` | Requested Call Duration Unit (processed as minutes/seconds) |
| `isInternational` | `boolean` | *(Optional)* Explicit flag to toggle high-tariff international routing |

#### Sample Live Test Scripts
```bash
# Scenario A: Standard Approved Call (Triggers Balances Updates & Warning Alarms)
curl -X POST http://<VM-1_EXTERNAL_IP>:8080/authorize-call -d "msisdn=5551112233" -d "destination=5554445566" -d "duration=12"

# Scenario B: Denied Call (Triggers Immediate Low Balance Warnings)
curl -X POST http://<VM-1_EXTERNAL_IP>:8080/authorize-call -d "msisdn=5557778899" -d "destination=5551112233" -d "duration=60"

# Scenario C: Barred Profile Routing Block
curl -X POST http://<VM-1_EXTERNAL_IP>:8080/authorize-call -d "msisdn=5554445566" -d "destination=12125550199" -d "isInternational=true" -d "duration=5"

🛠️ How to Deploy & Initialize
Prerequisites
Ensure Java 17 (OpenJDK) and Apache Maven are provisioned on both remote compute modules.

Phase 1: Initialize API Gateway Node (VM-1)
Bash
git clone https://github.com/SameBy/i2i-Telecom-OCS.git
cd i2i-Telecom-OCS
rm -rf src/main/java/com/i2i/telecom/service2

nohup mvn spring-boot:run \
  -Dstart-class=com.i2i.telecom.service1.Service1Application \
  -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=service1 -Dserver.port=8080" \
  > service1.log 2>&1 &
Phase 2: Initialize Charging Backend Worker (VM-2)
Bash
git clone https://github.com/SameBy/i2i-Telecom-OCS.git
cd i2i-Telecom-OCS
rm -rf src/main/java/com/i2i/telecom/service1

nohup mvn spring-boot:run \
  -Dstart-class=com.i2i.telecom.service2.Service2Application \
  -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=service2 -Dserver.port=8081" \
  > service2.log 2>&1 &

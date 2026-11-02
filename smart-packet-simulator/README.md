# Smart Packet Delivery and Self-Healing Network Simulator

A Spring Boot-based simulator for packet delivery, routing, error control, and self-healing networks.

## Phase 1 — Project Setup and Backend Initialization

This phase establishes a clean, runnable Spring Boot project with the required
folder structure, a health-check REST API, a minimal responsive frontend
placeholder, and automated tests. No packet simulation logic is implemented yet.

### Phase 1 Scope

- Maven project with Spring Boot parent, Spring Web, Spring Validation, DevTools, and Test starters.
- Main application class rooted at `com.network.simulator`.
- `GET /api/health` endpoint returning HTTP 200 with JSON `{ "status": "UP", "application": "Smart Packet Simulator" }`.
- Minimal responsive frontend at `http://localhost:8080/` with a backend status button.
- Spring Boot test verifying the health endpoint contract.
- Basic development logging and configuration.

### Prerequisites

- **Java 17 or later** (this project targets Java 17).
- **Windows** with PowerShell.
- **Maven** is provided through the Maven wrapper (`mvnw.cmd`), so no global
  Maven installation is required. The wrapper will download Maven 3.9.9 on first run.
- Internet access to download dependencies from Maven Central.

### Project Structure

```
smart-packet-simulator/
├── .gitignore
├── pom.xml
├── mvnw
├── mvnw.cmd
├── README.md
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/com/network/simulator/
│   │   │   ├── SimulatorApplication.java
│   │   │   ├── controller/
│   │   │   │   └── HealthController.java
│   │   │   ├── model/
│   │   │   │   └── HealthResponse.java
│   │   │   ├── service/        (future phases)
│   │   │   ├── routing/        (future phases)
│   │   │   ├── errorcontrol/   (future phases)
│   │   │   └── transport/      (future phases)
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   │           ├── index.html
│   │           ├── css/style.css
│   │           ├── js/api.js
│   │           └── js/app.js
│   └── test/java/com/network/simulator/
│       └── controller/
│           └── HealthControllerTest.java
```

### How to Run

Open PowerShell in the project directory and run the wrapper:

```powershell
cd E:\CN\smart-packet-simulator
./mvnw.cmd spring-boot:run
```

The application starts on `http://localhost:8080`.

To run the packaged jar:

```powershell
./mvnw.cmd package
java -jar target/smart-packet-simulator-0.1.0-SNAPSHOT.jar
```

### How to Test the Health Endpoint

```powershell
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP",
  "application": "Smart Packet Simulator"
}
```

You can also open `http://localhost:8080/` in a browser and click
**Check Backend Status**.

### How to Run Tests

```powershell
./mvnw.cmd test
```

The test suite includes `HealthControllerTest`, which starts the full Spring
context on a random port and asserts the health endpoint contract.

### Future Phases

Routing algorithms, packet transmission, CRC/checksum, and self-healing
mechanisms will be implemented in later phases. The package structure
(`routing`, `errorcontrol`, `transport`, `service`) is reserved for them.
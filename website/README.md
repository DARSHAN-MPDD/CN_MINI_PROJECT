# website

Frontend website for the CN (Computer Networks) Smart Packet Delivery and Self-Healing Network Simulator project.

## Structure

```
website/
├── index.html      # Landing page
├── css/
│   └── style.css   # Styles
├── js/
│   └── app.js      # Backend health polling
└── README.md
```

## Backend

The website polls `GET /api/health` on the Spring Boot backend (default `http://localhost:8080`) to show whether the simulator is running.

Run the backend:

```powershell
cd E:\CN\smart-packet-simulator
./mvnw.cmd spring-boot:run
```

Then open `website/index.html` in a browser, or serve it with any static file server.

## Git

Repository: https://github.com/DARSHAN-MPDD/CN.git
Remote: `origin`

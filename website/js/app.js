const statusIndicator = document.getElementById("statusIndicator");
const statusText = document.getElementById("statusText");

async function checkBackend() {
    try {
        const response = await fetch("/api/health", { mode: "no-cors" });
        statusText.textContent = "Backend reachable";
        statusIndicator.className = "status-indicator online";
    } catch (error) {
        statusText.textContent = "Backend not reachable (start the Spring Boot app)";
        statusIndicator.className = "status-indicator offline";
    }
}

checkBackend();
setInterval(checkBackend, 15000);

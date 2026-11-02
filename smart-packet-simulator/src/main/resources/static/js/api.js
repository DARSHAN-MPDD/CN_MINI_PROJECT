/**
 * api.js — API wrapper for Phase 3/4 endpoints
 */
(function (window) {
    "use strict";

    var api = {};

    api.fetchHealth = function () {
        return fetch("/api/health", { method: "GET" })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Backend responded with HTTP " + response.status);
                }
                return response.json();
            });
    };

    api.getTopology = function () {
        return fetch("/api/topology", { method: "GET" })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Backend responded with HTTP " + response.status);
                }
                return response.json();
            });
    };

    api.loadSampleTopology = function () {
        return fetch("/api/topology/sample", { method: "POST" })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Backend responded with HTTP " + response.status);
                }
                return response.json();
            });
    };

    api.deliverPacket = function (source, destination, payload) {
        return fetch("/api/packets/deliver", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ source: source, destination: destination, payload: payload })
        }).then(function (response) {
            return response.json();
        });
    };

    api.failLink = function (linkId) {
        return fetch("/api/failures/link/" + encodeURIComponent(linkId), { method: "POST" })
            .then(function (response) {
                return response.json();
            });
    };

    api.failNode = function (nodeId) {
        return fetch("/api/failures/node/" + encodeURIComponent(nodeId), { method: "POST" })
            .then(function (response) {
                return response.json();
            });
    };

    api.recoverLink = function (linkId) {
        return fetch("/api/recovery/link/" + encodeURIComponent(linkId), { method: "POST" })
            .then(function (response) {
                return response.json();
            });
    };

    api.recoverNode = function (nodeId) {
        return fetch("/api/recovery/node/" + encodeURIComponent(nodeId), { method: "POST" })
            .then(function (response) {
                return response.json();
            });
    };

    api.recalculateRoute = function (source, destination, oldRoute) {
        return fetch("/api/routing/recalculate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ source: source, destination: destination, oldRoute: oldRoute })
        }).then(function (response) {
            return response.json();
        });
    };

    api.getFailureStatus = function () {
        return fetch("/api/failures/status", { method: "GET" })
            .then(function (response) {
                return response.json();
            });
    };

    api.getHistory = function () {
        return fetch("/api/failures/history", { method: "GET" })
            .then(function (response) {
                return response.json();
            });
    };

    api.getMonitoringOverview = function () {
        return fetch("/api/monitoring/overview", { method: "GET" })
            .then(function (response) {
                return response.json();
            });
    };

    api.getMonitoringPackets = function () {
        return fetch("/api/monitoring/packets", { method: "GET" })
            .then(function (response) {
                return response.json();
            });
    };

    api.getMonitoringPerformance = function () {
        return fetch("/api/monitoring/performance", { method: "GET" })
            .then(function (response) {
                return response.json();
            });
    };

    api.getMonitoringLinks = function () {
        return fetch("/api/monitoring/links", { method: "GET" })
            .then(function (response) {
                return response.json();
            });
    };

    api.getMonitoringHistory = function () {
        return fetch("/api/monitoring/history", { method: "GET" })
            .then(function (response) {
                return response.json();
            });
    };

    api.getMonitoringAvailability = function () {
        return fetch("/api/monitoring/availability", { method: "GET" })
            .then(function (response) {
                return response.json();
            });
    };

    api.takeMonitoringSnapshot = function () {
        return fetch("/api/monitoring/snapshot", { method: "POST" })
            .then(function (response) {
                return response.json();
            });
    };

    window.SmartPacketApi = api;
})(window);


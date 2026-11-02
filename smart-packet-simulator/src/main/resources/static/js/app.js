/**
 * app.js — Client-side router and per-topic view logic for the simulator frontend.
 * The existing backend API contract (api.js) is preserved unchanged.
 */
(function (window, document) {
    "use strict";

    var VIEWS = ["home", "overview", "delivery", "routing", "failure", "monitoring"];
    var DEFAULT_VIEW = "home";

    var state = {
        topology: { nodes: [], links: [] },
        lastRoute: null,
        lastRouteResult: null,
        overview: null,
        packets: [],
        links: [],
        history: [],
        availability: null,
        charts: {}
    };

    // Shared element lookups (guarded; elements exist for all views in the single page).
    var navLinks = document.querySelectorAll(".navbar-menu a[data-view]");
    var statusDot = document.getElementById("status-dot");
    var statusText = document.getElementById("status-text");

    function setStatus(stateName, text) {
        if (!statusDot || !statusText) {
            return;
        }
        statusText.textContent = text;
        statusDot.className = "status-dot status-" + stateName;
    }

    function checkHealth() {
        if (!window.SmartPacketApi) {
            setStatus("error", "Backend: API module not loaded");
            return;
        }
        window.SmartPacketApi.fetchHealth()
            .then(function (data) {
                setStatus("success", "Backend: " + (data.status || "UP"));
            })
            .catch(function () {
                setStatus("error", "Backend: unavailable");
            });
    }

    // ---------- Router ----------
    function getViewFromHash() {
        var hash = window.location.hash.replace(/^#/, "");
        if (!hash || VIEWS.indexOf(hash) === -1) {
            return DEFAULT_VIEW;
        }
        return hash;
    }

    function setActiveNav(view) {
        navLinks.forEach(function (link) {
            var isActive = link.getAttribute("data-view") === view;
            link.setAttribute("aria-current", isActive ? "page" : "false");
            if (isActive) {
                link.classList.add("active");
            } else {
                link.classList.remove("active");
            }
        });
    }

    function showView(view) {
        var sections = document.querySelectorAll("main section.view");
        sections.forEach(function (section) {
            if (section.getAttribute("data-view") === view) {
                section.removeAttribute("hidden");
                section.style.display = section.getAttribute("data-view") === "home" ? "flex" : "block";
                section.focus();
            } else {
                section.setAttribute("hidden", "");
                section.style.display = "none";
            }
        });
        setActiveNav(view);

        if (view === "overview") {
            renderOverviewView();
        } else if (view === "delivery") {
            renderDeliveryView();
        } else if (view === "routing") {
            renderRoutingView();
        } else if (view === "failure") {
            renderFailureView();
        } else if (view === "monitoring") {
            renderMonitoringView();
        }
    }

    function route() {
        showView(getViewFromHash());
    }

    function bindCardClicks() {
        document.querySelectorAll(".topic-card[data-view]").forEach(function (card) {
            function go() {
                window.location.hash = "#" + card.getAttribute("data-view");
            }
            card.addEventListener("click", go);
            card.addEventListener("keydown", function (e) {
                if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    go();
                }
            });
        });
    }

    function bindNavToggle() {
        var toggle = document.querySelector(".nav-toggle");
        var menu = document.getElementById("navbar-menu");
        if (!toggle || !menu) {
            return;
        }
        toggle.addEventListener("click", function () {
            var expanded = toggle.getAttribute("aria-expanded") === "true";
            toggle.setAttribute("aria-expanded", String(!expanded));
            menu.classList.toggle("open");
        });
        navLinks.forEach(function (link) {
            link.addEventListener("click", function () {
                toggle.setAttribute("aria-expanded", "false");
                menu.classList.remove("open");
            });
        });
    }

    // ---------- Topology (shared) ----------
    function renderTopologyLists() {
        var nodes = state.topology.nodes || [];
        var links = state.topology.links || [];

        function fillList(id, items, render) {
            var el = document.getElementById(id);
            if (!el) {
                return;
            }
            el.innerHTML = "";
            items.forEach(function (item) {
                var li = document.createElement("li");
                li.textContent = render(item);
                el.appendChild(li);
            });
        }

        fillList("ov-nodes-list", nodes, function (n) {
            return (n.id || "") + " [" + (n.type || "") + "] - " + (n.status || "ACTIVE");
        });
        fillList("ov-links-list", links, function (l) {
            return (l.id || "") + ": " + (l.sourceNodeId || "") + " - " + (l.targetNodeId || "") + " (cost " + (l.cost || 0) + ") [" + (l.status || "ACTIVE") + "]";
        });

        var countEls = {
            "ov-node-count": nodes.length,
            "ov-link-count": links.length,
            "ov-nodes-total": nodes.length,
            "ov-links-total": links.length
        };
        Object.keys(countEls).forEach(function (id) {
            var el = document.getElementById(id);
            if (el) {
                el.textContent = countEls[id];
            }
        });

        // Availability
        var activeNodes = 0, failedNodes = 0, activeLinks = 0, failedLinks = 0;
        nodes.forEach(function (n) {
            if ((n.status || "ACTIVE").toUpperCase() === "FAILED") {
                failedNodes++;
            } else {
                activeNodes++;
            }
        });
        links.forEach(function (l) {
            if ((l.status || "ACTIVE").toUpperCase() === "FAILED") {
                failedLinks++;
            } else {
                activeLinks++;
            }
        });
        var meta = {
            "ov-active-nodes": activeNodes,
            "ov-failed-nodes": failedNodes,
            "ov-active-links": activeLinks,
            "ov-failed-links": failedLinks
        };
        Object.keys(meta).forEach(function (id) {
            var el = document.getElementById(id);
            if (el) {
                el.textContent = meta[id];
            }
        });

        renderSvg(document.getElementById("ov-topo-svg"), nodes, links);
        var empty = document.getElementById("ov-topo-empty");
        if (empty) {
            empty.style.display = (links.length > 0 || nodes.length > 0) ? "none" : "block";
        }
    }

    function renderAvailability() {
        var avail = state.availability;
        var showAvail = Boolean(avail && avail.totalNodes != null);
        var ov = document.getElementById("ov-availability");
        var mon = document.getElementById("mon-availability");
        if (ov) {
            ov.style.display = showAvail ? "block" : "none";
        }
        if (mon) {
            mon.style.display = showAvail ? "block" : "none";
        }
        if (!showAvail) {
            return;
        }
        function set(id, value) {
            var el = document.getElementById(id);
            if (el) {
                el.textContent = value == null ? "\u2014" : value;
            }
        }
        set("ov-node-availability", avail.nodeAvailabilityPercent == null ? "\u2014" : Number(avail.nodeAvailabilityPercent).toFixed(1) + "%");
        set("ov-link-availability", avail.linkAvailabilityPercent == null ? "\u2014" : Number(avail.linkAvailabilityPercent).toFixed(1) + "%");
        set("ov-overall-availability", avail.overallAvailabilityPercent == null ? "\u2014" : Number(avail.overallAvailabilityPercent).toFixed(1) + "%");
        set("m-node-availability", avail.nodeAvailabilityPercent == null ? "\u2014" : Number(avail.nodeAvailabilityPercent).toFixed(1) + "%");
        set("m-link-availability", avail.linkAvailabilityPercent == null ? "\u2014" : Number(avail.linkAvailabilityPercent).toFixed(1) + "%");
        set("m-overall-availability", avail.overallAvailabilityPercent == null ? "\u2014" : Number(avail.overallAvailabilityPercent).toFixed(1) + "%");
    }

    var SVG_NS = "http://www.w3.org/2000/svg";

    function labelPosition(p1, p2, side, offset) {
        if (!side || !offset) {
            return { x: (p1.x + p2.x) / 2, y: (p1.y + p2.y) / 2 };
        }
        var dx = p2.x - p1.x;
        var dy = p2.y - p1.y;
        var len = Math.sqrt(dx * dx + dy * dy) || 1;
        var nx = -dy / len;
        var ny = dx / len;
        return {
            x: (p1.x + p2.x) / 2 + nx * side * offset,
            y: (p1.y + p2.y) / 2 + ny * side * offset
        };
    }

    function renderSvg(svg, nodes, links) {
        if (!svg) {
            return;
        }
        svg.innerHTML = "";
        var positions = {
            "S1": { x: 80, y: 160 },
            "R1": { x: 240, y: 80 },
            "R2": { x: 240, y: 240 },
            "R3": { x: 420, y: 160 },
            "R4": { x: 420, y: 60 },
            "D1": { x: 600, y: 160 }
        };

        // Group links by unordered endpoint pair so opposite-direction links
        // (e.g. L1 S1->R1 and L2 R1->S1) get labels on opposite sides of the edge.
        var groups = {};
        links.forEach(function (l) {
            var a = l.sourceNodeId;
            var b = l.targetNodeId;
            if (!a || !b) {
                return;
            }
            var key = a < b ? a + "|" + b : b + "|" + a;
            if (!groups[key]) {
                groups[key] = [];
            }
            groups[key].push(l);
        });
        var linkOffsets = {};
        Object.keys(groups).forEach(function (key) {
            groups[key].forEach(function (l, i) {
                var ring = Math.floor(i / 2) + 1;
                var side = i % 2 === 0 ? -ring : ring;
                linkOffsets[l.id || ""] = { side: side, offset: 18 };
            });
        });

        // 1. Edges first.
        links.forEach(function (l) {
            var p1 = positions[l.sourceNodeId];
            var p2 = positions[l.targetNodeId];
            if (!p1 || !p2) {
                return;
            }
            var line = document.createElementNS(SVG_NS, "line");
            line.setAttribute("x1", p1.x);
            line.setAttribute("y1", p1.y);
            line.setAttribute("x2", p2.x);
            line.setAttribute("y2", p2.y);
            var status = (l.status || "ACTIVE").toLowerCase();
            line.setAttribute("stroke", status === "failed" ? "#ef4444" : "#38bdf8");
            line.setAttribute("stroke-width", "3");
            line.setAttribute("pointer-events", "none");
            svg.appendChild(line);
        });

        // 2. Link labels with background badges, offset per edge.
        links.forEach(function (l) {
            var p1 = positions[l.sourceNodeId];
            var p2 = positions[l.targetNodeId];
            if (!p1 || !p2) {
                return;
            }
            var status = (l.status || "ACTIVE").toLowerCase();
            var label = (l.id || "") + " \u00b7 " + (l.cost || 0);
            var info = linkOffsets[l.id || ""] || { side: 0, offset: 0 };
            var pos = labelPosition(p1, p2, info.side, info.offset);

            var g = document.createElementNS(SVG_NS, "g");
            g.setAttribute("pointer-events", "none");
            var text = document.createElementNS(SVG_NS, "text");
            text.setAttribute("x", pos.x);
            text.setAttribute("y", pos.y);
            text.setAttribute("text-anchor", "middle");
            text.setAttribute("dominant-baseline", "central");
            text.setAttribute("fill", status === "failed" ? "#fca5a5" : "#cbd5e1");
            text.setAttribute("font-size", "11");
            text.setAttribute("font-weight", "700");
            text.textContent = label;
            g.appendChild(text);
            svg.appendChild(g);

            var bb = text.getBBox();
            var pad = 5;
            var rect = document.createElementNS(SVG_NS, "rect");
            rect.setAttribute("x", bb.x - pad);
            rect.setAttribute("y", bb.y - pad);
            rect.setAttribute("width", bb.width + pad * 2);
            rect.setAttribute("height", bb.height + pad * 2);
            rect.setAttribute("rx", 6);
            rect.setAttribute("ry", 6);
            rect.setAttribute("fill", "#0b1026");
            rect.setAttribute("stroke", "#334155");
            rect.setAttribute("stroke-width", "1");
            rect.setAttribute("pointer-events", "none");
            g.insertBefore(rect, text);
        });

        // 3. Nodes last so they stay on top of edges and labels.
        nodes.forEach(function (n) {
            var p = positions[n.id];
            if (!p) {
                p = { x: 380, y: 160 };
            }
            var circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
            circle.setAttribute("cx", p.x);
            circle.setAttribute("cy", p.y);
            circle.setAttribute("r", "22");
            var status = (n.status || "ACTIVE").toLowerCase();
            circle.setAttribute("fill", status === "failed" ? "#7f1d1d" : "#1e293b");
            circle.setAttribute("stroke", status === "failed" ? "#ef4444" : "#38bdf8");
            circle.setAttribute("stroke-width", "2");
            svg.appendChild(circle);
            var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
            text.setAttribute("x", p.x);
            text.setAttribute("y", p.y + 4);
            text.setAttribute("text-anchor", "middle");
            text.setAttribute("fill", "#e2e8f0");
            text.setAttribute("font-size", "12");
            text.textContent = n.id;
            svg.appendChild(text);
        });
    }

    function fillSelect(selectId, nodes, links, mode) {
        var el = document.getElementById(selectId);
        if (!el) {
            return;
        }
        el.innerHTML = "";
        if (mode === "nodes" || mode === "all") {
            nodes.forEach(function (n) {
                var opt = document.createElement("option");
                opt.value = n.id;
                opt.textContent = n.id + " (" + n.type + ")";
                el.appendChild(opt);
            });
        }
        if (mode === "links" || mode === "all") {
            links.forEach(function (l) {
                var opt = document.createElement("option");
                opt.value = l.id;
                opt.textContent = l.id + ": " + l.sourceNodeId + "-" + l.targetNodeId + " [" + (l.status || "ACTIVE") + "]";
                el.appendChild(opt);
            });
        }
    }

    function loadTopology() {
        if (!window.SmartPacketApi) {
            return Promise.resolve();
        }
        return window.SmartPacketApi.getTopology()
            .then(function (data) {
                state.topology.nodes = data.nodes || [];
                state.topology.links = data.links || [];
                renderTopologyLists();
            })
            .catch(function (err) {
                console.error(err);
            });
    }

    function loadSample() {
        if (!window.SmartPacketApi) {
            return Promise.resolve();
        }
        var btn = document.getElementById("ov-load-sample");
        if (btn) {
            btn.disabled = true;
        }
        return window.SmartPacketApi.loadSampleTopology()
            .then(function (data) {
                state.topology.nodes = data.nodes || [];
                state.topology.links = data.links || [];
                renderTopologyLists();
                var res = document.getElementById("fl-action-result");
                if (res) {
                    res.textContent = "Sample topology loaded.";
                }
            })
            .catch(function (err) {
                var res = document.getElementById("fl-action-result");
                if (res) {
                    res.textContent = "Error loading sample: " + err;
                }
            })
            .then(function () {
                if (btn) {
                    btn.disabled = false;
                }
            });
    }

    // ---------- View: Overview ----------
    function renderOverviewView() {
        renderTopologyLists();
        renderAvailability();
        loadAvailability();
    }

    function loadAvailability() {
        if (!window.SmartPacketApi) {
            return;
        }
        window.SmartPacketApi.getMonitoringAvailability()
            .then(function (data) {
                state.availability = data;
                renderAvailability();
            })
            .catch(function () {
                // availability is supplementary; hide silently if unsupported
            });
    }

    // ---------- View: Packet Delivery ----------
    function renderDeliveryView() {
        var nodes = state.topology.nodes || [];
        var links = state.topology.links || [];
        fillSelect("dl-source-select", nodes, links, "nodes");
        fillSelect("dl-dest-select", nodes, links, "nodes");
        var hasNodes = nodes.length > 0;
        var btn = document.getElementById("dl-deliver-packet");
        if (btn) {
            btn.disabled = !hasNodes;
        }
        renderDeliveryResult(null);
    }

    function renderRouteSteps(el, route) {
        if (!el) {
            return;
        }
        el.innerHTML = "";
        if (!route || route.length === 0) {
            el.textContent = "\u2014";
            return;
        }
        route.forEach(function (node, i) {
            var step = document.createElement("span");
            step.className = "route-step";
            step.textContent = node;
            el.appendChild(step);
            if (i < route.length - 1) {
                var arrow = document.createElement("span");
                arrow.className = "route-arrow";
                arrow.textContent = "\u2192";
                el.appendChild(arrow);
            }
        });
    }

    function renderDeliveryResult(data) {
        var panel = document.getElementById("dl-result-card");
        var empty = document.getElementById("dl-result-empty");
        var updated = document.getElementById("dl-result-updated");
        var statusRow = document.getElementById("dl-result-status");
        var statusText = document.getElementById("dl-result-status-text");
        var dot = document.querySelector("#dl-result-status .result-dot");

        if (!data) {
            if (panel) panel.style.display = "none";
            if (empty) empty.style.display = "block";
            if (updated) setText(updated, "No delivery attempt yet.");
            return;
        }

        if (empty) empty.style.display = "none";
        if (panel) panel.style.display = "block";
        if (updated) setText(updated, "Latest response from API");

        var status = data.status || "UNKNOWN";
        var cls = String(status).toLowerCase().replace(/\s+/g, "-");
        if (statusText) {
            statusText.textContent = status;
        }
        if (statusRow) {
            statusRow.className = "result-status " + cls;
        }
        if (dot) {
            dot.className = "result-dot " + cls;
        }

        setEl("dl-result-packet-id", data.packetId || "\u2014");
        setEl("dl-result-source", data.sourceNodeId || "\u2014");
        setEl("dl-result-destination", data.destinationNodeId || "\u2014");
        setEl("dl-result-payload", data.payload != null ? data.payload : "\u2014");

        var route = data.route || [];
        var routeEl = document.getElementById("dl-result-route");
        var stepsEl = document.getElementById("dl-route-steps");
        if (routeEl) {
            routeEl.textContent = route.length ? route.join(" \u2192 ") : "\u2014";
        }
        renderRouteSteps(stepsEl, route);

        setEl("dl-result-cost", data.routeCost != null ? data.routeCost : "\u2014");
        setEl("dl-result-duration", "\u2014");

        var reasonRow = document.getElementById("dl-result-reason-row");
        var reasonEl = document.getElementById("dl-result-reason");
        if (data.errorMessage) {
            if (reasonRow) {
                reasonRow.style.display = "flex";
            }
            setEl("dl-result-reason", data.errorMessage);
        } else if (reasonRow) {
            reasonRow.style.display = "none";
        }

        var rawEl = document.getElementById("dl-delivery-result-raw");
        if (rawEl) {
            rawEl.textContent = JSON.stringify(data, null, 2);
        }
        bindRawToggle("dl-toggle-raw", "dl-raw-response", "dl-delivery-result-raw", "dl-copy-raw");
    }

    function bindRawToggle(toggleId, rawBoxId, rawElId, copyBtnId) {
        var toggle = document.getElementById(toggleId);
        var rawBox = document.getElementById(rawBoxId);
        if (!toggle || !rawBox) {
            return;
        }
        toggle.onclick = null;
        toggle.addEventListener("click", function () {
            var hidden = rawBox.style.display === "none";
            rawBox.style.display = hidden ? "block" : "none";
            toggle.textContent = hidden ? "Hide raw API response" : "View raw API response";
        });
        var copyBtn = document.getElementById(copyBtnId);
        if (copyBtn) {
            copyBtn.onclick = null;
            copyBtn.addEventListener("click", function () {
                var rawEl = document.getElementById(rawElId);
                var text = rawEl ? rawEl.textContent : "";
                var done = navigator.clipboard && navigator.clipboard.writeText
                    ? navigator.clipboard.writeText(text)
                    : Promise.resolve();
                done.then(function () {
                    var prev = copyBtn.textContent;
                    copyBtn.textContent = "Copied!";
                    setTimeout(function () {
                        copyBtn.textContent = prev;
                    }, 1200);
                }, function () {
                    // clipboard access denied; raw text remains selectable
                });
            });
        }
    }

    function renderRouteResult(data, source, dest) {
        if (!source) {
            var sEl = document.getElementById("rt-source-select");
            source = sEl ? sEl.value : null;
        }
        if (!dest) {
            var dEl = document.getElementById("rt-dest-select");
            dest = dEl ? dEl.value : null;
        }
        var panel = document.getElementById("rt-result-card");
        var empty = document.getElementById("rt-result-empty");
        var updated = document.getElementById("rt-result-updated");
        var statusRow = document.getElementById("rt-result-status");
        var statusText = document.getElementById("rt-result-status-text");
        var dot = document.querySelector("#rt-result-status .result-dot");

        if (!data) {
            if (panel) panel.style.display = "none";
            if (empty) empty.style.display = "block";
            if (updated) setText(updated, "No route calculated yet.");
            return;
        }

        if (empty) empty.style.display = "none";
        if (panel) panel.style.display = "block";
        if (updated) setText(updated, "Latest response from API");

        var status = (data.newRoute && data.newRoute.length) ? "DELIVERED" : "FAILED";
        var cls = String(status).toLowerCase().replace(/\s+/g, "-");
        if (statusText) {
            statusText.textContent = status;
        }
        if (statusRow) {
            statusRow.className = "result-status " + cls;
        }
        if (dot) {
            dot.className = "result-dot " + cls;
        }

        setEl("rt-result-packet-id", data.packetId || "\u2014");
        setEl("rt-result-source", source || data.sourceNodeId || "\u2014");
        setEl("rt-result-destination", dest || data.destinationNodeId || "\u2014");

        var route = data.newRoute || data.route || [];
        var routeEl = document.getElementById("rt-result-route");
        var stepsEl = document.getElementById("rt-route-steps");
        if (routeEl) {
            routeEl.textContent = route.length ? route.join(" \u2192 ") : "\u2014";
        }
        renderRouteSteps(stepsEl, route);

        setEl("rt-result-cost", data.newRouteCost != null ? data.newRouteCost : "\u2014");
        setEl("rt-result-rerouted", data.recovered === true ? "Yes" : "No");

        var messageRow = document.getElementById("rt-result-message-row");
        var messageEl = document.getElementById("rt-result-message");
        if (data.message) {
            if (messageRow) {
                messageRow.style.display = "flex";
            }
            setEl("rt-result-message", data.message);
        } else if (messageRow) {
            messageRow.style.display = "none";
        }

        var rawEl = document.getElementById("rt-route-result-raw");
        if (rawEl) {
            rawEl.textContent = JSON.stringify(data, null, 2);
        }
        bindRawToggle("rt-toggle-raw", "rt-raw-response", "rt-route-result-raw", "rt-copy-raw");
    }

    function deliverPacket() {
        if (!window.SmartPacketApi) {
            return;
        }
        var source = document.getElementById("dl-source-select") ? document.getElementById("dl-source-select").value : null;
        var dest = document.getElementById("dl-dest-select") ? document.getElementById("dl-dest-select").value : null;
        var payload = document.getElementById("dl-payload-input") ? document.getElementById("dl-payload-input").value : "";
        if (!source || !dest) {
            var empty = document.getElementById("dl-result-empty");
            var panel = document.getElementById("dl-result-card");
            var updated = document.getElementById("dl-result-updated");
            if (empty) empty.style.display = "block";
            if (panel) panel.style.display = "none";
            if (updated) {
                updated.textContent = "Please select source and destination.";
            }
            return;
        }
        var btn = document.getElementById("dl-deliver-packet");
        if (btn) {
            btn.disabled = true;
        }
        var updated = document.getElementById("dl-result-updated");
        if (updated) {
            updated.textContent = "Delivering\u2026";
        }
        window.SmartPacketApi.deliverPacket(source, dest, payload)
            .then(function (data) {
                state.lastRoute = data.route || [];
                renderDeliveryResult(data);
            })
            .catch(function (err) {
                var empty = document.getElementById("dl-result-empty");
                var panel = document.getElementById("dl-result-card");
                if (empty) empty.style.display = "block";
                if (panel) panel.style.display = "none";
                if (updated) {
                    updated.textContent = "Error: " + err;
                }
            })
            .then(function () {
                if (btn) {
                    btn.disabled = false;
                }
                loadTopology();
                state.overview = null;
                if (getViewFromHash() === "monitoring") {
                    loadMonitoring();
                }
            });
    }

    // ---------- View: Routing ----------
    function renderRoutingView() {
        var nodes = state.topology.nodes || [];
        fillSelect("rt-source-select", nodes, [], "nodes");
        fillSelect("rt-dest-select", nodes, [], "nodes");
        var hasNodes = nodes.length > 0;
        var calc = document.getElementById("rt-calculate-route");
        var recalc = document.getElementById("rt-recalculate-route");
        if (calc) {
            calc.disabled = !hasNodes;
        }
        if (recalc) {
            recalc.disabled = !hasNodes || !state.lastRoute;
        }
        renderRouteResult(state.lastRouteResult || null);
    }

    function calculateRoute() {
        if (!window.SmartPacketApi) {
            return;
        }
        var source = document.getElementById("rt-source-select") ? document.getElementById("rt-source-select").value : null;
        var dest = document.getElementById("rt-dest-select") ? document.getElementById("rt-dest-select").value : null;
        if (!source || !dest) {
            var empty = document.getElementById("rt-result-empty");
            var panel = document.getElementById("rt-result-card");
            var updated = document.getElementById("rt-result-updated");
            if (empty) empty.style.display = "block";
            if (panel) panel.style.display = "none";
            if (updated) {
                updated.textContent = "Please select source and destination.";
            }
            return;
        }
        var btn = document.getElementById("rt-calculate-route");
        if (btn) {
            btn.disabled = true;
        }
        var updated = document.getElementById("rt-result-updated");
        if (updated) {
            updated.textContent = "Calculating\u2026";
        }
        window.SmartPacketApi.recalculateRoute(source, dest, state.lastRoute || [])
            .then(function (data) {
                state.lastRouteResult = data;
                if (data && data.newRoute) {
                    state.lastRoute = data.newRoute;
                }
                renderRouteResult(data, source, dest);
            })
            .catch(function (err) {
                var empty = document.getElementById("rt-result-empty");
                var panel = document.getElementById("rt-result-card");
                var updated = document.getElementById("rt-result-updated");
                if (empty) empty.style.display = "block";
                if (panel) panel.style.display = "none";
                if (updated) {
                    updated.textContent = "Error: " + err;
                }
            })
            .then(function () {
                if (btn) {
                    btn.disabled = false;
                }
                loadTopology();
            });
    }

    // ---------- View: Failure & Self-Healing ----------
    function renderFailureView() {
        var nodes = state.topology.nodes || [];
        var links = state.topology.links || [];
        fillSelect("fl-fail-link-select", nodes, links, "links");
        fillSelect("fl-recover-link-select", nodes, links, "links");
        fillSelect("fl-fail-node-select", nodes, links, "nodes");
        fillSelect("fl-recover-node-select", nodes, links, "nodes");
    }

    function failLink() {
        if (!window.SmartPacketApi) {
            return;
        }
        var linkId = document.getElementById("fl-fail-link-select") ? document.getElementById("fl-fail-link-select").value : null;
        var result = document.getElementById("fl-action-result");
        if (!linkId) {
            if (result) {
                result.textContent = "Select a link to fail.";
            }
            return;
        }
        var btn = document.getElementById("fl-fail-link-btn");
        if (btn) {
            btn.disabled = true;
        }
        window.SmartPacketApi.failLink(linkId)
            .then(function (data) {
                if (result) {
                    result.textContent = JSON.stringify(data, null, 2);
                }
            })
            .catch(function (err) {
                if (result) {
                    result.textContent = "Error: " + err;
                }
            })
            .then(function () {
                if (btn) {
                    btn.disabled = false;
                }
                loadTopology();
            });
    }

    function failNode() {
        if (!window.SmartPacketApi) {
            return;
        }
        var nodeId = document.getElementById("fl-fail-node-select") ? document.getElementById("fl-fail-node-select").value : null;
        var result = document.getElementById("fl-action-result");
        if (!nodeId) {
            if (result) {
                result.textContent = "Select a node to fail.";
            }
            return;
        }
        var btn = document.getElementById("fl-fail-node-btn");
        if (btn) {
            btn.disabled = true;
        }
        window.SmartPacketApi.failNode(nodeId)
            .then(function (data) {
                if (result) {
                    result.textContent = JSON.stringify(data, null, 2);
                }
            })
            .catch(function (err) {
                if (result) {
                    result.textContent = "Error: " + err;
                }
            })
            .then(function () {
                if (btn) {
                    btn.disabled = false;
                }
                loadTopology();
            });
    }

    function recoverLink() {
        if (!window.SmartPacketApi) {
            return;
        }
        var linkId = document.getElementById("fl-recover-link-select") ? document.getElementById("fl-recover-link-select").value : null;
        var result = document.getElementById("fl-action-result");
        if (!linkId) {
            if (result) {
                result.textContent = "Select a link to recover.";
            }
            return;
        }
        var btn = document.getElementById("fl-recover-link-btn");
        if (btn) {
            btn.disabled = true;
        }
        window.SmartPacketApi.recoverLink(linkId)
            .then(function (data) {
                if (result) {
                    result.textContent = JSON.stringify(data, null, 2);
                }
            })
            .catch(function (err) {
                if (result) {
                    result.textContent = "Error: " + err;
                }
            })
            .then(function () {
                if (btn) {
                    btn.disabled = false;
                }
                loadTopology();
            });
    }

    function recoverNode() {
        if (!window.SmartPacketApi) {
            return;
        }
        var nodeId = document.getElementById("fl-recover-node-select") ? document.getElementById("fl-recover-node-select").value : null;
        var result = document.getElementById("fl-action-result");
        if (!nodeId) {
            if (result) {
                result.textContent = "Select a node to recover.";
            }
            return;
        }
        var btn = document.getElementById("fl-recover-node-btn");
        if (btn) {
            btn.disabled = true;
        }
        window.SmartPacketApi.recoverNode(nodeId)
            .then(function (data) {
                if (result) {
                    result.textContent = JSON.stringify(data, null, 2);
                }
            })
            .catch(function (err) {
                if (result) {
                    result.textContent = "Error: " + err;
                }
            })
            .then(function () {
                if (btn) {
                    btn.disabled = false;
                }
                loadTopology();
            });
    }

    function viewFailureStatus() {
        if (!window.SmartPacketApi) {
            return;
        }
        var result = document.getElementById("fl-status-result");
        window.SmartPacketApi.getFailureStatus()
            .then(function (data) {
                if (result) {
                    result.textContent = JSON.stringify(data, null, 2);
                }
                if (data) {
                    state.topology.nodes = data.nodes || [];
                    state.topology.links = data.links || [];
                    renderTopologyLists();
                }
            })
            .catch(function (err) {
                if (result) {
                    result.textContent = "Error: " + err;
                }
            });
    }

    function loadFailureHistory() {
        if (!window.SmartPacketApi) {
            return;
        }
        var result = document.getElementById("fl-status-result");
        window.SmartPacketApi.getHistory()
            .then(function (data) {
                if (result) {
                    result.textContent = JSON.stringify(data, null, 2);
                }
            })
            .catch(function (err) {
                if (result) {
                    result.textContent = "Error: " + err;
                }
            });
    }

    // ---------- Monitoring helpers ----------
    function setText(el, value) {
        if (el) {
            el.textContent = value == null ? "\u2014" : value;
        }
    }

    function setEl(id, value) {
        setText(document.getElementById(id), value);
    }

    function num(n) {
        return n == null ? 0 : Number(n);
    }

    function pct(n) {
        return num(n).toFixed(1) + "%";
    }

    function ms(n) {
        return num(n).toFixed(0) + " ms";
    }

    // ---------- View: Performance Monitoring ----------
    var monEls = {
        status: document.getElementById("mon-status"),
        refresh: document.getElementById("mon-refresh"),
        snapshot: document.getElementById("mon-snapshot"),
        mTotalPackets: document.getElementById("m-total-packets"),
        mDelivered: document.getElementById("m-delivered"),
        mFailed: document.getElementById("m-failed"),
        mInTransit: document.getElementById("m-in-transit"),
        mPdr: document.getElementById("m-pdr"),
        mPlr: document.getElementById("m-plr"),
        mAvgTime: document.getElementById("m-avg-time"),
        mActiveNodes: document.getElementById("m-active-nodes"),
        mFailedNodes: document.getElementById("m-failed-nodes"),
        mActiveLinks: document.getElementById("m-active-links"),
        mFailedLinks: document.getElementById("m-failed-links"),
        packetsTableBody: document.getElementById("packets-table-body"),
        historyTableBody: document.getElementById("history-table-body")
    };

    function renderOverview(data) {
        if (!data) {
            setText(monEls.status, "No monitoring data available.");
            return;
        }
        var hasData = num(data.totalPacketsCreated) > 0;
        setText(monEls.status, "Last refreshed: " + new Date().toLocaleTimeString());
        setText(monEls.mTotalPackets, num(data.totalPacketsCreated));
        setText(monEls.mDelivered, num(data.successfullyDeliveredPackets));
        setText(monEls.mFailed, num(data.failedPackets));
        setText(monEls.mInTransit, num(data.packetsInTransit));
        setText(monEls.mPdr, hasData ? pct(data.packetDeliveryRatio) : "No data yet");
        setText(monEls.mPlr, hasData ? pct(data.packetFailureRatio) : "No data yet");
        setText(monEls.mAvgTime, hasData ? ms(data.averagePacketDeliveryTime) : "No data yet");
        setText(monEls.mActiveNodes, num(data.totalActiveNodes));
        setText(monEls.mFailedNodes, num(data.totalFailedNodes));
        setText(monEls.mActiveLinks, num(data.totalActiveLinks));
        setText(monEls.mFailedLinks, num(data.totalFailedLinks));
        renderCharts(data, state.links, hasData);
    }

    function renderPackets(packets) {
        var tbody = monEls.packetsTableBody;
        if (!tbody) {
            return;
        }
        tbody.innerHTML = "";
        if (!packets || packets.length === 0) {
            var tr = document.createElement("tr");
            var td = document.createElement("td");
            td.colSpan = 7;
            td.className = "empty-state";
            td.textContent = "No packet data available yet. Simulate a delivery to populate this table.";
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }
        packets.forEach(function (p) {
            var status = p.status == null ? "UNKNOWN" : String(p.status);
            var cls = status.toLowerCase();
            var duration = p.deliveryDurationMillis == null ? "\u2014" : p.deliveryDurationMillis + " ms";
            var route = (p.route && p.route.length) ? p.route.join(" \u2192 ") : "\u2014";
            var tr = document.createElement("tr");
            tr.innerHTML =
                "<td class='status-" + cls + "'>" + (p.packetId || "\u2014") + "</td>" +
                "<td>" + (p.sourceNodeId || "\u2014") + "</td>" +
                "<td>" + (p.destinationNodeId || "\u2014") + "</td>" +
                "<td class='status-" + cls + "'>" + status + "</td>" +
                "<td>" + duration + "</td>" +
                "<td>" + route + "</td>" +
                "<td>" + (p.failureReason || "\u2014") + "</td>";
            tbody.appendChild(tr);
        });
    }

    function renderHistory(history) {
        var tbody = monEls.historyTableBody;
        if (!tbody) {
            return;
        }
        tbody.innerHTML = "";
        if (!history || history.length === 0) {
            var tr = document.createElement("tr");
            var td = document.createElement("td");
            td.colSpan = 10;
            td.className = "empty-state";
            td.textContent = "No snapshots taken yet. Use Take Snapshot to record performance history.";
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }
        history.slice().reverse().forEach(function (s) {
            var tr = document.createElement("tr");
            tr.innerHTML =
                "<td>" + (s.timestamp ? new Date(s.timestamp).toLocaleString() : "\u2014") + "</td>" +
                "<td>" + num(s.totalPackets) + "</td>" +
                "<td>" + num(s.deliveredPackets) + "</td>" +
                "<td>" + num(s.failedPackets) + "</td>" +
                "<td>" + pct(s.packetDeliveryRatio) + "</td>" +
                "<td>" + ms(s.averageDeliveryTime) + "</td>" +
                "<td>" + num(s.activeNodes) + "</td>" +
                "<td>" + num(s.failedNodes) + "</td>" +
                "<td>" + num(s.activeLinks) + "</td>" +
                "<td>" + num(s.failedLinks) + "</td>";
            tbody.appendChild(tr);
        });
    }

    function renderCharts(overview, links, hasData) {
        if (typeof Chart === "undefined") {
            return;
        }
        var delivered = overview ? num(overview.successfullyDeliveredPackets) : 0;
        var failed = overview ? num(overview.failedPackets) : 0;
        var pdrVal = overview ? num(overview.packetDeliveryRatio) : 0;
        var activeNodes = overview ? num(overview.totalActiveNodes) : 0;
        var failedNodes = overview ? num(overview.totalFailedNodes) : 0;
        var activeLinks = overview ? num(overview.totalActiveLinks) : 0;
        var failedLinks = overview ? num(overview.totalFailedLinks) : 0;
        var avgTime = overview ? num(overview.averagePacketDeliveryTime) : 0;

        var baseOptions = {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: "bottom", labels: { color: "#e5e7eb" } },
                title: { display: false }
            },
            scales: {
                x: { ticks: { color: "#94a3b8" }, grid: { color: "rgba(148, 163, 184, 0.16)" } },
                y: { ticks: { color: "#94a3b8" }, grid: { color: "rgba(148, 163, 184, 0.16)" } }
            }
        };

        function showChart(canvasId) {
            var canvas = document.getElementById(canvasId);
            var empty = document.getElementById(canvasId + "-empty");
            if (canvas) {
                canvas.style.display = "block";
            }
            if (empty) {
                empty.style.display = "none";
            }
        }

        function showEmpty(canvasId, message) {
            var canvas = document.getElementById(canvasId);
            var empty = document.getElementById(canvasId + "-empty");
            if (state.charts[canvasId]) {
                try {
                    state.charts[canvasId].destroy();
                } catch (e) {
                    // ignore
                }
                state.charts[canvasId] = null;
            }
            if (canvas) {
                canvas.style.display = "none";
            }
            if (empty) {
                empty.textContent = message;
                empty.style.display = "block";
            }
        }

        var hasDelivery = hasData && delivered + failed > 0;
        if (hasDelivery) {
            showChart("chart-delivery");
            getChart("chart-delivery", "doughnut", {
                labels: ["Delivered", "Failed"],
                datasets: [{
                    data: [delivered, failed],
                    backgroundColor: ["#22c55e", "#ef4444"],
                    borderColor: "rgba(15, 23, 42, 0.8)",
                    borderWidth: 2
                }]
            }, Object.assign({}, baseOptions, {
                responsive: true,
                maintainAspectRatio: false,
                cutout: "65%",
                plugins: {
                    legend: { position: "bottom", labels: { color: "#e5e7eb" } },
                    title: { display: true, text: "Delivered vs Failed" }
                }
            }));
        } else {
            showEmpty("chart-delivery", "No delivery data yet.");
        }

        var hasPdr = hasData && delivered + failed > 0;
        if (hasPdr) {
            showChart("chart-pdr");
            getChart("chart-pdr", "doughnut", {
                labels: ["Delivered", "Lost"],
                datasets: [{
                    data: [pdrVal, Math.max(0, 100 - pdrVal)],
                    backgroundColor: ["#38bdf8", "#ef4444"],
                    borderColor: "rgba(15, 23, 42, 0.8)",
                    borderWidth: 2
                }]
            }, Object.assign({}, baseOptions, {
                responsive: true,
                maintainAspectRatio: false,
                cutout: "65%",
                plugins: {
                    legend: { position: "bottom", labels: { color: "#e5e7eb" } },
                    title: { display: true, text: "Packet Delivery Ratio" }
                }
            }));
        } else {
            showEmpty("chart-pdr", "No packet delivery data yet.");
        }

        var hasAvgTime = hasData && delivered > 0;
        if (hasAvgTime) {
            showChart("chart-avgtime");
            getChart("chart-avgtime", "bar", {
                labels: ["Avg Delivery Time"],
                datasets: [{
                    label: "ms",
                    data: [avgTime],
                    backgroundColor: "rgba(56, 189, 248, 0.8)",
                    borderRadius: 6
                }]
            }, Object.assign({}, baseOptions, {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { ticks: { color: "#94a3b8" }, grid: { display: false } },
                    y: { beginAtZero: true, ticks: { color: "#94a3b8" }, grid: { color: "rgba(148, 163, 184, 0.16)" }, title: { display: true, text: "ms", color: "#94a3b8" } }
                },
                plugins: { legend: { display: false }, title: { display: true, text: "Average Delivery Time" } }
            }));
        } else {
            showEmpty("chart-avgtime", "No delivery-time data yet.");
        }

        var hasNet = hasData && (activeNodes + failedNodes + activeLinks + failedLinks > 0);
        if (hasNet) {
            showChart("chart-network");
            getChart("chart-network", "doughnut", {
                labels: ["Active Nodes", "Failed Nodes", "Active Links", "Failed Links"],
                datasets: [{
                    data: [activeNodes, failedNodes, activeLinks, failedLinks],
                    backgroundColor: ["#22c55e", "#ef4444", "#38bdf8", "#94a3b8"],
                    borderColor: "rgba(15, 23, 42, 0.8)",
                    borderWidth: 2
                }]
            }, Object.assign({}, baseOptions, {
                responsive: true,
                maintainAspectRatio: false,
                cutout: "65%",
                plugins: {
                    legend: { position: "bottom", labels: { color: "#e5e7eb" } },
                    title: { display: true, text: "Network Status Distribution" }
                }
            }));
        } else {
            showEmpty("chart-network", "No network data yet.");
        }

        var canvasLinks = document.getElementById("chart-links");
        if (links && links.length) {
            showChart("chart-links");
            getChart("chart-links", "bar", {
                labels: links.map(function (l) { return l.linkId || ""; }),
                datasets: [{
                    label: "Simulated utilization %",
                    data: links.map(function (l) { return num(l.utilizationPercentage); }),
                    backgroundColor: links.map(function (l) {
                        var note = l.statusNote || "";
                        return l.congested || note.indexOf("FAILED") >= 0 ? "#ef4444" : "rgba(56, 189, 248, 0.8)";
                    }),
                    borderRadius: 6
                }]
            }, Object.assign({}, baseOptions, {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { ticks: { color: "#94a3b8" }, grid: { display: false } },
                    y: { beginAtZero: true, max: 100, ticks: { color: "#94a3b8" }, grid: { color: "rgba(148, 163, 184, 0.16)" }, title: { display: true, text: "%", color: "#94a3b8" } }
                },
                plugins: { legend: { display: false }, title: { display: true, text: "Link Utilization (Simulated)" } }
            }));
        } else {
            showEmpty("chart-links", "No link utilization data available.");
        }
    }

    function getChart(canvasId, type, data, options) {
        if (typeof Chart === "undefined") {
            return null;
        }
        var canvas = document.getElementById(canvasId);
        if (!canvas) {
            return null;
        }
        if (state.charts[canvasId]) {
            try {
                state.charts[canvasId].destroy();
            } catch (e) {
                // ignore
            }
        }
        var ctx = canvas.getContext("2d");
        state.charts[canvasId] = new Chart(ctx, { type: type, data: data, options: options });
        return state.charts[canvasId];
    }

    function loadMonitoring() {
        if (!window.SmartPacketApi) {
            return;
        }
        if (monEls.refresh) {
            monEls.refresh.disabled = true;
        }
        if (monEls.snapshot) {
            monEls.snapshot.disabled = true;
        }
        setText(monEls.status, "Loading monitoring data\u2026");

        var overviewP = window.SmartPacketApi.getMonitoringOverview().catch(function () { return null; });
        var packetsP = window.SmartPacketApi.getMonitoringPackets().catch(function () { return []; });
        var linksP = window.SmartPacketApi.getMonitoringLinks().catch(function () { return []; });
        var historyP = window.SmartPacketApi.getMonitoringHistory().catch(function () { return []; });

        Promise.all([overviewP, packetsP, linksP, historyP])
            .then(function (r) {
                state.overview = r[0];
                state.packets = r[1] || [];
                state.links = r[2] || [];
                state.history = r[3] || [];
                renderOverview(r[0]);
                renderPackets(r[1]);
                renderHistory(r[3]);
                var overview = r[0];
                if (monEls.status && (!overview || num(overview.totalPacketsCreated) === 0)) {
                    setText(monEls.status, "No packet data yet. Deliver a packet to see metrics.");
                }
            })
            .catch(function (err) {
                setText(monEls.status, "Error loading monitoring: " + err);
                console.error(err);
            })
            .then(function () {
                if (monEls.refresh) {
                    monEls.refresh.disabled = false;
                }
                if (monEls.snapshot) {
                    monEls.snapshot.disabled = false;
                }
            });
    }

    function renderMonitoringView() {
        loadMonitoring();
        renderAvailability();
    }

    function takeSnapshot() {
        if (!window.SmartPacketApi) {
            return;
        }
        if (monEls.snapshot) {
            monEls.snapshot.disabled = true;
        }
        window.SmartPacketApi.takeMonitoringSnapshot()
            .then(function () {
                return loadMonitoring();
            })
            .catch(function (err) {
                setText(monEls.status, "Snapshot error: " + err);
            })
            .then(function () {
                if (monEls.snapshot) {
                    monEls.snapshot.disabled = false;
                }
            });
    }

    // ---------- Initialization ----------
    document.addEventListener("DOMContentLoaded", function () {
        bindCardClicks();
        bindNavToggle();

        document.getElementById("ov-load-sample") && document.getElementById("ov-load-sample").addEventListener("click", loadSample);
        document.getElementById("ov-refresh-topology") && document.getElementById("ov-refresh-topology").addEventListener("click", loadTopology);
        document.getElementById("dl-deliver-packet") && document.getElementById("dl-deliver-packet").addEventListener("click", deliverPacket);
        document.getElementById("rt-calculate-route") && document.getElementById("rt-calculate-route").addEventListener("click", calculateRoute);
        document.getElementById("rt-recalculate-route") && document.getElementById("rt-recalculate-route").addEventListener("click", calculateRoute);
        document.getElementById("fl-fail-link-btn") && document.getElementById("fl-fail-link-btn").addEventListener("click", failLink);
        document.getElementById("fl-fail-node-btn") && document.getElementById("fl-fail-node-btn").addEventListener("click", failNode);
        document.getElementById("fl-recover-link-btn") && document.getElementById("fl-recover-link-btn").addEventListener("click", recoverLink);
        document.getElementById("fl-recover-node-btn") && document.getElementById("fl-recover-node-btn").addEventListener("click", recoverNode);
        document.getElementById("fl-view-failure-status") && document.getElementById("fl-view-failure-status").addEventListener("click", viewFailureStatus);
        document.getElementById("fl-load-history") && document.getElementById("fl-load-history").addEventListener("click", loadFailureHistory);
        document.getElementById("mon-refresh") && document.getElementById("mon-refresh").addEventListener("click", loadMonitoring);
        document.getElementById("mon-snapshot") && document.getElementById("mon-snapshot").addEventListener("click", takeSnapshot);

        checkHealth();
        loadSample();
        window.addEventListener("hashchange", route);
        route();
    });
})(window, document);

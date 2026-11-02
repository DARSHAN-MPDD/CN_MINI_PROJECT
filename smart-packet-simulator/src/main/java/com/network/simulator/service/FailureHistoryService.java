package com.network.simulator.service;

import com.network.simulator.model.FailureEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class FailureHistoryService {
    private final ConcurrentLinkedDeque<FailureEvent> history = new ConcurrentLinkedDeque<>();

    public void addEvent(FailureEvent event) {
        if (event != null) {
            history.addFirst(event);
        }
    }

    public List<FailureEvent> getHistory() {
        return new ArrayList<>(history);
    }

    public void clear() {
        history.clear();
    }
}

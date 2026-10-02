package com.playville.crm.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DisclaimerStatusStream {
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long draftId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(draftId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(draftId, emitter));
        emitter.onTimeout(() -> remove(draftId, emitter));
        emitter.onError(ignored -> remove(draftId, emitter));
        return emitter;
    }

    public void signed(Long draftId, Long acceptanceId) {
        var listeners = emitters.remove(draftId);
        if (listeners == null) return;
        for (SseEmitter emitter : listeners) {
            try {
                emitter.send(SseEmitter.event().name("disclaimer-status").data(Map.of(
                        "status", "SIGNED",
                        "acceptanceId", acceptanceId
                )));
                emitter.complete();
            } catch (IOException ignored) {
                emitter.completeWithError(ignored);
            }
        }
    }

    private void remove(Long draftId, SseEmitter emitter) {
        var listeners = emitters.get(draftId);
        if (listeners == null) return;
        listeners.remove(emitter);
        if (listeners.isEmpty()) emitters.remove(draftId, listeners);
    }
}

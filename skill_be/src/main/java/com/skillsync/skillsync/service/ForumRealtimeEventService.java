package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.event.forum.ForumPostChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class ForumRealtimeEventService {
    private static final long EMITTER_TIMEOUT_MS = 0L;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((error) -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException error) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void publishForumPostChangedEvent(ForumPostChangedEvent event) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("forum-post-changed").data(event));
            } catch (IOException error) {
                emitters.remove(emitter);
            }
        }
    }
}
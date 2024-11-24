package com.aleksandr_kobelskiy.week12practice.rest;

import com.aleksandr_kobelskiy.week12practice.entity.EventEntity;
import com.aleksandr_kobelskiy.week12practice.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventRestControllerV1 {

    private final EventService eventService;

    @GetMapping
    public Flux<EventEntity> getAllEvents() {
        return eventService.getAllEvents();
    }

    @PostMapping
    public Mono<ResponseEntity<EventEntity>> createEvent(@RequestParam Long userId, @RequestParam Long fileId) {
        return eventService.logEvent(userId, fileId)
                .map(ResponseEntity::ok);
    }
}

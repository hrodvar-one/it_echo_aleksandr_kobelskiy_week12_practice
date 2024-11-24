package com.aleksandr_kobelskiy.week12practice.service;

import com.aleksandr_kobelskiy.week12practice.entity.EventEntity;
import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public Mono<EventEntity> logEvent(Long userId, Long fileId) {
        EventEntity event = new EventEntity();
        return eventRepository.save(
                event.toBuilder()
                        .userId(userId)
                        .fileId(fileId)
                        .status(Status.ACTIVE)
                        .build());
    }

    public Flux<EventEntity> getAllEvents() {
        return eventRepository.findAll();
    }
}

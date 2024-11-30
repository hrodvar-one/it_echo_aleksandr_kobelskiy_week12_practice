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
    private final UserService userService;

    public Mono<EventEntity> logEvent(Long fileId, Status status) {
        return userService.getCurrentUser() // Получаем текущего пользователя
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No authenticated user found")))
                .flatMap(currentUser -> {
                    EventEntity event = EventEntity.builder()
                            .userId(currentUser.getId()) // Устанавливаем ID текущего пользователя
                            .fileId(fileId)
                            .status(status)
                            .build();
                    return eventRepository.save(event);
                });
    }

    public Mono<Void> markEventAsDeleted(Long fileId) {
        return eventRepository.findByFileId(fileId)
                .flatMap(event -> {
                    event.setStatus(Status.DELETED);
                    return eventRepository.save(event);
                })
                .then();
    }

    public Flux<EventEntity> getAllEvents() {
        return eventRepository.findAll();
    }
}

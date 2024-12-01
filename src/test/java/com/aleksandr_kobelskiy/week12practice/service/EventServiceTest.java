package com.aleksandr_kobelskiy.week12practice.service;

import com.aleksandr_kobelskiy.week12practice.entity.EventEntity;
import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.entity.UserEntity;
import com.aleksandr_kobelskiy.week12practice.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private EventService eventServiceUnderTest;

    @Test
    @DisplayName("Test save event functionality")
    public void givenEventToSave_whenSaveEvent_thenRepositoryIsCalled() {
        //given
        Long fileId = 1L;
        UserEntity currentUser = UserEntity.builder()
                .id(1L)
                .username("testUser")
                .build();

        EventEntity eventToSave = EventEntity.builder()
                .userId(currentUser.getId())
                .fileId(fileId)
                .status(Status.ACTIVE)
                .build();

        BDDMockito.given(userService.getCurrentUser()).willReturn(Mono.just(currentUser));
        BDDMockito.given(eventRepository.save(any(EventEntity.class))).willReturn(Mono.just(eventToSave));

        // when
        Mono<EventEntity> result = eventServiceUnderTest.logEvent(fileId, Status.ACTIVE);

        // then
        StepVerifier.create(result)
                .expectNext(eventToSave)
                .verifyComplete();

        BDDMockito.then(eventRepository).should(times(1)).save(any(EventEntity.class));
    }

    @Test
    @DisplayName("Test save event functionality when no user is authenticated")
    public void givenNoAuthenticatedUser_whenSaveEvent_thenErrorIsThrown() {
        // given
        Long fileId = 1L;

        BDDMockito.given(userService.getCurrentUser()).willReturn(Mono.empty());

        // when
        Mono<EventEntity> result = eventServiceUnderTest.logEvent(fileId, Status.ACTIVE);

        // then
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();

        BDDMockito.then(userService).should(times(1)).getCurrentUser();
        BDDMockito.then(eventRepository).should(never()).save(any(EventEntity.class));
    }

    @Test
    @DisplayName("Test mark event as deleted successfully")
    public void givenExistingEvent_whenMarkEventAsDeleted_thenStatusIsUpdated() {
        // given
        Long fileId = 1L;
        EventEntity existingEvent = EventEntity.builder()
                .fileId(fileId)
                .status(Status.ACTIVE)
                .build();

        BDDMockito.given(eventRepository.findByFileId(any(Long.class))).willReturn(Flux.just(existingEvent));
        BDDMockito.given(eventRepository.save(any(EventEntity.class))).willReturn(Mono.just(existingEvent));

        // when
        Mono<Void> result = eventServiceUnderTest.markEventAsDeleted(fileId);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        // Проверяем вызовы методов
        BDDMockito.then(eventRepository).should(times(1)).findByFileId(fileId);
        BDDMockito.then(eventRepository).should(times(1)).save(existingEvent);

        // Проверяем статус
        assertEquals(Status.DELETED, existingEvent.getStatus());
    }

    @Test
    @DisplayName("Test mark event as deleted when no event is found")
    public void givenNoEvent_whenMarkEventAsDeleted_thenNoAction() {
        // given
        Long fileId = 1L;

        BDDMockito.given(eventRepository.findByFileId(any(Long.class))).willReturn(Flux.empty());

        // when
        Mono<Void> result = eventServiceUnderTest.markEventAsDeleted(fileId);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        // Проверяем вызовы методов
        BDDMockito.then(eventRepository).should(times(1)).findByFileId(fileId);
        BDDMockito.then(eventRepository).should(never()).save(any(EventEntity.class));
    }

    @Test
    @DisplayName("Test get all events successfully")
    public void givenEventsExist_whenGetAllEvents_thenReturnAllEvents() {
        // given
        EventEntity event1 = EventEntity.builder()
                .fileId(1L)
                .status(Status.ACTIVE)
                .build();

        EventEntity event2 = EventEntity.builder()
                .fileId(2L)
                .status(Status.DELETED)
                .build();

        BDDMockito.given(eventRepository.findAll()).willReturn(Flux.just(event1, event2));

        // when
        Flux<EventEntity> result = eventServiceUnderTest.getAllEvents();

        // then
        StepVerifier.create(result)
                .expectNext(event1)
                .expectNext(event2)
                .verifyComplete();

        // Проверяем вызов метода findAll
        BDDMockito.then(eventRepository).should(times(1)).findAll();
    }

    @Test
    @DisplayName("Test get all events when no events exist")
    public void givenNoEventsExist_whenGetAllEvents_thenReturnEmptyFlux() {
        // given
        BDDMockito.given(eventRepository.findAll()).willReturn(Flux.empty());

        // when
        Flux<EventEntity> result = eventServiceUnderTest.getAllEvents();

        // then
        StepVerifier.create(result)
                .verifyComplete();

        // Проверяем вызов метода findAll
        BDDMockito.then(eventRepository).should(times(1)).findAll();
    }
}
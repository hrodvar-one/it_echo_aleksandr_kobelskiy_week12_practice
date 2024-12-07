package com.aleksandr_kobelskiy.week12practice.repository;

import com.aleksandr_kobelskiy.week12practice.entity.EventEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface EventRepository extends R2dbcRepository<EventEntity, Long> {
    Flux<EventEntity> findByFileId(Long fileId);
}

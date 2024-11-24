package com.aleksandr_kobelskiy.week12practice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table("events")
public class EventEntity {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("file_id")
    private Long fileId;

    @Column("status")
    private Status status = Status.ACTIVE;
}

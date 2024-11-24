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
@Table("files")
public class FileEntity {

    @Id
    private Long id;

    @Column("file_name")
    private String fileName;

    @Column("location")
    private String location;

    @Column("status")
    private Status status = Status.ACTIVE;
}

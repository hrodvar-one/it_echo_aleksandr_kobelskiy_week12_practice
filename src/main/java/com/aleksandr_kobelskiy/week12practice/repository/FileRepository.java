package com.aleksandr_kobelskiy.week12practice.repository;

import com.aleksandr_kobelskiy.week12practice.entity.FileEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface FileRepository extends R2dbcRepository<FileEntity, Long> {

}

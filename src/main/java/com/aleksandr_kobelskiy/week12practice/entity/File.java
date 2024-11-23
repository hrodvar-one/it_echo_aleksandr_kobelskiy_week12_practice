package com.aleksandr_kobelskiy.week12practice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "files")
public class File {

    @Column(name = "name")
    private String name;

    @Column(name = "location")
    private String location;
}

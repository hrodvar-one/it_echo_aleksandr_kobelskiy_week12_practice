package com.aleksandr_kobelskiy.week12practice.repository;

import com.aleksandr_kobelskiy.week12practice.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}

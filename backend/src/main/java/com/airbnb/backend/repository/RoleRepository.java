package com.airbnb.backend.repository;

import com.airbnb.backend.entity.Role;
import com.airbnb.backend.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {


    Optional<Role> findByName(RoleName name);
}
package com.nethub.repository;

import com.nethub.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    List<Connection> findByProjectId(Long projectId);
}

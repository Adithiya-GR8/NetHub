package com.nethub.repository;

import com.nethub.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByProjectId(Long projectId);
    List<Route> findByDeviceId(Long deviceId);
    void deleteByDeviceId(Long deviceId);
}

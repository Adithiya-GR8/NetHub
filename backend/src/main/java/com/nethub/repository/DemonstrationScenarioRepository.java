package com.nethub.repository;

import com.nethub.entity.DemonstrationScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemonstrationScenarioRepository extends JpaRepository<DemonstrationScenario, Long> {
    Optional<DemonstrationScenario> findByCode(String code);
    List<DemonstrationScenario> findByCategoryOrderByCodeAsc(String category);
    List<DemonstrationScenario> findAllByOrderByCategoryAscCodeAsc();
    boolean existsByCode(String code);
}

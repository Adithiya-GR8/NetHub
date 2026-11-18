package com.nethub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "demonstration_scenarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemonstrationScenario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g. "V1", "V2", ..., "I1", "I2", ...

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category; // "VALID", "INVALID"

    @Column(length = 2000)
    private String description;

    private String difficulty; // "BEGINNER", "INTERMEDIATE", "ADVANCED"

    private Integer deviceCount;

    @Column(length = 2000)
    private String concepts; // Comma-separated list or summary of concepts

    @Column(length = 2000)
    private String expectedResult;

    @Column(length = 2000)
    private String expectedErrors;

    @Column(name = "template_project_id")
    private Long templateProjectId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

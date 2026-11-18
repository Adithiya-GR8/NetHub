package com.nethub.entity;

import com.nethub.entity.enums.ConnectionStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "connections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Connection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    
    @Column(name = "source_interface_id", nullable = false)
    private Long sourceInterfaceId;
    
    @Column(name = "destination_interface_id", nullable = false)
    private Long destinationInterfaceId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionStatus status = ConnectionStatus.ACTIVE;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

package com.nethub.entity;

import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    
    @Column(nullable = false)
    private String name;

    @Column(name = "hostname")
    private String hostname;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.ONLINE;
    
    @Column(name = "position_x", nullable = false)
    @Builder.Default
    private Double positionX = 0.0;
    
    @Column(name = "position_y", nullable = false)
    @Builder.Default
    private Double positionY = 0.0;

    @Column(name = "default_gateway")
    private String defaultGateway;

    @Column(name = "primary_dns")
    private String primaryDns;

    @Column(name = "secondary_dns")
    private String secondaryDns;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Backward-compatible constructor for existing tests
    public Device(Long id, Long projectId, String name, DeviceType type, DeviceStatus status, Double positionX, Double positionY, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.hostname = name;
        this.type = type;
        this.status = status != null ? status : DeviceStatus.ONLINE;
        this.positionX = positionX != null ? positionX : 0.0;
        this.positionY = positionY != null ? positionY : 0.0;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    @PrePersist
    protected void onCreate() {
        if (hostname == null || hostname.isEmpty()) {
            hostname = name;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

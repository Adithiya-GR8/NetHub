package com.nethub.entity;

import com.nethub.entity.enums.RouteType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "destination_network", nullable = false)
    private String destinationNetwork;

    @Column(name = "subnet_mask", nullable = false)
    private String subnetMask;

    @Column(name = "next_hop")
    private String nextHop; // Null for CONNECTED routes

    @Column(name = "outgoing_interface")
    private String outgoingInterface; // Interface name, e.g., eth0

    @Column(nullable = false)
    @Builder.Default
    private Integer metric = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false)
    @Builder.Default
    private RouteType routeType = RouteType.STATIC;
}

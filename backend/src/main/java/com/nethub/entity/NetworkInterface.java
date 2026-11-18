package com.nethub.entity;

import com.nethub.entity.enums.InterfaceStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "network_interfaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkInterface {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id", nullable = false)
    private Long deviceId;
    
    @Column(nullable = false)
    private String name; // e.g. eth0, port1
    
    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "subnet_mask")
    private String subnetMask;
    
    private String gateway;
    
    @Column(name = "dns_server")
    private String dnsServer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterfaceStatus status = InterfaceStatus.UP;
}

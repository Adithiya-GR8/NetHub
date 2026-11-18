package com.nethub.model.state;

import com.nethub.entity.enums.InterfaceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceOperationalDto {
    private Long id;
    private Long deviceId;
    private String name;
    private String macAddress;
    private String ipAddress;
    private String subnetMask;
    private String gateway;
    private String dnsServer;

    private InterfaceStatus adminStatus;
    private InterfaceStatus operationalStatus;

    private Integer mtu;
    private String speed;
    private String duplex;
    private Integer vlanId;
    private String portMode;
    private String allowedVlans;

    private Long connectedDeviceId;
    private String connectedDeviceName;
    private String connectedInterfaceName;

    private InterfaceStats stats;
}


package com.nethub.model.state;

import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import com.nethub.model.ArpEntry;
import com.nethub.model.MacTableEntry;
import com.nethub.model.RouteEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInspectionDto {
    private Long deviceId;
    private String name;
    private String hostname;
    private DeviceType type;
    private DeviceStatus status;

    private String defaultGateway;
    private String primaryDns;
    private String secondaryDns;

    @Builder.Default
    private List<InterfaceOperationalDto> interfaces = new ArrayList<>();

    @Builder.Default
    private List<ArpEntry> arpTable = new ArrayList<>();

    @Builder.Default
    private List<MacTableEntry> macTable = new ArrayList<>();

    @Builder.Default
    private List<RouteEntry> routingTable = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> vlanInfo = new HashMap<>();

    @Builder.Default
    private Map<String, Object> stpInfo = new HashMap<>();

    @Builder.Default
    private List<SocketDto> activeSockets = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> services = new HashMap<>();
}


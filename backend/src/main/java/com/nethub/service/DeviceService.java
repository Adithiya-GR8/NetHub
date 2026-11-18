package com.nethub.service;

import com.nethub.entity.Device;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.ProjectRepository;
import com.nethub.exception.ResourceNotFoundException;
import com.nethub.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final ProjectRepository projectRepository;

    public List<Device> getDevicesByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        return deviceRepository.findByProjectId(projectId);
    }

    public Device getDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + id));
    }

    public Device createDevice(Long projectId, Device device) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        device.setProjectId(projectId);
        
        // Sensible non-IP defaults
        if (device.getHostname() == null || device.getHostname().trim().isEmpty()) {
            device.setHostname((device.getName() != null ? device.getName() : "device").toLowerCase() + ".local");
        }
        if (device.getStatus() == null) {
            device.setStatus(com.nethub.entity.enums.DeviceStatus.ONLINE);
        }
        if (device.getRoutingProtocol() == null || device.getRoutingProtocol().trim().isEmpty()) {
            device.setRoutingProtocol("STATIC");
        }
        if (device.getType() == com.nethub.entity.enums.DeviceType.ACCESS_POINT) {
            if (device.getSsid() == null || device.getSsid().trim().isEmpty()) {
                device.setSsid("NetHub-WiFi");
            }
            if (device.getWirelessChannel() == null) {
                device.setWirelessChannel(6);
            }
            if (device.getWirelessSecurity() == null || device.getWirelessSecurity().trim().isEmpty()) {
                device.setWirelessSecurity("WPA2-PSK (AES)");
            }
            if (device.getWirelessKey() == null || device.getWirelessKey().trim().isEmpty()) {
                device.setWirelessKey("nethub123");
            }
        }
        if (device.getType() == com.nethub.entity.enums.DeviceType.ROUTER || device.getType() == com.nethub.entity.enums.DeviceType.GATEWAY) {
            if (device.getAsNumber() == null) {
                device.setAsNumber(65001);
            }
        }

        return deviceRepository.save(device);
    }

    public Device updateDevice(Long id, Device updatedDevice) {
        Device existing = getDevice(id);
        existing.setName(updatedDevice.getName());
        if (updatedDevice.getHostname() != null && !updatedDevice.getHostname().isEmpty()) {
            existing.setHostname(updatedDevice.getHostname());
        }
        existing.setStatus(updatedDevice.getStatus());
        existing.setPositionX(updatedDevice.getPositionX());
        existing.setPositionY(updatedDevice.getPositionY());
        if (updatedDevice.getDefaultGateway() != null) {
            existing.setDefaultGateway(updatedDevice.getDefaultGateway());
        }
        if (updatedDevice.getPrimaryDns() != null) {
            existing.setPrimaryDns(updatedDevice.getPrimaryDns());
        }
        if (updatedDevice.getSecondaryDns() != null) {
            existing.setSecondaryDns(updatedDevice.getSecondaryDns());
        }
        if (updatedDevice.getSsid() != null) {
            existing.setSsid(updatedDevice.getSsid());
        }
        if (updatedDevice.getWirelessChannel() != null) {
            existing.setWirelessChannel(updatedDevice.getWirelessChannel());
        }
        if (updatedDevice.getWirelessSecurity() != null) {
            existing.setWirelessSecurity(updatedDevice.getWirelessSecurity());
        }
        if (updatedDevice.getWirelessKey() != null) {
            existing.setWirelessKey(updatedDevice.getWirelessKey());
        }
        if (updatedDevice.getRoutingProtocol() != null) {
            existing.setRoutingProtocol(updatedDevice.getRoutingProtocol());
        }
        if (updatedDevice.getAsNumber() != null) {
            existing.setAsNumber(updatedDevice.getAsNumber());
        }
        return deviceRepository.save(existing);
    }

    public void deleteDevice(Long id) {
        deviceRepository.deleteById(id);
    }
}

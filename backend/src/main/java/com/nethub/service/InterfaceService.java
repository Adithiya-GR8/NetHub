package com.nethub.service;

import com.nethub.entity.NetworkInterface;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.repository.NetworkInterfaceRepository;
import com.nethub.repository.DeviceRepository;
import com.nethub.exception.ResourceNotFoundException;
import com.nethub.exception.ValidationException;
import com.nethub.utils.MACAddressGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class InterfaceService {
    private final NetworkInterfaceRepository interfaceRepository;
    private final DeviceRepository deviceRepository;

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    public List<NetworkInterface> getInterfacesByDeviceId(Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("Device not found with id: " + deviceId);
        }
        return interfaceRepository.findByDeviceId(deviceId);
    }

    public NetworkInterface getInterface(Long id) {
        return interfaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interface not found with id: " + id));
    }

    public NetworkInterface createInterface(Long deviceId, NetworkInterface networkInterface) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("Device not found with id: " + deviceId);
        }
        
        networkInterface.setDeviceId(deviceId);
        
        if (networkInterface.getMacAddress() == null || networkInterface.getMacAddress().isEmpty()) {
            String mac = MACAddressGenerator.generateMAC();
            while (interfaceRepository.existsByMacAddress(mac)) {
                mac = MACAddressGenerator.generateMAC();
            }
            networkInterface.setMacAddress(mac);
        }

        if (networkInterface.getName() == null || networkInterface.getName().trim().isEmpty()) {
            networkInterface.setName("eth0");
        }
        if (networkInterface.getSubnetMask() == null || networkInterface.getSubnetMask().trim().isEmpty()) {
            networkInterface.setSubnetMask("255.255.255.0");
        }
        if (networkInterface.getMtu() == null) {
            networkInterface.setMtu(1500);
        }
        if (networkInterface.getSpeed() == null || networkInterface.getSpeed().trim().isEmpty()) {
            networkInterface.setSpeed("1Gbps");
        }
        if (networkInterface.getDuplex() == null || networkInterface.getDuplex().trim().isEmpty()) {
            networkInterface.setDuplex("FULL");
        }
        if (networkInterface.getStatus() == null) {
            networkInterface.setStatus(InterfaceStatus.UP);
        }
        if (networkInterface.getAdminStatus() == null) {
            networkInterface.setAdminStatus(InterfaceStatus.UP);
        }
        if (networkInterface.getPortMode() == null || networkInterface.getPortMode().trim().isEmpty()) {
            networkInterface.setPortMode("ACCESS");
        }
        if (networkInterface.getVlanId() == null) {
            networkInterface.setVlanId(1);
        }
        if (networkInterface.getDnsServer() == null || networkInterface.getDnsServer().trim().isEmpty()) {
            networkInterface.setDnsServer("8.8.8.8");
        }
        
        validateIp(networkInterface.getIpAddress());
        
        return interfaceRepository.save(networkInterface);
    }

    public NetworkInterface updateInterface(Long id, NetworkInterface updatedInterface) {
        NetworkInterface existing = getInterface(id);
        
        validateIp(updatedInterface.getIpAddress());
        
        existing.setName(updatedInterface.getName());
        existing.setIpAddress(updatedInterface.getIpAddress());
        existing.setSubnetMask(updatedInterface.getSubnetMask());
        existing.setGateway(updatedInterface.getGateway());
        existing.setDnsServer(updatedInterface.getDnsServer());
        existing.setStatus(updatedInterface.getStatus());
        if (updatedInterface.getAdminStatus() != null) {
            existing.setAdminStatus(updatedInterface.getAdminStatus());
        }
        if (updatedInterface.getMtu() != null) {
            existing.setMtu(updatedInterface.getMtu());
        }
        if (updatedInterface.getSpeed() != null) {
            existing.setSpeed(updatedInterface.getSpeed());
        }
        if (updatedInterface.getDuplex() != null) {
            existing.setDuplex(updatedInterface.getDuplex());
        }
        if (updatedInterface.getVlanId() != null) {
            existing.setVlanId(updatedInterface.getVlanId());
        }
        if (updatedInterface.getPortMode() != null) {
            existing.setPortMode(updatedInterface.getPortMode());
        }
        if (updatedInterface.getAllowedVlans() != null) {
            existing.setAllowedVlans(updatedInterface.getAllowedVlans());
        }
        
        return interfaceRepository.save(existing);
    }

    public void deleteInterface(Long id) {
        interfaceRepository.deleteById(id);
    }

    private void validateIp(String ip) {
        if (ip != null && !ip.isEmpty() && !IPV4_PATTERN.matcher(ip).matches()) {
            throw new ValidationException("Invalid IPv4 address format: " + ip);
        }
    }
}

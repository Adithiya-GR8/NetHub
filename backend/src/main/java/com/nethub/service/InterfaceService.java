package com.nethub.service;

import com.nethub.entity.NetworkInterface;
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

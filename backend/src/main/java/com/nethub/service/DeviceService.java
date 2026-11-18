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
        return deviceRepository.save(device);
    }

    public Device updateDevice(Long id, Device updatedDevice) {
        Device existing = getDevice(id);
        existing.setName(updatedDevice.getName());
        existing.setStatus(updatedDevice.getStatus());
        existing.setPositionX(updatedDevice.getPositionX());
        existing.setPositionY(updatedDevice.getPositionY());
        return deviceRepository.save(existing);
    }

    public void deleteDevice(Long id) {
        deviceRepository.deleteById(id);
    }
}

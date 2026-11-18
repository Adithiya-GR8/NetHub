package com.nethub.controller;

import com.nethub.dto.ApiResponse;
import com.nethub.entity.Device;
import com.nethub.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping("/projects/{projectId}/devices")
    public ApiResponse<List<Device>> getDevices(@PathVariable Long projectId) {
        return ApiResponse.success(deviceService.getDevicesByProjectId(projectId), "Devices fetched successfully");
    }

    @GetMapping("/devices/{id}")
    public ApiResponse<Device> getDevice(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getDevice(id), "Device fetched successfully");
    }

    @PostMapping("/projects/{projectId}/devices")
    public ApiResponse<Device> createDevice(@PathVariable Long projectId, @RequestBody Device device) {
        return ApiResponse.success(deviceService.createDevice(projectId, device), "Device created successfully");
    }

    @PutMapping("/devices/{id}")
    public ApiResponse<Device> updateDevice(@PathVariable Long id, @RequestBody Device device) {
        return ApiResponse.success(deviceService.updateDevice(id, device), "Device updated successfully");
    }

    @DeleteMapping("/devices/{id}")
    public ApiResponse<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ApiResponse.success(null, "Device deleted successfully");
    }
}

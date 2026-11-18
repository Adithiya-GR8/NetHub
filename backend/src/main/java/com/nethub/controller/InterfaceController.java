package com.nethub.controller;

import com.nethub.dto.ApiResponse;
import com.nethub.entity.NetworkInterface;
import com.nethub.service.InterfaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InterfaceController {

    private final InterfaceService interfaceService;

    @GetMapping("/devices/{deviceId}/interfaces")
    public ApiResponse<List<NetworkInterface>> getInterfaces(@PathVariable Long deviceId) {
        return ApiResponse.success(interfaceService.getInterfacesByDeviceId(deviceId), "Interfaces fetched successfully");
    }

    @PostMapping("/devices/{deviceId}/interfaces")
    public ApiResponse<NetworkInterface> createInterface(@PathVariable Long deviceId, @RequestBody NetworkInterface networkInterface) {
        return ApiResponse.success(interfaceService.createInterface(deviceId, networkInterface), "Interface created successfully");
    }

    @PutMapping("/interfaces/{id}")
    public ApiResponse<NetworkInterface> updateInterface(@PathVariable Long id, @RequestBody NetworkInterface networkInterface) {
        return ApiResponse.success(interfaceService.updateInterface(id, networkInterface), "Interface updated successfully");
    }

    @DeleteMapping("/interfaces/{id}")
    public ApiResponse<Void> deleteInterface(@PathVariable Long id) {
        interfaceService.deleteInterface(id);
        return ApiResponse.success(null, "Interface deleted successfully");
    }
}

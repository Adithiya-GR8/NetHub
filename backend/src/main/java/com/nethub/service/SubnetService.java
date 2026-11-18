package com.nethub.service;

import com.nethub.model.Subnet;
import com.nethub.utils.SubnetCalculator;
import org.springframework.stereotype.Service;

@Service
public class SubnetService {

    public Subnet calculateSubnet(String ip, String mask) {
        return SubnetCalculator.calculate(ip, mask);
    }
}

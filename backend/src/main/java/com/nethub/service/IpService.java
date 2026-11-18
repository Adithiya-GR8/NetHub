package com.nethub.service;

import com.nethub.utils.IpUtils;
import org.springframework.stereotype.Service;

@Service
public class IpService {

    public boolean isValidIPv4(String ip) {
        return IpUtils.isValidIPv4(ip);
    }

    public boolean isValidSubnetMask(String mask) {
        return IpUtils.isValidSubnetMask(mask);
    }

    public boolean inSameSubnet(String ip1, String mask1, String ip2, String mask2) {
        return IpUtils.inSameSubnet(ip1, mask1, ip2, mask2);
    }

    public boolean isIpInSubnet(String ip, String networkIp, String mask) {
        return IpUtils.isIpInSubnet(ip, networkIp, mask);
    }

    public boolean isLocalDestination(String sourceIp, String sourceMask, String destIp) {
        return IpUtils.isLocal(sourceIp, sourceMask, destIp);
    }
}

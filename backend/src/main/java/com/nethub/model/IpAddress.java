package com.nethub.model;

import com.nethub.utils.IpUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IpAddress {
    private String value;
    private long longValue;

    public IpAddress(String value) {
        if (!IpUtils.isValidIPv4(value)) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + value);
        }
        this.value = value.trim();
        this.longValue = IpUtils.ipToLong(this.value);
    }

    public static IpAddress of(String value) {
        return new IpAddress(value);
    }

    public static IpAddress fromLong(long longVal) {
        return new IpAddress(IpUtils.longToIp(longVal));
    }

    @Override
    public String toString() {
        return value;
    }
}

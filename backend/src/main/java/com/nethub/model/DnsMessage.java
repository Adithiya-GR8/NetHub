package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DnsMessage {
    @Builder.Default
    private String transactionId = "0x4C8E";
    
    @Builder.Default
    private boolean response = false;
    
    private String queryDomain;
    
    @Builder.Default
    private String queryType = "A";
    
    private String resolvedIp;
    
    @Builder.Default
    private String responseCode = "NOERROR";
}


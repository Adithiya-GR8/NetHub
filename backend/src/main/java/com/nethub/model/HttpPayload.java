package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpPayload {
    @Builder.Default
    private boolean request = true;
    
    @Builder.Default
    private String method = "GET";
    
    @Builder.Default
    private String uri = "/index.html";
    
    @Builder.Default
    private int statusCode = 200;
    
    @Builder.Default
    private String statusText = "OK";
    
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
    
    @Builder.Default
    private String body = "<!DOCTYPE html><html><body><h1>NetHub Web Server</h1><p>HTTP Request Served Successfully.</p></body></html>";
}


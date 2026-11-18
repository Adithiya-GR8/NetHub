package com.nethub.model.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceStats {
    @Builder.Default
    private long packetsSent = 0;
    
    @Builder.Default
    private long packetsReceived = 0;
    
    @Builder.Default
    private long bytesSent = 0;
    
    @Builder.Default
    private long bytesReceived = 0;
    
    @Builder.Default
    private long droppedPackets = 0;
    
    @Builder.Default
    private long errors = 0;

    public void incrementTx(int bytes) {
        this.packetsSent++;
        this.bytesSent += bytes;
    }

    public void incrementRx(int bytes) {
        this.packetsReceived++;
        this.bytesReceived += bytes;
    }

    public void incrementDrop() {
        this.droppedPackets++;
    }

    public void incrementError() {
        this.errors++;
    }
}


package com.bro.binterface.door.tcp.gaoyou.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GaoYouDeviceMessage {
    private byte[] infoCode;
    private String deviceSn;
    private byte[] password;
    private int category;
    private int command;
    private int parameter;
    private byte[] data;
}

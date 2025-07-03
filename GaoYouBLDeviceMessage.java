package com.bro.binterface.door.udp.gaoyoubl.mesaage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GaoYouBLDeviceMessage {
    private byte[] infoCode;
    private String deviceSn;
    private byte[] password;
    private int category;
    private int command;
    private int parameter;
    private byte[] data;
}

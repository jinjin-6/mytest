package com.bro.binterface.door.udp.gaoyoubl;

import lombok.Data;

/**
 * UDP通信 配置采集参数类
 */
@Data
public class GaoYouBLDoorConfig {
    private String deviceSn;
    private String passwd;
    private int localPort; //本地监听端口
    private int serialPort; // 串口号
    private int virtualNum; // 虚拟设备号
    private int timeout = 5000; // 默认值
}

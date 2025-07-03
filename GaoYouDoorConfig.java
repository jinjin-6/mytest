package com.bro.binterface.door.tcp.gaoyou;

import lombok.Data;

/**
 * TCP通信 高优门禁 BM系列采集基础参数
 */
@Data
public class GaoYouDoorConfig {
    private String deviceSn;
    private String passwd;
    private int timeout = 5000; // 默认值
}

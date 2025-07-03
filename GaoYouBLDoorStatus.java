package com.bro.binterface.door.udp.gaoyoubl.mesaage;

import lombok.Data;

@Data
public class GaoYouBLDoorStatus {
    // 门继电器物理状态 (4字节)
    private final byte[] relayPhysicalState = new byte[4];
    // 运行状态 (4字节)
    private final byte[] runState = new byte[4];
    // 门磁开关状态 (4字节)
    private final byte[] doorSensorState = new byte[4];
    // 门报警状态 (4字节)
    private final byte[] alarmState = new byte[4];
    // 设备报警状态 (1字节)
    private byte deviceAlarmState;
    // 继电器逻辑状态 (8字节)
    private final byte[] relayLogicalState = new byte[8];
    // 锁定状态 (4字节)
    private final byte[] lockState = new byte[4];
    // 监控状态 (1字节)
    private byte monitorState;
    // 门内人数 (20字节)
    private final byte[] peopleCount = new byte[20];
    // 防盗主机布防状态 (1字节)
    private byte securityStatus;

    public GaoYouBLDoorStatus(byte[] data) {
        int pos = 0;
        
        // 门继电器物理状态 (4字节)
        System.arraycopy(data, pos, relayPhysicalState, 0, 4);
        pos += 4;
        
        // 运行状态 (4字节)
        System.arraycopy(data, pos, runState, 0, 4);
        pos += 4;
        
        // 门磁开关状态 (4字节)
        System.arraycopy(data, pos, doorSensorState, 0, 4);
        pos += 4;
        
        // 门报警状态 (4字节)
        System.arraycopy(data, pos, alarmState, 0, 4);
        pos += 4;
        
        // 设备报警状态 (1字节)
        deviceAlarmState = data[pos++];
        
        // 继电器逻辑状态 (8字节)
        System.arraycopy(data, pos, relayLogicalState, 0, 8);
        pos += 8;
        
        // 锁定状态 (4字节)
        System.arraycopy(data, pos, lockState, 0, 4);
        pos += 4;
        
        // 监控状态 (1字节)
        monitorState = data[pos++];
        
        // 门内人数 (20字节)
        System.arraycopy(data, pos, peopleCount, 0, 20);
        pos += 20;
        
        // 防盗主机布防状态 (1字节)
        securityStatus = data[pos];
    }

    /**
     * 门磁开关：判断指定门是否开启
     * @param doorIndex 门序号：1~4
     * @return true开 false关
     */
    public boolean isDoorOpen(int doorIndex) {
        if (doorIndex < 1 || doorIndex > 4) return false;
        return (doorSensorState[doorIndex - 1] & 0x01) != 0;
    }
    
    // 判断指定门是否锁定
    public boolean isDoorLocked(int doorIndex) {
        if (doorIndex < 1 || doorIndex > 4) return false;
        return (lockState[doorIndex - 1] & 0x01) != 0;
    }
    
    // 判断指定门是否有报警
    public boolean hasAlarm(int doorIndex) {
        if (doorIndex < 1 || doorIndex > 4) return false;
        return (alarmState[doorIndex - 1] & 0x01) != 0;
    }
    
    // 获取指定门内人数
    public int getPeopleCount(int doorIndex) {
        if (doorIndex < 1 || doorIndex > 4) return -1;
        int start = (doorIndex - 1) * 4;
        return ((peopleCount[start] & 0xFF) << 24) |
               ((peopleCount[start + 1] & 0xFF) << 16) |
               ((peopleCount[start + 2] & 0xFF) << 8) |
               (peopleCount[start + 3] & 0xFF);
    }
    
    // 监控是否启用
    public boolean isMonitorEnabled() {
        return monitorState != 0;
    }

    /**
     * 通过继电器逻辑开关状态获取门状态
     * @param doorIndex
     * @return true开 false关
     */
    public int doorStatus(int doorIndex) {
        if (doorIndex < 1 || doorIndex > 4) return 0;
        int state = relayLogicalState[doorIndex - 1] & 0xFF;

        return state;
    }
    
    // ... 其他状态检查方法 ...
}


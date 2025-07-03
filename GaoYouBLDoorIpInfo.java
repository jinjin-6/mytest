package com.bro.binterface.door.udp.gaoyoubl.mesaage;

import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * IP信息
 */
@Data
public class GaoYouBLDoorIpInfo {
    private byte[] macAddress = new byte[6]; // MAC地址
    private byte[] ipAddress = new byte[4]; // IP地址
    private byte[] subnetMask = new byte[4]; // 子网掩码
    private byte[] gateway = new byte[4]; // 网关IP
    private byte[] dns = new byte[4]; // DNS
    private byte[] backupDns = new byte[4]; // 备用DNS
    private int tcpMode; // TCP工作模式：1-TCP  client 2-TCP Server 3-混合
    private int localTcpPort; // 本地TCP监听端口
    private int localUdpPort; // 本地UDP监听端口
    private int targetPort; // 目标端口
    private byte[] targetIp = new byte[4]; // 目标ip
    private boolean autoIp; // 自动获得IP
    private String domain; // 目标域名

    private byte[] origData;
    
    public GaoYouBLDoorIpInfo(byte[] data) {
        this.origData = Arrays.copyOf(data, data.length);

        int pos = 0;
        
        // MAC地址 (6字节)
        System.arraycopy(data, pos, macAddress, 0, 6);
        pos += 6;
        
        // IP地址 (4字节)
        System.arraycopy(data, pos, ipAddress, 0, 4);
        pos += 4;
        
        // 子网掩码 (4字节)
        System.arraycopy(data, pos, subnetMask, 0, 4);
        pos += 4;
        
        // 网关 (4字节)
        System.arraycopy(data, pos, gateway, 0, 4);
        pos += 4;
        
        // DNS (4字节)
        System.arraycopy(data, pos, dns, 0, 4);
        pos += 4;
        
        // 备用DNS (4字节)
        System.arraycopy(data, pos, backupDns, 0, 4);
        pos += 4;
        
        // TCP工作模式 (1字节)
        tcpMode = data[pos++] & 0xFF;
        
        // 本地TCP监听端口 (2字节)
        localTcpPort = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
        pos += 2;
        
        // 本地UDP监听端口 (2字节)
        localUdpPort = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
        pos += 2;
        
        // 目标端口 (2字节)
        targetPort = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
        pos += 2;
        
        // 目标IP (4字节)
        System.arraycopy(data, pos, targetIp, 0, 4);
        pos += 4;
        
        // 自动获得IP (1字节)
        autoIp = data[pos++] != 0;
        
        // 目标域名 (99字节)
        int domainEnd = pos;
        while (domainEnd < data.length && data[domainEnd] != 0) {
            domainEnd++;
        }
        domain = new String(data, pos, domainEnd - pos, StandardCharsets.US_ASCII);
    }

    /**
     * 获取Mac
     * @return
     */
    public String getMacAddress() {
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X", 
            macAddress[0] & 0xFF, macAddress[1] & 0xFF, macAddress[2] & 0xFF,
            macAddress[3] & 0xFF, macAddress[4] & 0xFF, macAddress[5] & 0xFF);
    }

    /**
     * 获取IP地址
     * @return
     */
    public String getIpAddress() {
        return (ipAddress[0] & 0xFF) + "." + (ipAddress[1] & 0xFF) + "." + 
               (ipAddress[2] & 0xFF) + "." + (ipAddress[3] & 0xFF);
    }

    /**
     * 获取子网掩码
     * @return
     */
    public String getSubnetMask() {
        return (subnetMask[0] & 0xFF) + "." + (subnetMask[1] & 0xFF) + "." +
                (subnetMask[2] & 0xFF) + "." + (subnetMask[3] & 0xFF);
    }

    /**
     * 网关IP
     * @return
     */
    public String getGateway() {
        return (gateway[0] & 0xFF) + "." + (gateway[1] & 0xFF) + "." +
                (gateway[2] & 0xFF) + "." + (gateway[3] & 0xFF);
    }

    /**
     * 获取DNS
     * @return
     */
    public String getDns() {
        return (dns[0] & 0xFF) + "." + (dns[1] & 0xFF) + "." +
                (dns[2] & 0xFF) + "." + (dns[3] & 0xFF);
    }

    /**
     * 获取备用DNS
     * @return
     */
    public String getBackupDns() {
        return (backupDns[0] & 0xFF) + "." + (backupDns[1] & 0xFF) + "." +
                (backupDns[2] & 0xFF) + "." + (backupDns[3] & 0xFF);
    }

    /**
     * 获取TCP工作模式
     * @return
     */
    public String getTcpMode() {
        switch (tcpMode) {
            case 1:
                return "TCP client";
            case 2:
                return "TCP Server";
            case 3:
                return "混合";
            default: return "";
        }
    }

    /**
     * 目标IP
     * @return
     */
    public String getTargetIp() {
        return (targetIp[0] & 0xFF) + "." + (targetIp[1] & 0xFF) + "." +
                (targetIp[2] & 0xFF) + "." + (targetIp[3] & 0xFF);
    }
}

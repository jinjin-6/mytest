package com.bro.binterface.door.udp.domain;

import lombok.Data;

/**
 * 透传数据包格式
 */
@Data
public class TransparentPacket {
    private byte pHeader = (byte) 0xFF; // 协议包头(1 byte) 0xFF
    private byte[] pDestAddr; // 目标设备地址 (20 bytes) (FSU的ID)
    private byte[] pSrcAddr; // 源设备地址(8 bytes) (SC端地址为0x00)
    private byte subDevType; // 子设备类型(1 byte)：1-串口设备, 2-USB设备, 3-IP网络设备
    private byte subDevAddr; // 透传模块地址(1 byte) Bit0~4:串口号；Bit5~8： 表示虚拟设备号
    private short pLen;   // 协议族数据包长度(2 bytes)
    private byte rtnFlag; // 设置/应答类型(1 byte)
    private TransparentCommand commandType; // 命令编号(2 bytes)
    private short dataLength; // 透传数据长度 (2 bytes)
    private byte[] data; // 透传数据内容 (N bytes)
    private byte pVerify; // 校验字段 (异或校验) (1 byte)
    private byte pTailer = (byte) 0xFE; // 协议包尾 0xFE (1 byte)

    // 获取串口号（bit0 ~ bit3）
    public int getComPort() {
        return subDevAddr & 0x0F; // 与 00001111 做按位与操作
    }

    // 设置串口号（bit0 ~ bit3）
    public void setComPort(int comPort) {
        if (comPort < 0 || comPort > 0x0F) {
            throw new IllegalArgumentException("串口号必须在 0~15 之间");
        }
        this.subDevAddr = (byte) ((subDevAddr & 0xF0) | (comPort & 0x0F));
    }

    // 获取虚拟设备号（bit4 ~ bit7）
    public int getVirtualDeviceAddress() {
        return (subDevAddr >> 4) & 0x0F; // 右移4位后与 00001111 做按位与
    }

    // 设置虚拟设备号（bit4 ~ bit7）
    public void setVirtualDeviceAddress(int virtualAddr) {
        if (virtualAddr < 0 || virtualAddr > 0x0F) {
            throw new IllegalArgumentException("虚拟设备号必须在 0~15 之间");
        }
        this.subDevAddr = (byte) ((subDevAddr & 0x0F) | ((virtualAddr & 0x0F) << 4));
    }

    /**
     * 同时设置串口号和虚拟设备号
     *
     * @param comPort      串口号（bit0~3，取值 0~15）
     * @param virtualAddr  虚拟设备号（bit4~7，取值 0~15）
     */
    public void setComPortAndVirtualAddr(int comPort, int virtualAddr) {
        if (comPort < 0 || comPort > 0x0F || virtualAddr < 0 || virtualAddr > 0x0F) {
            throw new IllegalArgumentException("串口号和虚拟设备号必须在 0~15 范围内");
        }

        // 构造 subDevAddr = (virtualAddr << 4) | (comPort & 0x0F)
        this.subDevAddr = (byte) ((virtualAddr << 4) | (comPort & 0x0F));
    }
}

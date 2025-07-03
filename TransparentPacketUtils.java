package com.bro.binterface.door.udp.utils;

import com.bro.binterface.door.udp.domain.TransparentCommand;
import com.bro.binterface.door.udp.domain.TransparentPacket;
import com.bro.binterface.door.utils.Utils;
import com.bro.binterface.transparent.util.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransparentPacketUtils {

    /**
     * 构建完整透传协议包数据
     * @param command 透传数据
     * @param fsuId
     * @param serialPort 串口号
     * @param virtualNum 虚拟设备号
     * @return
     */
    public static byte[] buildTransparentPacket(byte[] command, String fsuId, int serialPort, int virtualNum) {
        byte[] packet = new byte[40 + command.length];
        // P_header 协议包的开始标识ff 1 byte
        packet[0] = (byte) 0xFF;
        // P_dest_addr 目标设备地址 20bytes
        byte[] fsuByte = UdpUtils.convertFsuIdTo20Bytes(fsuId);
        System.arraycopy(fsuByte, 0, packet, 1, 20);
        // P_src_addr 源设备地址 8byte
        System.arraycopy(new byte[8], 0, packet, 21, 8);
        // P_subDevType 子设备类型 1 byte
        packet[29] = 0x01;
        // P_subDev_addr Bit0~4串口号；Bit5~8表示虚拟设备号 1 byte
        packet[30] = (byte) ((virtualNum << 4) | (serialPort & 0x0F));
        // P_pLen 协议族数据包长度 5+N 2byte
        packet[31] = (byte) ((5 + command.length) >> 8);
        packet[32] = (byte) (5 + command.length);
        // RtnFlag 设置/应答类型 1byte
        packet[33] = (byte) 0xEE;
        // CommType 命令编号 2bytes
        packet[34] = 0x00;
        packet[35] = 0x01;
        // 透传数据长度 2byte
        packet[36] = (byte) (command.length >> 8);
        packet[37] = (byte) command.length;
        // 透传数据 N byte
        System.arraycopy(command, 0, packet, 38, command.length);
        // 校验
        packet[38 + command.length] = calculateChecksum(packet);
        // 包尾
        packet[39 + command.length] = (byte) 0xFE;

        return escapePacket(packet);
    }

    /**
     * 解析协议包 (FSU -> SC)
     */
    public static TransparentPacket parseTransparentPacket(byte[] data) {
        if (data == null || data.length < 40) {
            throw new IllegalArgumentException("数据包长度不足");
        }

        // 检查包头和包尾是否存在
        if (data[0] != 0xFF || data[data.length - 1] != 0XFE) {
            throw new IllegalArgumentException("协议包格式错误");
        }

        // 提取中间数据（不包含包头和包尾）
        byte[] middle = Arrays.copyOfRange(data, 1, data.length - 1);

        // 反转义中间数据
        byte[] unescapedMiddle = unescapeData(middle);

        // 构造完整的未转义数据：包头 + 反转义后的内容 + 包尾
        byte[] unescapedData = new byte[1 + unescapedMiddle.length + 1];
        System.arraycopy(new byte[]{(byte)0xFF}, 0, unescapedData, 0, 1);
        System.arraycopy(unescapedMiddle, 0, unescapedData, 1, unescapedMiddle.length);
        System.arraycopy(new byte[]{(byte)0xFE}, 0, unescapedData, 1 + unescapedMiddle.length, 1);

        // 解析数据
        TransparentPacket transparentPacket = new TransparentPacket();
        int pos = 0;

        // 包头 (1 byte)
        transparentPacket.setPHeader(unescapedData[pos++]);

        // 目标地址 (8 bytes)
        byte[] destAddr = new byte[8];
        System.arraycopy(unescapedData, pos, destAddr, 0, 8);
        transparentPacket.setPDestAddr(destAddr);
        pos += 8;

        // 源地址 (20 bytes)
        byte[] srcAddr = new byte[20];
        System.arraycopy(unescapedData, pos, srcAddr, 0, 20);
        transparentPacket.setPSrcAddr(srcAddr);
        pos += 20;

        // 子设备类型 (1 byte)
        transparentPacket.setSubDevType(unescapedData[pos++]);

        // 透传模块地址 (1 byte)
        transparentPacket.setSubDevAddr(unescapedData[pos++]);

        // 协议族数据包长度 (2 bytes)
        byte[] pLenBytes = new byte[2];
        System.arraycopy(unescapedData, pos, pLenBytes, 0, 2);
        transparentPacket.setPLen(ByteUtils.bytesToShort(pLenBytes));
        pos += 2;

        // 设置/应答类型 (1 byte)
        transparentPacket.setRtnFlag(unescapedData[pos++]);

        // 命令编号 (2 bytes)
        byte[] cmdBytes = new byte[2];
        System.arraycopy(unescapedData, pos, cmdBytes, 0, 2);
        transparentPacket.setCommandType(TransparentCommand.fromCode(Utils.bytesToShort(cmdBytes)));
        pos += 2;

        // 透传数据长度 (2 bytes)
        byte[] dataLenBytes = new byte[2];
        System.arraycopy(unescapedData, pos, dataLenBytes, 0, 2);
        transparentPacket.setDataLength(ByteUtils.bytesToShort(dataLenBytes));
        pos += 2;

        // 透传数据 (N bytes)
        int dataLen = transparentPacket.getDataLength();
        if (dataLen > 0) {
            byte[] payload = new byte[dataLen];
            System.arraycopy(unescapedData, pos, payload, 0, dataLen);
            transparentPacket.setData(payload);
            pos += dataLen;
        }

        // 校验和 (1 byte)
        transparentPacket.setPVerify(unescapedData[pos++]);

        // 包尾 (1 byte)
        transparentPacket.setPTailer(unescapedData[pos]);

        // 验证校验和
        byte calculatedChecksum = calculateChecksum(unescapedData);
        if (calculatedChecksum != transparentPacket.getPVerify()) {
            throw new IllegalArgumentException("校验和验证失败");
        }

        return transparentPacket;
    }

    /**
     * 计算校验和 (异或校验)
     * -- 排除首尾和校验位
     */
    private static byte calculateChecksum(byte[] data) {
        byte checksum = 0;
        for (int i = 1; i < data.length - 2; i++) {
            checksum ^= data[i];
        }
        return checksum;
    }

    /**
     * 对完整数据包进行转义处理
     * - 包头和包尾不转义
     * - 中间内容需要转义
     */
    private static byte[] escapePacket(byte[] fullPacket) {
        if (fullPacket == null || fullPacket.length < 2) {
            return fullPacket;
        }

        // 包头 (不转义)
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(fullPacket[0]);

        // 中间内容 (需要转义)
        byte[] middle = Arrays.copyOfRange(fullPacket, 1, fullPacket.length - 1);
        byte[] escapedMiddle = escapeData(middle);
        output.write(escapedMiddle, 0, escapedMiddle.length);

        // 包尾 (不转义)
        output.write(fullPacket[fullPacket.length - 1]);

        return output.toByteArray();
    }

    /**
     * 转义处理
     * - 0xFF -> 0xFD 0x00
     * - 0xFE -> 0xFD 0x01
     * - 0xFD -> 0xFD 0x02
     */
    public static byte[] escapeData(byte[] original) {
        if (original == null) {
            return null;
        }

        List<Byte> escaped = new ArrayList<>();
        for (byte b : original) {
            if (b == (byte) 0xFF) {
                escaped.add((byte) 0xFD);
                escaped.add((byte) 0x00);
            } else if (b == (byte) 0xFE) {
                escaped.add((byte) 0xFD);
                escaped.add((byte) 0x01);
            } else if (b == (byte) 0xFD) {
                escaped.add((byte) 0xFD);
                escaped.add((byte) 0x02);
            } else {
                escaped.add(b);
            }
        }

        byte[] result = new byte[escaped.size()];
        for (int i = 0; i < escaped.size(); i++) {
            result[i] = escaped.get(i);
        }
        return result;
    }

    /**
     * 反转义处理
     */
    public static byte[] unescapeData(byte[] escaped) {
        if (escaped == null) {
            return null;
        }

        List<Byte> unescaped = new ArrayList<>();
        for (int i = 0; i < escaped.length; i++) {
            if (escaped[i] == (byte) 0xFD && i + 1 < escaped.length) {
                switch (escaped[i + 1]) {
                    case 0x00:
                        unescaped.add((byte) 0xFF);
                        break;
                    case 0x01:
                        unescaped.add((byte) 0xFE);
                        break;
                    case 0x02:
                        unescaped.add((byte) 0xFD);
                        break;
                    default:
                        unescaped.add(escaped[i]);
                        continue;
                }
                i++;
            } else {
                unescaped.add(escaped[i]);
            }
        }

        byte[] result = new byte[unescaped.size()];
        for (int i = 0; i < unescaped.size(); i++) {
            result[i] = unescaped.get(i);
        }
        return result;
    }

}

package com.bro.binterface.door.udp.gaoyoubl.utils;

import com.bro.binterface.door.tcp.gaoyou.message.GaoYouDoorIpInfo;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouReportMessage;
import com.bro.binterface.door.udp.gaoyoubl.mesaage.*;
import com.bro.binterface.door.utils.Utils;
import com.bro.common.core.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
public class GaoYouBLProtocolParser {

    /**
     * 报文反转义
     *
     * @param packet
     * @param pkLen
     * @return
     */
    public static byte[] decodeData(byte[] packet, int pkLen) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean isEscaped = false;
        for (int i = 0; i < pkLen; i++) {
            byte current = packet[i];
            if (isEscaped) {
                if (current == 0x01) {
                    out.write(0x7E);
                    isEscaped = false;
                } else {
                    out.write(0x7F);
                    isEscaped = false;
                }
            } else if (current == 0x7F) {
                isEscaped = true;
            } else {
                out.write(current);
            }
        }
        return out.toByteArray();
    }

    /**
     * 协议解析
     * 
     * @param data 数据包：不包含头尾字节
     * @return
     */
    public static GaoYouBLDeviceMessage parseProtocol(byte[] data) {
        GaoYouBLDeviceMessage msg = new GaoYouBLDeviceMessage();
        int pos = 1;

        // 信息代码 (4字节)
        byte[] infoCode = new byte[4];
        System.arraycopy(data, pos, infoCode, 0, 4);
        msg.setInfoCode(infoCode);
        pos += 4;

        // 设备SN (16字节)
        byte[] snBytes = new byte[16];
        System.arraycopy(data, pos, snBytes, 0, 16);
        msg.setDeviceSn(new String(snBytes, StandardCharsets.US_ASCII));
        pos += 16;

        // 通讯密码 (4字节)
        byte[] password = new byte[4];
        System.arraycopy(data, pos, password, 0, 4);
        msg.setPassword(password);
        pos += 4;

        // 控制码
        msg.setCategory(data[pos++] & 0xFF);
        msg.setCommand(data[pos++] & 0xFF);
        msg.setParameter(data[pos++] & 0xFF);

        // 数据长度 (4字节)
        int dataLength = ((data[pos] & 0xFF) << 24) |
                ((data[pos + 1] & 0xFF) << 16) |
                ((data[pos + 2] & 0xFF) << 8) |
                (data[pos + 3] & 0xFF);
        pos += 4;

        // 数据内容
        if (dataLength > 0 && (pos + dataLength) <= data.length) {
            byte[] content = new byte[dataLength];
            System.arraycopy(data, pos, content, 0, dataLength);
            msg.setData(content);
        }

        return msg;
    }

    /**
     * 解析所有授权卡
     * @param response
     */
    public static List<GaoYouBLCardInfo> parseAllUserPermissionResponse(byte[] response) {
        // 跳过基本信息 (FE 1B + 信息代码 4B + SN 16B + 密码 4B)
        int start = 25;
        // 控制码 (3B)
        int category = response[start] & 0xFF;
        int command = response[start + 1] & 0xFF;
        int parameter = response[start + 2] & 0xFF;
        if (category != 0x37 || command != 0x03 || parameter != 0x00) {
            return null;
        }
        // 数据长度 (4B)
        int dataLength = ((response[start + 3] & 0xFF) << 24) |
                ((response[start + 4] & 0xFF) << 16) |
                ((response[start + 5] & 0xFF) << 8) |
                (response[start + 6] & 0xFF);

        // 数据内容 (0x04+0x21*n字节)
        byte[] data = new byte[dataLength];
        System.arraycopy(response, start + 7, data, 0, dataLength);
        // 前4字节是卡数量（大端序）
        int cardCount = ((data[0] & 0xFF) << 24) |
                        ((data[1] & 0xFF) << 16) |
                        ((data[2] & 0xFF) << 8) |
                        (data[3] & 0xFF);
        if (cardCount == 0) {
            return null;
        }
        // 检查数据总长度是否正确 (4 + 33*n)
        if (data.length != 4 + 0x21 * cardCount) {
            throw new IllegalArgumentException(
                    String.format("数据长度不匹配，应有%d字节(4+33×%d)，实际%d字节",
                            4 + 0x21 * cardCount, cardCount, data.length));
        }
        List<GaoYouBLCardInfo> cardInfos = new ArrayList<>(cardCount);
        int pos = 4; // 跳过4字节数量字段
        for (int i = 0; i < cardCount; i++) {
            byte[] cardData = Arrays.copyOfRange(data, pos, pos + 0x21);
            cardInfos.add(new GaoYouBLCardInfo(cardData));
            pos += 0x25;
        }

        return cardInfos;
    }

    /**
     * 解析TCP参数
     * @param response
     * @return
     */
    public static GaoYouBLDoorIpInfo parseTcpResponse(byte[] response) {
        // 跳过基本信息 (FE 1B + 信息代码 4B + SN 16B + 密码 4B)
        int start = 25;
        // 控制码 (3B)
        int category = response[start] & 0xFF;
        int command = response[start + 1] & 0xFF;
        int parameter = response[start + 2] & 0xFF;
        if (category != 0x31 || command != 0x06 || parameter != 0x00) {
            return null;
        }
        // 数据长度 (4B)
        int dataLength = ((response[start + 3] & 0xFF) << 24) |
                ((response[start + 4] & 0xFF) << 16) |
                ((response[start + 5] & 0xFF) << 8) |
                (response[start + 6] & 0xFF);
        // 数据内容 (137字节)
        byte[] data = new byte[dataLength];
        System.arraycopy(response, start + 7, data, 0, dataLength);

        return new GaoYouBLDoorIpInfo(data);

    }

    /**
     * 解析设备搜索响应
     * 
     * @param response 设备响应数据
     * @return 设备信息对象
     */
    public static GaoYouDoorIpInfo parseDeviceSearchResponse(byte[] response) {
        // 跳过基本信息 (FE 1B + 信息代码 4B + SN 16B + 密码 4B)
        int start = 25;

        // 控制码 (3B)
        int category = response[start] & 0xFF;
        int command = response[start + 1] & 0xFF;
        int parameter = response[start + 2] & 0xFF;
        if (category != 0x31 || command != 0xFE || parameter != 0x00) {
            return null;
        }

        // 数据长度 (4B)
        int dataLength = ((response[start + 3] & 0xFF) << 24) |
                ((response[start + 4] & 0xFF) << 16) |
                ((response[start + 5] & 0xFF) << 8) |
                (response[start + 6] & 0xFF);

        // 数据内容 (137字节)
        byte[] data = new byte[dataLength];
        System.arraycopy(response, start + 7, data, 0, dataLength);

        return new GaoYouDoorIpInfo(data);
    }

    /**
     * 解析门状态响应
     * 
     * @param response 设备响应数据
     * @return 门状态对象
     */
    public static GaoYouBLDoorStatus parseDoorStatusResponse(byte[] response) {
        // 跳过基本信息 (FE 1B + 信息代码 4B + SN 16B + 密码 4B)
        int start = 25;

        // 控制码 (3B)
        int category = response[start] & 0xFF;
        int command = response[start + 1] & 0xFF;
        int parameter = response[start + 2] & 0xFF;
        if (category != 0x31 || command != 0x0E || parameter != 0x00) {
            return null;
        }

        // 数据长度 (4B)
        int dataLength = ((response[start + 3] & 0xFF) << 24) |
                ((response[start + 4] & 0xFF) << 16) |
                ((response[start + 5] & 0xFF) << 8) |
                (response[start + 6] & 0xFF);

        // 数据内容 (52字节)
        byte[] data = new byte[dataLength];
        System.arraycopy(response, start + 7, data, 0, dataLength);

        return new GaoYouBLDoorStatus(data);
    }

    /**
     * 解析事件记录
     * 
     * @param packet
     */
    public static GaoYouReportMessage procRecord(byte[] packet, int recType) {
        if (recType == 1) // 刷卡记录
        {
            String cardNo = Utils.int9ToCardno(packet, 40);
            String timeStr = Utils.bcdToString(packet, 49, 6);
            Date timeDate = DateUtils.dateTime(DateUtils.YYYYMMDDHHMMSS, "20" + timeStr);
            String timeFormatStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, timeDate);
            int doorNum = packet[55];
            int status = packet[56];

            log.debug("读取刷卡记录:卡号[{}],时间[{}],门号[{}],状态[{}]",
                    cardNo, timeStr, doorNum, status);
            return new GaoYouReportMessage(String.valueOf(doorNum), timeFormatStr, status, cardNo, recType);
        } else {
            int doorNum = packet[40];
            String timeStr = Utils.bcdToString(packet, 41, 6);
            Date timeDate = DateUtils.dateTime(DateUtils.YYYYMMDDHHMMSS, "20" + timeStr);
            String timeFormatStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, timeDate);
            int status = packet[47];

            log.info("读取其它记录[{}]:时间[{}],门号[{}],状态[{}]",
                    recType, timeStr, doorNum, status);
            return new GaoYouReportMessage(String.valueOf(doorNum), timeFormatStr, status, "", recType);
        }
    }

    /**
     * 解析事件上报
     * 
     * @param response
     * @return
     */
    public static GaoYouBLReportMessage parseEventReportResponse(byte[] response) {
        log.debug("收到门禁控制器主动上报事件[{},{},{}]",
                Utils.bytesToHex(response, 25, 1), Utils.bytesToHex(response, 26, 1),
                Utils.bytesToHex(response, 27, 1));

        if (response[25] != 0x19)
            return null;

        if (response[26] == 0x01) {
            // 读卡信息
            String carNo = Utils.int9ToCardno(response, 32);
            // 时间转换
            String timeStr = Utils.bcdToString(response, 41, 6);
            Date timeDate = DateUtils.dateTime(DateUtils.YYYYMMDDHHMMSS, "20" + timeStr);
            String timeFormatStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, timeDate);
            // 门号
            int door_num = response[47];
            // 状态
            int status = response[48];

            log.debug("收到刷卡信息:卡号[{}],时间[{}],门号[{}],状态[{}]",
                    carNo, timeFormatStr, door_num, status);

            return new GaoYouBLReportMessage(String.valueOf(door_num), timeFormatStr, status, carNo, response[26]);
        } else {
            int door_num = response[32]; // 门号
            String timeStr = Utils.bcdToString(response, 33, 6); // 时间
            Date timeDate = DateUtils.dateTime(DateUtils.YYYYMMDDHHMMSS, "20" + timeStr);
            String timeFormatStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, timeDate);
            int status = response[39]; // 状态

            switch (response[26]) {
                case 0x02:
                    // 出门开关信息
                    log.debug("收到出门开关信息:门号[{}],时间[{}],状态[{}]", door_num, timeFormatStr, status);
                    break;
                case 0x03:
                    log.debug("收到门磁信息:门号[{}],时间[{}],状态[{}]", door_num, timeFormatStr, status);
                            break;
                case 0x04:
                    // 远程开关门
                    log.debug("收到远程开门信息:门号[{}],时间[{}],状态[{}]", door_num, timeFormatStr, status);
                    break;
                case 0x05:
                    // 报警信息
                    log.debug("收到报警信息:门号[{}],时间[{}],状态[{}]", door_num, timeFormatStr, status);
                    break;
                case 0x06:
                    log.debug("收到系统信息:门号[{}],时间[{}],状态[{}]", door_num, timeFormatStr, status);
                    break;
                default:
                    break;
            }
            return new GaoYouBLReportMessage(String.valueOf(door_num), timeFormatStr, status, "", response[26]);
        }
    }
}

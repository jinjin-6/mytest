package com.bro.binterface.door.udp.utils;

import com.bro.binterface.door.udp.domain.TransparentPacket;
import com.bro.binterface.door.udp.gaoyoubl.GaoYouBLAccessDevice;
import com.bro.binterface.door.utils.Utils;
import com.bro.binterface.message.AlarmPublisher;
import com.bro.binterface.service.INoticeService;
import com.bro.common.core.constant.FSUConstants;
import com.bro.common.core.utils.DateUtils;
import com.bro.common.core.utils.SpringUtils;
import com.bro.common.redis.service.RedisService;

import java.nio.charset.StandardCharsets;

public class UdpUtils {
    private static final long HEARTBEAT_TIMEOUT = 360000; // 360秒
    /**
     * 将字符串转换为20位的字节数组，不足部分用0填充，超出部分截断
     * @param fsuId 输入的字符串（如 "2003"）
     * @return 20位的字节数组，如 "2003" -> [0,0,...,0x32,0x30,0x30,0x33]
     */
    public static byte[] convertFsuIdTo20Bytes(String fsuId) {
        byte[] result = new byte[20];
        if (fsuId == null || fsuId.isEmpty()) {
            return result; // 全0
        }

        // 将字符串转换为字节数组（UTF-8编码）
        byte[] strBytes = fsuId.getBytes(StandardCharsets.UTF_8);

        // 计算填充起始位置（右对齐）
        int startPos = Math.max(0, result.length - strBytes.length);

        // 复制字符串字节到结果数组的右侧
        System.arraycopy(
                strBytes,                       // 源数组
                0,                              // 源起始位置
                result,                         // 目标数组
                startPos,                       // 目标起始位置
                Math.min(strBytes.length, 20)   // 复制的长度
        );

        return result;
    }

    /**
     * 将fsu数组转换称字符串
     * @param bytes
     * @return
     */
    public static String convert20BytesToFsuId(byte[] bytes) {
        if (bytes == null || bytes.length != 20) {
            return "";
        }

        // 找到第一个非零字节的位置
        int startIndex = 0;
        while (startIndex < bytes.length && bytes[startIndex] == 0) {
            startIndex++;
        }

        // 如果全部是零，返回空字符串
        if (startIndex == bytes.length) {
            return "";
        }

        // 复制非零部分到新数组
        int length = bytes.length - startIndex;
        byte[] strBytes = new byte[length];
        System.arraycopy(bytes, startIndex, strBytes, 0, length);

        // 转换为字符串
        return new String(strBytes, StandardCharsets.UTF_8);
    }

    /**
     * 判断是否是心跳包
     */
    public static boolean isHeartbeatPacket(byte[] data) {
        return data.length >= 38 &&
                data[33] == (byte)0xED &&
                data[34] == 0x00 &&
                data[35] == 0x02; // 命令号0x0002
    }

    /**
     * 判断是否是命令响应 FSU=>SC
     */
    public static boolean isCommandResponse(byte[] data) {
        return data.length >= 38 &&
                data[33] == 0x00 &&
                data[34] == 0x00 &&
                data[35] == 0x01; // 命令号0x0001; // RtnFlag=0x00表示应答
    }

    /**
     * 校验返回命令包
     * @param packet
     * @param device
     * @return
     */
    public static boolean verifyResponse(TransparentPacket packet, GaoYouBLAccessDevice device) {
        // 校验fsuId和串口号、虚拟设备号
        String fsuId = convert20BytesToFsuId(packet.getPDestAddr());
        int serialPort = packet.getComPort();
        int virtualNum = packet.getVirtualDeviceAddress();
        // 校验
        if (!device.getFsuId().equals(fsuId) || device.getConfig().getSerialPort() != serialPort
                || device.getConfig().getVirtualNum() != virtualNum) {
            return false;
        }
        return true;
    }

    /**
     * 更新心跳时间
     * @param doorId fsuId:deviceId
     */
    public static Long updateHeartbeat(String doorId) {
        // fsu:udp:heartbeat:fsuId:deviceId
        String redisKey = FSUConstants.REDIS_FSU_UDP_HEARTBEAT_PREFIX + doorId;
        RedisService redisService = SpringUtils.getBean(RedisService.class);
        // 获取前一次的心跳时间
        Long previousTime = redisService.getCacheObject(redisKey);
        // 设置新的心跳时间（注意单位转换）
        long currentTime = System.currentTimeMillis();
        redisService.setCacheObject(redisKey, currentTime);
        return previousTime;
    }

    /**
     * 通知透传通道连接恢复
     */
    public static void notifyConnectionRecovery(String fsuId, String deviceName) {
        AlarmPublisher alarmPublisher = SpringUtils.getBean(AlarmPublisher.class);
        String time = DateUtils.getTime();
        String title = "UDP透传通道[FSUID:" + fsuId  + "]：恢复心跳 " + time;
        String noticeContent =
                "UDP透传通道[FSUID:" + fsuId  + "] [门禁名称：" + deviceName + "]：恢复心跳 " + time;
        alarmPublisher.sendFSUAlarm(title);
        INoticeService noticeService = SpringUtils.getBean(INoticeService.class);
        noticeService.saveNoticeData(title, noticeContent, fsuId);
    }

    public static void main(String[] args) {
        System.out.println(Utils.bytesToHex(convertFsuIdTo20Bytes("test001")));
    }
}

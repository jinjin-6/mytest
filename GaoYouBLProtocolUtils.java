package com.bro.binterface.door.udp.gaoyoubl.utils;

import com.bro.binterface.door.tcp.gaoyou.constants.GaoYouConstants;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouDoorRecordInfo;
import com.bro.binterface.door.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;

public class GaoYouBLProtocolUtils {

    /**
     * 构建协议命令的通用方法
     */
    public static byte[] buildCommand(
            String deviceSn,
            String password,
            int serial,
            byte category,
            byte command,
            byte parameter,
            byte[] dataContent) {

        int dataLength = (dataContent != null) ? dataContent.length : 0;

        // 创建缓冲区
        ByteBuffer buf = ByteBuffer.allocate(32 + dataLength);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序

        // 1. 设备SN (16字节)
        byte[] snBytes = Arrays.copyOf(deviceSn.getBytes(StandardCharsets.US_ASCII), 16);
        buf.put(snBytes);

        // 2. 通讯密码 (4字节)
        buf.put(procPasswd(password));

        // 3. 信息代码 (4字节)
        buf.putInt(serial);

        // 4. 控制码 (分类1B + 命令1B + 参数1B)
        buf.put(category);
        buf.put(command);
        buf.put(parameter);

        // 5. 数据长度 (4字节)
        // int dataLength = (dataContent != null) ? dataContent.length : 0;
        buf.putInt(dataLength);

        // 6. 数据内容
        if (dataLength > 0) {
            buf.put(dataContent);
        }

        // 7. 计算校验和 (除头尾外所有字节的累加和低字节)
        byte checksum = Utils.calculateChecksum(buf);
        buf.put(checksum);

        // 8. 构建完整帧 (添加转译和头尾)
        return buildFullFrame(buf);
    }

    /**
     * 转译并添加首尾字节
     * 
     * @param content
     * @return
     */
    private static byte[] buildFullFrame(ByteBuffer content) {
        // 创建转译后的缓冲区
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {

            // 添加帧头
            out.write(GaoYouConstants.FRAME_DELIMITER);

            // 转译内容
            for (int i = 0; i < content.capacity(); i++) {
                byte b = content.get(i);
                if (b == GaoYouConstants.FRAME_DELIMITER) {
                    out.write(GaoYouConstants.ESCAPE_CHAR);
                    out.write(0x01);
                } else if (b == GaoYouConstants.ESCAPE_CHAR) {
                    out.write(GaoYouConstants.ESCAPE_CHAR);
                    out.write(0x02);
                } else {
                    out.write(b);
                }
            }
            // 添加帧尾
            out.write(GaoYouConstants.FRAME_DELIMITER);
            byte[] buf = out.toByteArray();

            return buf;
            
        } finally {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * 构建设备搜索命令
     * 
     * @param networkId 网络标识 (2字节)
     * @return 协议数据包
     */
    public static byte[] buildSearchControllerCommand(int serial, byte[] networkId) {
        if (networkId == null || networkId.length != 2) {
            throw new IllegalArgumentException("Network ID must be 2 bytes");
        }

        String deviceSn = "0000000000000000";
        return buildCommand(
                deviceSn,
                GaoYouConstants.DEFAULT_PASSWORD,
                serial,
                (byte) 0x01,
                (byte) 0xFE,
                (byte) 0x00,
                networkId);
    }

    /**
     * 读取门禁控制器TCP参数
     * 
     * @param deviceSn
     * @return
     */
    public static byte[] buildGetControllerIP(String deviceSn,  String password, int serial) {
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x01,
                (byte) 0x06,
                (byte) 0x00,
                null);
    }

    /**
     * 设置控制器TCP参数
     * 设置前先读取，获取原配置信息数组，将该数组作为origSetting参数传入
     * 
     * @param deviceSn
     * @param password
     * @param origSetting 读取的IP信息数据
     * @param ipAddr      IP地址
     * @param mask        子网掩码
     * @param gateway     网关
     * @return
     * @throws UnknownHostException
     */
    public static byte[] buildSetControllerIP(String deviceSn, String password,
            int serial, byte[] origSetting, String ipAddr,
            String mask,
            String gateway) throws UnknownHostException {

        // 拷贝设置
        byte[] newSetting = new byte[137];
        System.arraycopy(origSetting, 0, newSetting, 0, 137);

        // ip
        InetAddress inet = InetAddress.getByName(ipAddr);
        byte[] bytes = inet.getAddress(); // 返回4字节的网络字节序数组, 大端序
        System.arraycopy(bytes, 0, newSetting, 6, 4);

        // mask
        inet = InetAddress.getByName(mask);
        bytes = inet.getAddress();
        System.arraycopy(bytes, 0, newSetting, 10, 4);

        // gateway
        inet = InetAddress.getByName(gateway);
        bytes = inet.getAddress();
        System.arraycopy(bytes, 0, newSetting, 14, 4);

        // 构造数据包
        return buildCommand(
                deviceSn,
                GaoYouConstants.DEFAULT_PASSWORD,
                serial,
                (byte) 0x01,
                (byte) 0x06,
                (byte) 0x01,
                newSetting);
    }

    /**
     * 构建设置实时监控命令
     * 
     * @param deviceSn 设备SN
     * @param password 通讯密码
     * @param enable   是否启用监控 true开启 false关闭
     * @return 协议数据包
     */
    public static byte[] buildSetRealTimeMonitorCommand(
            String deviceSn,
            String password,
            int serial,
            boolean enable) {
        byte parameter;
        parameter = enable ? (byte) 0x00 : (byte) 0x01;

        // 实时监控命令没有数据内容
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x01,
                (byte) 0x0B,
                parameter,
                null);
    }

    /**
     * 构建获取监控状态命令
     * 
     * @param deviceSn 设备SN
     * @param password 通讯密码
     * @return 协议数据包
     */
    public static byte[] buildGetMonitorStatusCommand(String deviceSn, String password, int serial) {
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x01,
                (byte) 0x0B,
                (byte) 0x02,
                null);
    }

    /**
     * 构建获取门状态命令
     * 
     * @param deviceSn 设备SN
     * @param password 通讯密码
     * @return 协议数据包
     */
    public static byte[] buildGetDoorStatusCommand(String deviceSn, String password, int serial) {
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x01,
                (byte) 0x0E,
                (byte) 0x00,
                null);
    }

    /**
     * 构建远程开关门命令
     * 
     * @param deviceSn
     * @param password
     * @param doorOperation
     * @param doorPorts
     * @param
     * @param
     * @return
     */
    public static byte[] buildDoorControlCommand(
            String deviceSn,
            String password,
            int serial,
            byte doorOperation,
            byte[] doorPorts) {
        byte[] dataContent = doorPorts;
        byte actualOperation = doorOperation;

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x03,
                (byte) 0x03,
                actualOperation,
                dataContent);
    }

    /**
     * 构建设置控制器时间命令
     * 
     * @param deviceSn
     * @param password
     * @param dateTime
     * @param broadcast
     * @return
     */
    public static byte[] buildSetTimeCommand(
            String deviceSn,
            String password,
            int serial,
            LocalDateTime dateTime,
            boolean broadcast) {
        // 时间数据 (7字节: 秒分时日月周年)
        byte[] timeData = new byte[7];
        timeData[0] = bcdEncode(dateTime.getSecond());
        timeData[1] = bcdEncode(dateTime.getMinute());
        timeData[2] = bcdEncode(dateTime.getHour());
        timeData[3] = bcdEncode(dateTime.getDayOfMonth());
        timeData[4] = bcdEncode(dateTime.getMonthValue());

        // 周: 1=周一...7=周日
        int dayOfWeek = dateTime.getDayOfWeek().getValue() % 7 + 1;
        timeData[5] = bcdEncode(dayOfWeek);

        // 年: 取后两位 (2000-2099)
        timeData[6] = bcdEncode(dateTime.getYear() % 100);

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x02,
                (byte) 0x02,
                broadcast ? (byte) 0x01 : (byte) 0x00,
                timeData);
    }

    /**
     * 构建读取设备SN命令
     * 
     * @param deviceSn
     * @param password
     * @return
     */
    public static byte[] buildReadSnCommand(String deviceSn, String password, int serial) {
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x01,
                (byte) 0x02,
                (byte) 0x00,
                null);
    }

    /**
     * 读取记录指针
     * 
     * @param deviceSn
     * @param password
     * @return
     */
    public static byte[] buildGetRecordIndexCommand(
            String deviceSn,
            String password,
            int serial) {

        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x08,
                (byte) 0x01,
                (byte) 0x00,
                null);
    }

    /**
     * 更新记录指针
     * 
     * @param deviceSn
     * @param password
     * @param recType
     * @param recordIndex
     * @param loopFlag
     * @return
     */
    public static byte[] buildUpdateRecordIndexCommand(
            String deviceSn,
            String password,
            int serial,
            int recType,
            int recordIndex,
            int loopFlag) {

        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序

        buf.put((byte) (recType & 0xFF));
        buf.putInt(recordIndex);
        buf.put((byte) (loopFlag & 0xFF));

        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x08,
                (byte) 0x03,
                (byte) 0x00,
                buf.array());
    }

    /**
     * 读取记录
     * 
     * @param deviceSn    设备序号
     * @param password    密码
     * @param recType     记录类型
     * @param recordIndex 记录指针
     * @param count       读取记录数量
     * @return
     */
    public static byte[] buildGetRecordCommand(
            String deviceSn,
            String password,
            int serial,
            int recType,
            int recordIndex,
            int count) {

        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序

        buf.put((byte) (recType & 0xFF));
        buf.putInt(recordIndex);
        buf.putInt(count);

        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x08,
                (byte) 0x04,
                (byte) 0x00,
                buf.array());
    }

    /**
     * 检查是否有新的记录
     * 
     * @param packet 记录指针响应数据包
     * @param dri    存储记录信息，用于读取记录
     * @return
     */
    public static boolean haveNewRecord(byte[] packet, GaoYouDoorRecordInfo dri) {

        int[] recordCount = new int[6]; // 记录内容
        int[] recordTail = new int[6]; // 记录尾号
        int[] recordHead = new int[6]; // 上传断点
        int[] loopFlag = new int[6]; // 循环标志

        for (int i = 0; i < 6; i++) {
            recordCount[i] = Utils.getIntFromBuf(packet, 32 + 13 * i); // 4b
            recordTail[i] = Utils.getIntFromBuf(packet, 32 + 13 * i + 4); // 4b
            recordHead[i] = Utils.getIntFromBuf(packet, 32 + 13 * i + 8); // 4b
            loopFlag[i] = packet[32 + 13 * i + 12]; // 1b
        }

        // 不检查报警记录和系统记录
        // 1读卡记录 2出门开关 3门磁 4远程开门 5报警 6系统记录
        for (int i = 0; i < 4; i++) {
            if (recordHead[i] != recordTail[i]) {
                dri.recType = (byte) (i + 1);
                dri.recIndex = recordHead[i] + 1;
                dri.loopFlag = loopFlag[i];
                if (dri.recIndex >= recordCount[i])
                    dri.recIndex = 0;
                return true;
            }
        }
        return false;
    }

    /**
     * 权限添加与修改 -> 添加授权卡至非排序卡区域
     * 
     * @param deviceSn
     * @param password
     * @param serial
     * @return
     */
    public static byte[] buildModifyOrAddUserPermissionCommand(
            String deviceSn,
            String password,
            int serial,
            byte[] data) {

        ByteBuffer buf = ByteBuffer.allocate(41);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序

        // 授权卡数量
        buf.putInt(1);

        // 授权卡信息 37字节
        buf.put(data);

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x07,
                (byte) 0x04,
                (byte) 0x00,
                buf.array());
    }

    /**
     * 删除单个用户权限
     * 
     * @param deviceSn 设备序号
     * @param password 密码
     * @param cardNo   卡号
     * @return
     */
    public static byte[] buildDeleteUserPermissionCommand(
            String deviceSn,
            String password,
            int serial,
            String cardNo) {

        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序

        // 授权卡数量
        buf.putInt(1);

        // 卡号，5字节
        byte[] carNoBuf = Utils.cardnoToInt5(cardNo);
        buf.put(carNoBuf);

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x07,
                (byte) 0x05,
                (byte) 0x00,
                buf.array());
    }

    /**
     * 权限清空
     * 
     * @param deviceSn
     * @param password
     * @return
     */
    public static byte[] buildDeleteAllUserPermissionCommand(
            String deviceSn,
            String password,
            int serial) {

        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序

        // 所有区域
        buf.put((byte) 0x03);

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x07,
                (byte) 0x02,
                (byte) 0x00,
                buf.array());
    }

    /**
     * 查询所有授权卡
     * - 非排序区
     * @param deviceSn
     * @param password
     * @param serial
     * @return
     */
    public static byte[] buildGetAllUserPermissionCommand(
            String deviceSn,
            String password,
            int serial) {

        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序
        // 非排序区
        buf.put((byte) 0x02);

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x07,
                (byte) 0x03,
                (byte) 0x00,
                buf.array());

    }

    /**
     * 权限查询
     * 
     * @param deviceSn
     * @param password
     * @param cardNo   卡号
     * @return
     */
    public static byte[] buildGetUserPermissionCommand(
            String deviceSn,
            String password,
            int serial,
            String cardNo) {

        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序

        // 卡号
        byte[] carNoBuf = Utils.cardnoToInt5(cardNo);
        buf.put(carNoBuf);

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x07,
                (byte) 0x03,
                (byte) 0x01,
                buf.array());
    }

    /**
     * 设置控制器参数
     * 只能设置延迟时间 -> 开锁时输出时长
     * 
     * @param deviceSn
     * @param password
     * @param doorNum
     * @param delayTime
     * @return
     */
    public static byte[] buildSetControllerParaCommand(
            String deviceSn,
            String password,
            int serial,
            int doorNum,
            int delayTime) {

        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序

        // 3字节，门号及延迟时间
        buf.put((byte) doorNum);
        buf.put((byte) (delayTime / 256));
        buf.put((byte) (delayTime % 256));

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x03,
                (byte) 0x08,
                (byte) 0x01,
                buf.array());
    }

    /**
     * 读取门禁控制器参数
     * 
     * @param deviceSn
     * @param password
     * @param doorNum  门号
     * @return
     */
    public static byte[] buildGetControllerParaCommand(
            String deviceSn,
            String password,
            int serial,
            int doorNum) {

        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序

        // 门号
        buf.put((byte) doorNum);

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x03,
                (byte) 0x08,
                (byte) 0x00,
                buf.array());
    }

    /**
     * 解除门报警
     * 
     * @param deviceSn
     * @param password
     * @param doorNum
     * @return
     */
    public static byte[] buildResetAlarmCommand(
            String deviceSn,
            String password,
            int serial,
            int doorNum) {

        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.order(ByteOrder.BIG_ENDIAN);// 大端字节序

        // 3字节，解除门报警
        buf.put((byte) doorNum);
        buf.put((byte) 0xFF);
        buf.put((byte) 0xFF);

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x01,
                (byte) 0x0D,
                (byte) 0x00,
                buf.array());
    }

    /**
     * 构建OK响应包
     * 
     * @param deviceSn
     * @param password
     * @param doorNum
     * @return
     */
    public static byte[] buildOkRespCommand(
            String deviceSn,
            String password,
            int serial,
            int doorNum) {

        // 构建数据包
        return buildCommand(
                deviceSn,
                password,
                serial,
                (byte) 0x21,
                (byte) 0x01,
                (byte) 0x00,
                null);
    }

    // ====================== 辅助方法 ======================

    /**
     * 密码转字节
     * 
     * @param passwd
     * @return
     */
    private static byte[] procPasswd(String passwd) {
        String pwd = passwd;
        // 默认密码
        if (pwd.equals(GaoYouConstants.DEFAULT_PASSWORD)) {
            return new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
        }
        // 不足8位后面补F
        while (pwd.length() < GaoYouConstants.PASSWD_LEN) {
            pwd += "F";
        }
        // 超过8位取前8位
        if (pwd.length() > GaoYouConstants.PASSWD_LEN) {
            pwd.substring(0, GaoYouConstants.PASSWD_LEN);
        }
        // 转换为字节数组
        byte[] buf = new byte[4];
        for (int i = 0; i < 4; ++i) {
            buf[i] = (byte) (Integer.parseInt(pwd.substring(i * 2, i * 2 + 2), 16) & 0xFF);
        }
        return buf;
    }

    /**
     * BCD编码
     * 
     * @param value
     * @return
     */
    public static byte bcdEncode(int value) {
        return (byte) (((value / 10) << 4) | (value % 10));
    }
}

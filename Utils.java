package com.bro.binterface.door.utils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {

    /**
     * 算术和
     * 
     * @param buf
     * @return
     */
    public static byte calculateChecksum(ByteBuffer buf) {
        int sum = 0;
        for (int i = 0; i < buf.capacity(); i++) {
            sum += buf.get(i) & 0xFF;
        }
        return (byte) (sum & 0xFF);
    }

    /**
     * 将BCD字节数组转换为字符串
     * 
     * @param bcd        BCD字节数组
     * @param startIndex 起始索引位置
     * @param bcdLen     要转换的字节长度
     * @return 转换后的字符串，如果输入无效返回空字符串
     */
    public static String bcdToString(byte[] bcd, int startIndex, int bcdLen) {
        // 参数校验
        if (bcd == null || startIndex < 0 || bcdLen < 0 || startIndex + bcdLen > bcd.length) {
            return "";
        }

        StringBuilder sb = new StringBuilder(bcdLen * 2);

        for (int i = startIndex; i < startIndex + bcdLen; i++) {
            // 处理高4位
            int high = (bcd[i] >> 4) & 0x0F;
            if (high > 9)
                break;
            sb.append((char) ('0' + high));

            // 处理低4位
            int low = bcd[i] & 0x0F;
            if (low > 9)
                break;
            sb.append((char) ('0' + low));
        }

        return sb.toString();
    }

    /**
     * 将字符串转换为BCD字节数组
     * 
     * @param str
     * @return
     */
    public static byte[] stringToBcd(String str) {
        if (str == null)
            return new byte[0];

        // 过滤非数字字符
        String digits = str.replaceAll("\\D", "");
        int bcdLength = (digits.length() + 1) / 2;
        byte[] bcd = new byte[bcdLength];

        // 初始化BCD数组为全0xFF
        for (int i = 0; i < bcdLength; i++) {
            bcd[i] = (byte) 0xFF;
        }

        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            int digit = c - '0';
            int byteIndex = i / 2;

            if (i % 2 == 0) {
                // 偶数位置：设置高4位
                bcd[byteIndex] = (byte) ((bcd[byteIndex] & 0x0F) | (digit << 4));
            } else {
                // 奇数位置：设置低4位
                bcd[byteIndex] = (byte) ((bcd[byteIndex] & 0xF0) | digit);
            }
        }
        return bcd;
    }

    /**
     * 将字符串转换为4字节BCD码，不足8位时低位补0xF
     * @param str 密码字符串（仅数字，最多8位）
     * @return 4字节BCD数组
     */
    public static byte[] passwordToBcd4Bytes(String str) {
        if (str == null || str.length() > 8 || !str.matches("\\d+")) {
            throw new IllegalArgumentException("密码必须是1-8位数字");
        }

        byte[] bcd = new byte[4];
        Arrays.fill(bcd, (byte) 0xFF); // 初始化为全0xFF

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int digit = c - '0';
            int byteIndex = i / 2; // 每字节存2位数字

            if (i % 2 == 0) {
                // 高4位
                bcd[byteIndex] = (byte) ((digit << 4) | 0x0F);
            } else {
                // 低4位
                bcd[byteIndex] = (byte) ((bcd[byteIndex] & 0xF0) | digit);
            }
        }

        return bcd;
    }

    /**
     * 将4字节BCD码转换回密码字符串，自动去除补位的0xF
     * @param bcd 4字节BCD数组
     * @return 密码字符串（去掉末尾的F）
     */
    public static String bcd4BytesToPassword(byte[] bcd) {
        if (bcd == null || bcd.length != 4) {
            throw new IllegalArgumentException("BCD数组必须为4字节");
        }

        StringBuilder sb = new StringBuilder(8); // 最多8位数字

        for (int i = 0; i < 4; i++) {
            byte currentByte = bcd[i];

            // 处理高4位
            int high = (currentByte >> 4) & 0x0F;
            if (high <= 9) {
                sb.append(high);
            } else {
                break; // 遇到0xF，停止解析
            }

            // 处理低4位
            int low = currentByte & 0x0F;
            if (low <= 9) {
                sb.append(low);
            } else {
                break; // 遇到0xF，停止解析
            }
        }

        return sb.toString();
    }

    /**
     * 从缓冲区获取整形, 按大端处理
     * 
     * @param buf
     * @param startIndex
     * @return
     */
    public static int getIntFromBuf(byte[] buf, int startIndex) {
        return ((int) (buf[startIndex + 0] & 0xFF) << 24)
                | ((int) (buf[startIndex + 1] & 0xFF) << 16)
                | ((int) (buf[startIndex + 2] & 0xFF) << 8)
                | ((int) (buf[startIndex + 3] & 0xFF));
    }

    /**
     * 将卡号字符串转换为9字节数组（高位补0）
     * @param cardNo 卡号字符串（最长支持9位数字）
     * @return 9字节数组（大端序，高位补0）
     */
    public static byte[] cardnoToInt9(String cardNo) {
        // 参数校验
        if (cardNo == null || cardNo.length() == 0 || cardNo.length() > 9) {
            throw new IllegalArgumentException("卡号长度必须为1-9位数字");
        }
        // 检查是否为纯数字
        if (!cardNo.matches("\\d+")) {
            throw new IllegalArgumentException("卡号必须为数字");
        }
        byte[] buf = new byte[9];
        try {
            // 将字符串转换为数值（最大9位数字）
            long num = Long.parseLong(cardNo);

            // 大端序存储到后8字节（第1字节保持为0）
            for (int i = 0; i < 8; i++) {
                buf[8 - i] = (byte) (num & 0xFF); // 取最低字节
                num >>= 8; // 右移8位
            }
            // 第1字节(buf[0])保持为0

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("卡号数值太大，超过处理范围");
        }

        return buf;
    }

    /**
     * 从9字节数组解析卡号（忽略最高位字节）
     * @param buf 字节数组（长度至少为startIndex+9）
     * @param startIndex 起始位置
     * @return 卡号字符串
     */
    public static String int9ToCardno(byte[] buf, int startIndex) {
        // 参数校验
        if (buf == null || buf.length < startIndex + 9) {
            throw new IllegalArgumentException("字节数组长度不足或起始位置无效");
        }

        // 从后8字节重建数值（忽略第1字节）
        long num = 0;
        for (int i = 0; i < 8; i++) {
            num = (num << 8) | (buf[startIndex + 1 + i] & 0xFF);
        }

        return Long.toString(num);
    }

    /**
     * 将卡号字符串转换为5字节的大端序表示
     * 
     * @param cardNo
     * @return
     */
    public static byte[] cardnoToInt5(String cardNo) {

        byte[] buf = new byte[5];

        // 将字符串转换为long
        long num = Long.parseLong(cardNo);

        // 提取低40位（5字节）
        long mask = 0xFFFFFFFFFFL; // 40位掩码
        num &= mask;

        // 以大端序存储到5字节数组
        buf[0] = (byte) ((num >> 32) & 0xFF); // 最高位字节
        buf[1] = (byte) ((num >> 24) & 0xFF);
        buf[2] = (byte) ((num >> 16) & 0xFF);
        buf[3] = (byte) ((num >> 8) & 0xFF);
        buf[4] = (byte) (num & 0xFF); // 最低位字节

        return buf;
    }

    /**
     * 将5字节的大端序表示转换回卡号字符串
     * 
     * @param buf
     * @return
     */
    public static String int5ToCardno(byte[] buf, int startIndex) {

        // 从大端序字节数组重建long值
        long num = ((long) (buf[startIndex + 0] & 0xFF) << 32)
                | ((long) (buf[startIndex + 1] & 0xFF) << 24)
                | ((long) (buf[startIndex + 2] & 0xFF) << 16)
                | ((long) (buf[startIndex + 3] & 0xFF) << 8)
                | (buf[startIndex + 4] & 0xFF);

        // 将long转换为字符串
        return Long.toString(num);
    }

    /**
     * 数组转十六进制串
     * 
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public static String bytesToHex(byte[] bytes, int startIndex, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < startIndex + len; ++i) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    /**
     * 信息代码
     * 
     * @return
     */
    public static byte[] generateInfoCode() {
        byte[] infoCode = new byte[4];
        ThreadLocalRandom.current().nextBytes(infoCode);
        return infoCode;
    }

    /**
     * 字节数组转short (大端序)
     */
    public static short bytesToShort(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            throw new IllegalArgumentException("字节数组长度不足");
        }
        return (short) ((bytes[0] << 8) | (bytes[1] & 0xFF));
    }

    public static void main(String[] args) {
//        String c = "19137621";
//        byte[] a = cardnoToInt9(c);
//        System.out.println(bytesToHex(a));
//
//        byte[] b = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x24, 0x04, 0x55};
//        String d = int9ToCardno(b, 0);
//        System.out.println(d);

//        byte[] card = new byte[]{(byte) 0x88, 0x12, 0x30, 0x23, 0x59};
//        String yearStr = "20" + Utils.bcdToString(card, 0, 1);
//        String monthStr = Utils.bcdToString(card, 1, 1);
//        String dayStr = Utils.bcdToString(card, 2, 1);
//
//        System.out.println(String.format("%s-%s-%s",yearStr, monthStr, dayStr));

//        List<String> enabledDoors = new ArrayList<>();
//        byte enable = 0x10;
//        byte doorEnable = (byte)((enable >> 4) & 0x0F);
//        if ((doorEnable & 0x01) != 0) enabledDoors.add("门1");
//        if ((doorEnable & 0x02) != 0) enabledDoors.add("门2");
//        if ((doorEnable & 0x04) != 0) enabledDoors.add("门3");
//        if ((doorEnable & 0x08) != 0) enabledDoors.add("门4");
//        System.out.println(String.join(",", enabledDoors));

//        String doorStr = "1,4";
//        String[] doors = doorStr.split("\\s*,\\s*"); // 处理可能存在的空格
//        byte result = 0;
//        for (String door : doors) {
//            try {
//                int doorNum = Integer.parseInt(door);
//                if (doorNum >= 1 && doorNum <= 4) { // 假设最多8个门
//                    result |= (1 << (doorNum - 1)); // 设置对应位
//                }
//            } catch (NumberFormatException e) {
//                // 忽略非数字项
//            }
//        }
//        System.out.println(Integer.toHexString(result & 0xFF));

//        byte[] a = "2028-06-25".replaceAll("-", "").substring(2).getBytes();
//        System.out.println("2028-06-25".replaceAll("-", "").substring(2));
//        System.out.println(a[0] + "-" + a[1] + "-" + a[2]);

//        System.out.println(Arrays.toString(Utils.stringToBcd("1234")));
//        System.out.println(bytesToHex(Utils.stringToBcd("1234")));

//        byte[] a = new byte[] { 0x11, 0x12};
//        int b = a[0];
//        System.out.println(b);

//        String tempStr = String.format("%s2359", "881205");
//        byte[] time = Utils.stringToBcd(tempStr);
//        String yearStr = "20" + Utils.bcdToString(time, 0, 1);
//        String monthStr = Utils.bcdToString(time, 1, 1);
//        String dayStr = Utils.bcdToString(time, 2, 1);
//        System.out.println(String.format("%s-%s-%s", yearStr, monthStr, dayStr));

//        String passwd = "12345";
//        byte[] passwdByte = Utils.stringToBcd(passwd);
//        System.out.println(bytesToHex(passwdByte));

    }

}

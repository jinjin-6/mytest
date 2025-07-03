package com.bro.binterface.door.tcp.gaoyou.constants;

public final class GaoYouConstants {

    public static final byte PACKET_MIN_LEN = 34;
    public static final byte PASSWD_LEN = 8;

    // 协议常量
    public static final byte FRAME_DELIMITER = 0x7E;
    public static final byte ESCAPE_CHAR = 0x7F;
    public static final String DEFAULT_PASSWORD = "FFFFFFFF";
}

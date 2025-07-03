package com.bro.binterface.door.udp.domain;

public enum TransparentCommand {
    TRANSPARENT_TRANSMISSION(0x0001, "透传串口数据"),
    HEARTBEAT(0x0002, "FSU透传通道心跳");

    private final int code;
    private final String description;

    TransparentCommand(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TransparentCommand fromCode(int code) {
        for (TransparentCommand cmd : values()) {
            if (cmd.getCode() == code) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("未知命令类型: 0x" + Integer.toHexString(code));
    }
}

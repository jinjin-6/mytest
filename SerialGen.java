package com.bro.binterface.door.utils;

/**
 * 序列号
 */
public class SerialGen {

    private static int serial = 0;
    private static final Object lock = new Object(); // 锁对象

    /**
     * 生成序列号
     * 
     * @return
     */
    public static int genPacketSerial() {
        synchronized (lock) {
            serial++;
            if (serial == 0xFFFFFFFF) {
                serial = 0x0;
            }
            return serial;
        }
    }
}

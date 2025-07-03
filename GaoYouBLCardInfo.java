package com.bro.binterface.door.udp.gaoyoubl.mesaage;

import lombok.Data;

/**
 * 授权卡信息
 */
@Data
public class GaoYouBLCardInfo {
    private byte[] cardNo = new byte[5]; // 卡号(5字节)
    private byte[] passwd = new byte[4]; // 密码(4字节)
    private byte[] endDay = new byte[5]; // 有效期(5字节)
    private byte[] openTime = new byte[4];   // 开门时段 (4字节)
    private byte[] validTimes = new byte[2]; // 有效次数 (2字节)
    private byte doorEnable;                 // 权限 (低4位)
    private byte privilege;                  // 特权 (高4位)
    private byte status;                     // 状态 (1字节)
    private byte[] holidays = new byte[4];   // 节假日 (4字节)
    private byte inOutFlag;                  // 出入标志 (1字节)
    private byte[] lastReadTime = new byte[6]; // 最近读卡时间 (6字节)
    public GaoYouBLCardInfo(byte[] cardData) {
        if (cardData == null || cardData.length != 0x21) {
            throw new IllegalArgumentException("卡数据长度必须为37字节(0x25)");
        }
        int pos = 0;
        // 1. 卡号 (5字节)
        System.arraycopy(cardData, pos, cardNo, 0, 5);
        pos += 5;
        // 2. 密码 (4字节)
        System.arraycopy(cardData, pos, passwd, 0, 4);
        pos += 4;
        // 3. 有效期 (5字节)
        System.arraycopy(cardData, pos, endDay, 0, 5);
        pos += 5;
        // 4. 开门时段 (4字节)
        System.arraycopy(cardData, pos, openTime, 0, 4);
        pos += 4;
        // 5. 有效次数 (2字节)
        System.arraycopy(cardData, pos, validTimes, 0, 2);
        pos += 2;
        // 6. 权限和特权 (共1字节，权限占高4位，特权占低4位)
        byte enableAndPrivilege = cardData[pos++];
        this.doorEnable = (byte)((enableAndPrivilege >> 4) & 0x0F); // 高4位是权限
        this.privilege = (byte)(enableAndPrivilege & 0x0F);  // 低4位是特权

        // 7. 状态 (1字节)
        this.status = cardData[pos++];
        // 8. 节假日 (4字节)
        System.arraycopy(cardData, pos, holidays, 0, 4);
        pos += 4;
        // 9. 出入标志 (1字节)
        this.inOutFlag = cardData[pos++];
        // 10. 最近读卡时间 (6字节)
        System.arraycopy(cardData, pos, lastReadTime, 0, 6);
    }

}

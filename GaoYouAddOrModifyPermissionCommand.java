package com.bro.binterface.door.tcp.gaoyou.cmd.impl;

import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.tcp.gaoyou.GaoYouAccessDoor;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolUtils;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandleManager;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandler;
import com.bro.binterface.door.utils.SerialGen;
import com.bro.binterface.door.utils.Utils;
import com.bro.common.core.constant.Constants;
import com.bro.common.core.constant.DoorAccessDeviceCmdCode;
import com.bro.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

/**
 * 新增授权卡
 * - 每次新增/修改一个
 */
@Slf4j
@Component
public class GaoYouAddOrModifyPermissionCommand implements GaoYouCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouAccessDoor device, CmdParam param) {
        if (param.getParams().get("cardNo") == null) {
            return R.fail("缺少参数cardNo");
        }
        String cardNo = String.valueOf(param.getParams().get("cardNo")); // 卡号

        if (param.getParams().get("endDay") == null) {
            return R.fail("缺少参数endDay，格式：年-月-日");
        }
        String endDayStr = String.valueOf(param.getParams().get("endDay"));
        String endDay = endDayStr.replaceAll("-", "").substring(2); // 有效期 格式：年月日

        String passwd = String.valueOf(param.getParams().get("passwd")); // 密码
//        if (StringUtils.isEmpty(passwd)) {
//            return R.fail("缺少参数passwd");
//        }
        Object doorEnableObj = param.getParams().get("doorEnable"); // 格式：1,2,3,4 表示四个门都有权限
        if (doorEnableObj == null) {
            return R.fail("缺少参数doorEnable，格式：1,2,3,4");
        }
        byte doorEnable = parseDoorEnable(doorEnableObj);;

        ByteBuffer buffer = ByteBuffer.allocate(37);
        buffer.order(ByteOrder.BIG_ENDIAN);// 大端字节序
        // 卡号（9b）
        byte[] carNoBuf = Utils.cardnoToInt9(cardNo);
        buffer.put(carNoBuf);
        // 密码(4b)
        byte[] bcdBuf = Utils.passwordToBcd4Bytes(passwd);
        buffer.put(bcdBuf);
        // 有效期（5B)
        String tempStr = String.format("%s2359", endDay);
        bcdBuf = Utils.stringToBcd(tempStr);
        System.out.println(Utils.bytesToHex(bcdBuf));
        buffer.put(bcdBuf);
        // 开门时段, 4字节，全0
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        // 有效次数, FFFF: 不受限制
        buffer.put((byte) 0xFF);
        buffer.put((byte) 0xFF);
        // 权限：每一位代表一个门，从右至左，依次是1、2、3、4门。例如0011表示第一门和第二门有效,bit4开始
        buffer.put(doorEnable);
        // 状态：0：正常状态；1：挂失；2：黑名单
        buffer.put((byte) 0x00);
        // 节假日,出入标志,最近读卡时间 全默认为 0xFF
        for (int i = 0; i < 11; ++i) {
            buffer.put((byte) 0xFF);
        }

        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBMProtocolUtils.buildModifyOrAddUserPermissionCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial,
                buffer.array());
        log.debug("send hex：" + Utils.bytesToHex(command));
        R resp = device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        byte[] data = (byte[]) resp.getData();
        if (data[25] == 0x21) {
            return R.ok("添加授权卡成功");
        } else {
            return R.fail("添加授权卡失败");
        }
    }

    /**
     * 解析门权限数组
     * @param doorEnable 门权限参数（如 [1,0,1,0] 表示第1、3门有权限）
     */
    private byte parseDoorEnable(Object doorEnable) {
        if (doorEnable instanceof String) {
            // 格式--四个门权限都有：1，2，3，4；只有一个门：1
            String doorStr = (String) doorEnable;
            String[] doors = doorStr.split("\\s*,\\s*"); // 处理可能存在的空格
            byte result = 0;
            for (String door : doors) {
                try {
                    int doorNum = Integer.parseInt(door);
                    if (doorNum >= 1 && doorNum <= 4) { // 假设最多8个门
                        result |= (1 << (doorNum - 1)); // 设置对应位
                    }
                } catch (NumberFormatException e) {
                    // 忽略非数字项
                }
            }
            return result;
        }
        return 0x0F; // 默认返回 1111
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GaoYouCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_ADD_USER_PERMISSION, this);
    }
}

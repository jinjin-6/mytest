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

import java.util.concurrent.TimeUnit;

/**
 * 高优门禁：开/关门
 */
@Slf4j
@Component
public class GaoYouDoorOpenCommand implements GaoYouCmdHandler, InitializingBean {

    @Override
    public R exec(GaoYouAccessDoor device, CmdParam param) {
        // 门号：[1 2 3 4]
        if (param.getParams().get("doorNum") == null) {
            return R.fail("缺少参数doorNum");
        }
        int doorNum = Integer.parseInt(String.valueOf(param.getParams().get("doorNum")));
        // 操作：0开门 1关门
        int doorOperation = Integer.parseInt(String.valueOf(param.getParams().get("doorOperation")));

        byte[] doorPorts = new byte[4];
        if (doorNum >= 1 && doorNum <= 4) {
            doorPorts[doorNum - 1] = 0x01;
        }

        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBMProtocolUtils.buildDoorControlCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial,
                (byte) doorOperation,
                doorPorts);
        log.debug("send hex：" + Utils.bytesToHex(command));
        R resp = device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        byte[] data = (byte[]) resp.getData();
        if (data[25] == 0x21) {
            return R.ok("操作成功");
        } else {
            return R.fail("操作失败");
        }
    }

    /**
     * Spring组件实例化后事件
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 注册
        GaoYouCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_OPEN_AND_CLOSE_DOOR, this);
    }
}

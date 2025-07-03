package com.bro.binterface.door.tcp.gaoyou.cmd.impl;

import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.tcp.gaoyou.GaoYouAccessDoor;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolParser;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolUtils;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandleManager;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandler;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouDoorStatus;
import com.bro.binterface.door.utils.SerialGen;
import com.bro.binterface.door.utils.Utils;
import com.bro.common.core.constant.Constants;
import com.bro.common.core.constant.DoorAccessDeviceCmdCode;
import com.bro.common.core.constant.DoorConstants;
import com.bro.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 高优门禁：门状态
 */
@Slf4j
@Component
public class GaoYouDoorStatusCommand implements GaoYouCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouAccessDoor device, CmdParam param) {
        if (param.getParams().get("doorNum") == null) {
            return R.fail("缺少参数doorNum");
        }
        // [1, 2, 3, 4]
        String doorNumStr = String.valueOf(param.getParams().get("doorNum"));
        // "null"判断
        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBMProtocolUtils.buildGetDoorStatusCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial);
        log.debug("send hex：" + Utils.bytesToHex(command));
        R resp = device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        // 解析响应
        byte[] data = (byte[]) resp.getData();
        GaoYouDoorStatus gaoYouDoorStatus = GaoYouBMProtocolParser.parseDoorStatusResponse(data);
        int doorNum = Integer.parseInt(doorNumStr);
        int doorStatus = gaoYouDoorStatus.doorStatus(doorNum);
        if (doorStatus == 1) {
            return R.ok(DoorConstants.COMMAND_OPEN_DOOR);
        }
        return R.ok(DoorConstants.COMMAND_CLOSE_DOOR);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GaoYouCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_DOOR_STATUS, this);
    }
}

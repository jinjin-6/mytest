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
 * 清空所有授权卡
 * - 所有区域的授权卡
 */
@Slf4j
@Component
public class GaoYouDeleteAllUserPermissionCommand implements GaoYouCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouAccessDoor device, CmdParam param) {
        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBMProtocolUtils.buildDeleteAllUserPermissionCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial);
        log.info("send hex：" + Utils.bytesToHex(command));
        R resp = device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        byte[] data = (byte[]) resp.getData();
        if (data[25] == 0x21) {
            return R.ok("删除所有授权卡成功");
        } else {
            return R.fail("删除所有授权卡失败");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GaoYouCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_GET_DELETE_ALL_USER_PERMISSION, this);
    }
}

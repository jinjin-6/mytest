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
import com.bro.common.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 删除授权卡
 * - 每次删除一个
 */
@Slf4j
@Component
public class GaoYouDeleteUserPermissionCommand implements GaoYouCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouAccessDoor device, CmdParam param) {
        String cardNo = String.valueOf(param.getParams().get("cardNo")); // 卡号
        if (StringUtils.isEmpty(cardNo)) {
            return R.fail("缺少参数cardNo");
        }
        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBMProtocolUtils.buildDeleteUserPermissionCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial,
                cardNo);
        log.info("send hex：" + Utils.bytesToHex(command));
        R resp = device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        byte[] data = (byte[]) resp.getData();
        if (data[25] == 0x21) {
            return R.ok("删除授权卡成功");
        } else {
            return R.fail("删除授权卡失败");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GaoYouCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_DELETE_USER_PERMISSION, this);
    }
}

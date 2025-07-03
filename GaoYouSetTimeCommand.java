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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 写入设备时间（非广播）
 */
@Slf4j
@Component
public class GaoYouSetTimeCommand implements GaoYouCmdHandler, InitializingBean {

    @Override
    public R exec(GaoYouAccessDoor device, CmdParam param) {
        if (param.getParams().get("dateTime") == null) {
            return R.fail("缺少参数dateTime");
        }
        String dateTime = param.getParams().get("dateTime").toString();

        int serial = SerialGen.genPacketSerial();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime ldt = LocalDateTime.parse(dateTime, formatter);

        byte[] command = GaoYouBMProtocolUtils.buildSetTimeCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial,
                ldt,
                false);
        log.debug("send hex：" + Utils.bytesToHex(command));
        R resp = device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        byte[] data = (byte[]) resp.getData();
        if (data[25] == 0x21) {
            return R.ok("设置成功");
        } else {
            return R.fail("设置失败");
        }
    }

    /**
     * Spring组件实例化后事件
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 注册
        GaoYouCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_SET_TIME, this);
    }
}

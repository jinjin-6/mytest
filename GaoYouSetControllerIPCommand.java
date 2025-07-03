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

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * 设置TCP参数
 */
@Slf4j
@Component
public class GaoYouSetControllerIPCommand implements GaoYouCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouAccessDoor device, CmdParam param) {
        if (param.getParams().get("ipAddress") == null) {
            return R.fail("缺少参数ipAddress");
        }
        String ipAddr = param.getParams().get("ipAddress").toString(); // IP地址

        if (param.getParams().get("subnetMask") == null) {
            return R.fail("缺少参数subnetMask");
        }
        String mask = param.getParams().get("subnetMask").toString(); // 子网掩码

        if (param.getParams().get("gateway") == null) {
            return R.fail("缺少参数gateway");
        }
        String gateway = param.getParams().get("gateway").toString(); // 网关

        int serial = SerialGen.genPacketSerial();
        // 获取TCP参数
        byte[] getCommand = GaoYouBMProtocolUtils.buildGetControllerIP(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial);
        log.debug("send hex：" + Utils.bytesToHex(getCommand));
        R getCommandResp = device.sendCommandWithResponse(serial, getCommand, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == getCommandResp.getCode()) {
            return getCommandResp;
        }
        byte[] getCommandData = (byte[]) getCommandResp.getData();
        byte[] origSetting = new byte[137];
        System.arraycopy(getCommandData, 32, origSetting, 0, 137);

        serial = SerialGen.genPacketSerial();
        try {
            byte[] command = GaoYouBMProtocolUtils.buildSetControllerIP(
                            device.getConfig().getDeviceSn(),
                            device.getConfig().getPasswd(),
                            serial,
                            origSetting,
                            ipAddr,
                            mask,
                            gateway
                            );
            log.debug("send hex：" + Utils.bytesToHex(command));
            R resp = device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
            if (Constants.FAIL == resp.getCode()) {
                return resp;
            }
            byte[] data = (byte[]) resp.getData();
            if (data[25] == 0x21) {
                return R.ok("设置TCP参数成功");
            } else {
                return R.fail("设置TCP参数失败");
            }
        } catch (UnknownHostException e) {
            log.error("设置TCP参数错误：{}", e.getMessage());
            return R.fail("设置TCP参数异常");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GaoYouCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_SET_CONTROLLER_IP, this);
    }
}

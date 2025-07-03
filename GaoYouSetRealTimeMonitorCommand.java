package com.bro.binterface.door.tcp.gaoyou.cmd.impl;

import com.bro.binterface.domain.ScDeviceDoorSignal;
import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.tcp.gaoyou.GaoYouAccessDoor;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolUtils;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandleManager;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandler;
import com.bro.binterface.door.utils.SerialGen;
import com.bro.binterface.door.utils.Utils;
import com.bro.binterface.service.IScDeviceDoorSignalService;
import com.bro.common.core.constant.Constants;
import com.bro.common.core.constant.DoorAccessDeviceCmdCode;
import com.bro.common.core.domain.R;
import com.bro.common.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 高优门禁：设置实时监控
 */
@Slf4j
@Component
public class GaoYouSetRealTimeMonitorCommand implements GaoYouCmdHandler, InitializingBean {
    @Resource
    private IScDeviceDoorSignalService deviceDoorSignalService;

    @Override
    public R exec(GaoYouAccessDoor device, CmdParam param) {
        if (param.getParams().get("enable") == null) {
            return R.fail("缺少参数enable");
        }
        // 是否启用监控 true开启 false关闭
        String enableStr = param.getParams().get("enable").toString();
        if (StringUtils.isEmpty(enableStr)) {
            return R.fail("缺少参数enable");
        }
        boolean enable = Boolean.parseBoolean(param.getParams().get("enable").toString());
        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBMProtocolUtils.buildSetRealTimeMonitorCommand(
                            device.getConfig().getDeviceSn(),
                            device.getConfig().getPasswd(),
                            serial,
                            enable);
        log.debug("send hex：" + Utils.bytesToHex(command));
        R resp = device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        byte[] data = (byte[]) resp.getData();
        if (data[25] == 0x21) {
            // 更改数据库监控状态
            ScDeviceDoorSignal scDeviceDoorSignal = new ScDeviceDoorSignal();
            scDeviceDoorSignal.setFsuId(device.getFsuId());
            scDeviceDoorSignal.setDeviceId(device.getDeviceId());
            scDeviceDoorSignal.setSignalId(DoorAccessDeviceCmdCode.GAOYOU_GET_REAL_TIME_MONITOR);
            // 0--未开启监控；1--已开启监控
            String flag = enable ? "1" : "0";
            scDeviceDoorSignal.setMeasuredValue(flag);
            deviceDoorSignalService.updateScDeviceDoorSignal(scDeviceDoorSignal);
            return R.ok("设置成功");
        } else {
            return R.fail("设置失败");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GaoYouCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_SET_REAL_TIME_MONITOR, this);
    }
}

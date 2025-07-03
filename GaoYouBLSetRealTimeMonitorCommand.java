package com.bro.binterface.door.udp.gaoyoubl.cmd.impl;

import com.bro.binterface.domain.ScDeviceDoorSignal;
import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.udp.domain.TransparentPacket;
import com.bro.binterface.door.udp.gaoyoubl.GaoYouBLAccessDevice;
import com.bro.binterface.door.udp.gaoyoubl.cmd.GaoYouBLCmdHandleManager;
import com.bro.binterface.door.udp.gaoyoubl.cmd.GaoYouBLCmdHandler;
import com.bro.binterface.door.udp.gaoyoubl.utils.GaoYouBLProtocolUtils;
import com.bro.binterface.door.udp.utils.TransparentPacketUtils;
import com.bro.binterface.door.udp.utils.UdpUtils;
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
public class GaoYouBLSetRealTimeMonitorCommand implements GaoYouBLCmdHandler, InitializingBean {
    @Resource
    private IScDeviceDoorSignalService deviceDoorSignalService;

    @Override
    public R exec(GaoYouBLAccessDevice device, CmdParam param) {
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
        byte[] command = GaoYouBLProtocolUtils.buildSetRealTimeMonitorCommand(
                            device.getConfig().getDeviceSn(),
                            device.getConfig().getPasswd(),
                            serial,
                            enable);
        log.debug("透传 send hex：" + Utils.bytesToHex(command));
        // 透传数据构造
        byte[] transparent = TransparentPacketUtils.buildTransparentPacket(
                command, device.getFsuId(),
                device.getConfig().getSerialPort(),
                device.getConfig().getVirtualNum());
        R resp = device.sendCommandWithResponse(serial, transparent, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        byte[] data = (byte[]) resp.getData();
        TransparentPacket packet = TransparentPacketUtils.parseTransparentPacket(data);
        // 校验
        boolean verifyFlag = UdpUtils.verifyResponse(packet, device);
        if (!verifyFlag) {
            log.warn("透传数据无法对应:{}", Utils.bytesToHex(data));
            return R.fail("返回透传数据校验失败");
        }
        byte[] transparentData = packet.getData();
        if (transparentData[25] == 0x21) {
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
        GaoYouBLCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_SET_REAL_TIME_MONITOR, this);
    }
}

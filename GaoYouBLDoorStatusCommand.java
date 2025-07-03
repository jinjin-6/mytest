package com.bro.binterface.door.udp.gaoyoubl.cmd.impl;

import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.udp.domain.TransparentPacket;
import com.bro.binterface.door.udp.gaoyoubl.GaoYouBLAccessDevice;
import com.bro.binterface.door.udp.gaoyoubl.cmd.GaoYouBLCmdHandleManager;
import com.bro.binterface.door.udp.gaoyoubl.cmd.GaoYouBLCmdHandler;
import com.bro.binterface.door.udp.gaoyoubl.mesaage.GaoYouBLDoorStatus;
import com.bro.binterface.door.udp.gaoyoubl.utils.GaoYouBLProtocolParser;
import com.bro.binterface.door.udp.gaoyoubl.utils.GaoYouBLProtocolUtils;
import com.bro.binterface.door.udp.utils.TransparentPacketUtils;
import com.bro.binterface.door.udp.utils.UdpUtils;
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
public class GaoYouBLDoorStatusCommand implements GaoYouBLCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouBLAccessDevice device, CmdParam param) {
        if (param.getParams().get("doorNum") == null) {
            return R.fail("缺少参数doorNum");
        }
        // [1, 2, 3, 4]
        String doorNumStr = String.valueOf(param.getParams().get("doorNum"));
        // "null"判断
        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBLProtocolUtils.buildGetDoorStatusCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial);

        log.debug("send hex：" + Utils.bytesToHex(command));

        // 透传数据构造
        byte[] transparent = TransparentPacketUtils.buildTransparentPacket(
                command, device.getFsuId(),
                device.getConfig().getSerialPort(),
                device.getConfig().getVirtualNum());

        log.debug("透传send hex：" + Utils.bytesToHex(transparent));

        R resp = device.sendCommandWithResponse(serial, transparent, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        // 解析响应
        byte[] data = (byte[]) resp.getData();
        TransparentPacket packet = TransparentPacketUtils.parseTransparentPacket(data);
        // 校验
        boolean verifyFlag = UdpUtils.verifyResponse(packet, device);
        if (!verifyFlag) {
            log.warn("透传数据无法对应:{}", Utils.bytesToHex(data));
            return R.fail("返回透传数据校验失败");
        }
        byte[] transparentData = packet.getData();
        GaoYouBLDoorStatus gaoYouDoorStatus = GaoYouBLProtocolParser.parseDoorStatusResponse(transparentData);
        int doorNum = Integer.parseInt(doorNumStr);
        int doorStatus = gaoYouDoorStatus.doorStatus(doorNum);
        if (doorStatus == 1) {
            return R.ok(DoorConstants.COMMAND_OPEN_DOOR);
        }
        return R.ok(DoorConstants.COMMAND_CLOSE_DOOR);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GaoYouBLCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_DOOR_STATUS, this);
    }
}

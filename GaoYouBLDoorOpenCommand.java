package com.bro.binterface.door.udp.gaoyoubl.cmd.impl;

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
import com.bro.common.core.constant.Constants;
import com.bro.common.core.constant.DoorAccessDeviceCmdCode;
import com.bro.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 高优门禁BL系列：开/关门
 */
@Slf4j
@Component
public class GaoYouBLDoorOpenCommand implements GaoYouBLCmdHandler, InitializingBean {

    @Override
    public R exec(GaoYouBLAccessDevice device, CmdParam param) {
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
        byte[] command = GaoYouBLProtocolUtils.buildDoorControlCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial,
                (byte) doorOperation,
                doorPorts);
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
        GaoYouBLCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_OPEN_AND_CLOSE_DOOR, this);
    }
}

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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 写入设备时间（非广播）
 */
@Slf4j
@Component
public class GaoYouBLSetTimeCommand implements GaoYouBLCmdHandler, InitializingBean {

    @Override
    public R exec(GaoYouBLAccessDevice device, CmdParam param) {
        if (param.getParams().get("dateTime") == null) {
            return R.fail("缺少参数dateTime");
        }
        String dateTime = param.getParams().get("dateTime").toString();

        int serial = SerialGen.genPacketSerial();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime ldt = LocalDateTime.parse(dateTime, formatter);

        byte[] command = GaoYouBLProtocolUtils.buildSetTimeCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial,
                ldt,
                false);
        log.debug("透传send hex：" + Utils.bytesToHex(command));
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
        GaoYouBLCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_SET_TIME, this);
    }
}

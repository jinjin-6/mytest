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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 读取设备SN
 */
@Slf4j
@Component
public class GaoYouBLReadSnCommand implements GaoYouBLCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouBLAccessDevice device, CmdParam param) {
        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBLProtocolUtils.buildReadSnCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial);
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
        if (transparentData.length < 50) {
            return R.fail("数据长度不足，无法解析");
        }
        if (transparentData[25] == 0x31 && transparentData[26] == 0x02 && transparentData[27] == 0x00) {
            byte[] snByte = new byte[16];
            System.arraycopy(transparentData, 32, snByte, 0, 16);
            String sn = new String(snByte, StandardCharsets.US_ASCII);
            return R.ok(sn);
        } else {
            return R.fail("数据响应格式错误");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GaoYouBLCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_GET_SN, this);
    }
}

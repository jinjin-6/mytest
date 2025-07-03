package com.bro.binterface.door.udp.gaoyoubl.cmd.impl;

import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.udp.domain.TransparentPacket;
import com.bro.binterface.door.udp.gaoyoubl.GaoYouBLAccessDevice;
import com.bro.binterface.door.udp.gaoyoubl.cmd.GaoYouBLCmdHandleManager;
import com.bro.binterface.door.udp.gaoyoubl.cmd.GaoYouBLCmdHandler;
import com.bro.binterface.door.udp.gaoyoubl.mesaage.GaoYouBLDoorIpInfo;
import com.bro.binterface.door.udp.gaoyoubl.utils.GaoYouBLProtocolParser;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 读取Tcp参数
 */
@Slf4j
@Component
public class GaoYouBLGetControllerIPCommand implements GaoYouBLCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouBLAccessDevice device, CmdParam param) {
        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBLProtocolUtils.buildGetControllerIP(
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
        byte[] data = (byte[]) resp.getData();
        TransparentPacket packet = TransparentPacketUtils.parseTransparentPacket(data);
        // 校验
        boolean verifyFlag = UdpUtils.verifyResponse(packet, device);
        if (!verifyFlag) {
            log.warn("透传数据无法对应:{}", Utils.bytesToHex(data));
            return R.fail("返回透传数据校验失败");
        }
        byte[] transparentData = packet.getData();
        GaoYouBLDoorIpInfo info = GaoYouBLProtocolParser.parseTcpResponse(transparentData);
        // 返回数据
        Map<String, Object> tcpMap = new HashMap<>();
        tcpMap.put("ipAddress", info.getIpAddress());
        tcpMap.put("macAddress", info.getMacAddress());
        tcpMap.put("tcpMode", info.getTcpMode());
        tcpMap.put("targetPort", info.getTargetPort());
        tcpMap.put("subnetMask", info.getSubnetMask());
        tcpMap.put("dns", info.getDns());
        tcpMap.put("localTcpPort", info.getLocalTcpPort());
        tcpMap.put("targetIp", info.getTargetIp());
        tcpMap.put("gateway", info.getGateway());
        tcpMap.put("backupDns", info.getBackupDns());
        tcpMap.put("localUdpPort", info.getLocalUdpPort());
        tcpMap.put("autoIp", info.isAutoIp());
        tcpMap.put("domain", info.getDomain());

        return R.ok(tcpMap);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GaoYouBLCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_GET_CONTROLLER_IP, this);
    }
}

package com.bro.binterface.door.tcp.gaoyou.cmd.impl;

import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.tcp.gaoyou.GaoYouAccessDoor;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolParser;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolUtils;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandleManager;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandler;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouDoorIpInfo;
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
public class GaoYouGetControllerIPCommand implements GaoYouCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouAccessDoor device, CmdParam param) {
        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBMProtocolUtils.buildGetControllerIP(
                                device.getConfig().getDeviceSn(),
                                device.getConfig().getPasswd(),
                                serial);
        log.debug("send hex：" + Utils.bytesToHex(command));
        R resp = device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        byte[] data = (byte[]) resp.getData();
        GaoYouDoorIpInfo info = GaoYouBMProtocolParser.parseTcpResponse(data);
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
        GaoYouCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_GET_CONTROLLER_IP, this);
    }
}

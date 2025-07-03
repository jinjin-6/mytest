package com.bro.binterface.door.udp.gaoyoubl.cmd.impl;

import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.udp.domain.TransparentPacket;
import com.bro.binterface.door.udp.gaoyoubl.GaoYouBLAccessDevice;
import com.bro.binterface.door.udp.gaoyoubl.cmd.GaoYouBLCmdHandleManager;
import com.bro.binterface.door.udp.gaoyoubl.cmd.GaoYouBLCmdHandler;
import com.bro.binterface.door.udp.gaoyoubl.mesaage.GaoYouBLCardInfo;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 读取所有授权卡
 * - 非排序卡区域
 */
@Slf4j
@Component
public class GaoYouBLGetAllUserPermissionCommand implements GaoYouBLCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouBLAccessDevice device, CmdParam param) {
        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBLProtocolUtils.buildGetAllUserPermissionCommand(
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
        List<GaoYouBLCardInfo> cards = GaoYouBLProtocolParser.parseAllUserPermissionResponse(transparentData);
        // 数据整理:卡号、密码、有效期、权限
        List<Map<String, Object>> cardList = cards.stream().map(card -> {
                            Map<String, Object> cardMap = new LinkedHashMap<>();
                            cardMap.put("cardNo", Utils.int5ToCardno(card.getCardNo(), 0));
                            cardMap.put("passwd", Utils.bcd4BytesToPassword(card.getPasswd()));
                            String yearStr = "20" + Utils.bcdToString(card.getEndDay(), 0, 1);
                            String monthStr = Utils.bcdToString(card.getEndDay(), 1, 1);
                            String dayStr = Utils.bcdToString(card.getEndDay(), 2, 1);
                            cardMap.put("endDay", String.format("%s-%s-%s", yearStr, monthStr, dayStr)); // 只显示年月日
                            // 处理权限
                            List<String> enabledDoors = new ArrayList<>();
                            byte enable = card.getDoorEnable();
                            if ((enable & 0x01) != 0) enabledDoors.add("1");
                            if ((enable & 0x02) != 0) enabledDoors.add("2");
                            if ((enable & 0x04) != 0) enabledDoors.add("3");
                            if ((enable & 0x08) != 0) enabledDoors.add("4");
                            cardMap.put("doorEnable", String.join(",", enabledDoors));

                            return cardMap;
                        }).collect(Collectors.toList());

        return R.ok(cardList);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GaoYouBLCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_GET_ALL_USER_PERMISSION, this);
    }
}

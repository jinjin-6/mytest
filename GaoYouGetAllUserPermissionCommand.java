package com.bro.binterface.door.tcp.gaoyou.cmd.impl;

import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.tcp.gaoyou.GaoYouAccessDoor;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolParser;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolUtils;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandleManager;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandler;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouCardInfo;
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
public class GaoYouGetAllUserPermissionCommand implements GaoYouCmdHandler, InitializingBean {
    @Override
    public R exec(GaoYouAccessDoor device, CmdParam param) {
        int serial = SerialGen.genPacketSerial();
        byte[] command = GaoYouBMProtocolUtils.buildGetAllUserPermissionCommand(
                device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(),
                serial);
        log.debug("send hex：" + Utils.bytesToHex(command));
        R resp = device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
        if (Constants.FAIL == resp.getCode()) {
            return resp;
        }
        byte[] data = (byte[]) resp.getData();
        List<GaoYouCardInfo> cards = GaoYouBMProtocolParser.parseAllUserPermissionResponse(data);
        // 数据整理:卡号、密码、有效期、权限
        List<Map<String, Object>> cardList = cards.stream().map(card -> {
                            Map<String, Object> cardMap = new LinkedHashMap<>();
                            cardMap.put("cardNo", Utils.int9ToCardno(card.getCardNo(), 0));
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
        GaoYouCmdHandleManager.register(DoorAccessDeviceCmdCode.GAOYOU_GET_ALL_USER_PERMISSION, this);
    }
}

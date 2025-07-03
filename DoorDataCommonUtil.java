package com.bro.binterface.door.utils;

import com.bro.binterface.door.AccessDoor;
import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.DoorRegistry;
import com.bro.common.core.constant.DoorConstants;
import com.bro.common.core.domain.R;
import com.bro.common.core.utils.StringUtils;

/**
 * 门禁数据相关校验工具类
 */
public class DoorDataCommonUtil {
    /**
     * 获取门禁设备
     */
    public static R getDoorDevice(CmdParam param, DoorRegistry doorDeviceRegistry) {
        String cmd = param.getCmd();
        if (StringUtils.isEmpty(cmd)) {
            return R.fail("缺少参数cmd");
        }
        String fsuId = param.getParams().get("fsuId").toString();
        if (StringUtils.isEmpty(fsuId)) {
            return R.fail("缺少参数fsuId");
        }
        String deviceId = param.getParams().get("deviceId").toString();
        if (StringUtils.isEmpty(deviceId)) {
            return R.fail("缺少参数deviceId");
        }

        String doorId = fsuId + "-" + deviceId;
        AccessDoor device = doorDeviceRegistry.getDevice(doorId);
        if (device == null) {
            return R.fail("未找到对应的门禁设备");
        }

        // 串口校验
        if (DoorConstants.PORT_TYPE_SERIAL.equals(device.getPortType())) {

            if (param.getParams().get("serialPort") == null) {
                return R.fail("缺少参数serialPort");
            }
            if (param.getParams().get("virtualNum") == null) {
                return R.fail("缺少参数virtualNum");
            }
            if (param.getParams().get("localPort") == null) {
                return R.fail("缺少参数localPort");
            }
        }

        return R.ok(device);
    }
}

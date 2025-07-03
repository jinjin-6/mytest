package com.bro.binterface.door.utils;

import com.bro.binterface.door.AccessDoor;
import com.bro.binterface.door.DoorRegistry;
import com.bro.binterface.door.tcp.gaoyou.GaoYouAccessDoor;
import com.bro.binterface.door.udp.gaoyoubl.GaoYouBLAccessDevice;
import com.bro.common.core.constant.DoorConstants;
import com.bro.common.fsu.domain.vo.DeviceDoor;
import com.bro.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DoorDataExtraction {
    @Resource
    private RedisService redisService;
    @Resource
    private DoorRegistry tcpDoorRegistry;

    /**
     * 将门禁数据按照portType分组
     * @return
     */
    public Map<Integer, List<DeviceDoor>> groupDoorListByPortType() {
        // key--door:portType:collectType:fsuId:deviceId
        Collection<String> keys = redisService.keys(DoorConstants.REDIS_DOOR_KEY + "*");
        if (CollectionUtils.isEmpty(keys)) {
            return null;
        }
        // 根据portType进行分组
        Map<Integer, List<DeviceDoor>> groupDoorList = keys.stream()
                .map(key -> {
                    Object obj = redisService.getCacheObject(key); // 先获取Object
                    return obj instanceof DeviceDoor ? (DeviceDoor) obj : null; // 类型检查
                })
                .filter(door -> door != null) // 双重过滤
                .collect(Collectors.groupingBy(
                        DeviceDoor::getPortType,
                        HashMap::new,
                        Collectors.toList()
                ));
        return groupDoorList;
    }

    /**
     * 门禁设备数据配置
     */
    public void doorDataConfig() {
        // 根据portType将门禁分组
        Map<Integer, List<DeviceDoor>> groupDoorList = groupDoorListByPortType();
        if (CollectionUtils.isEmpty(groupDoorList)) {
            log.info("没有需要采集的门禁设备");
            return;
        }
        for (Map.Entry<Integer, List<DeviceDoor>> entry : groupDoorList.entrySet()) {
            List<DeviceDoor> doors = entry.getValue();
            processDoors(doors);
        }
    }

    /**
     * 设备处理
     * @param doors
     */
    private void processDoors(List<DeviceDoor> doors) {
        for (DeviceDoor door : doors) {
            String doorId = door.getFsuId() + "-" +door.getDeviceId();
            try {
                AccessDoor device = createDeviceByCollectType(door);
                if (device != null) {
                    // key:fsuID-deviceId
                    tcpDoorRegistry.register(doorId, device);
                    device.init(); // 初始化设备连接
                }
            } catch (Exception e) {
                log.error("初始化门禁设备失败: {}", doorId, e);
            }
        }
    }


    /**
     * 根据门禁自定义协议，选择模板
     * @param door
     * @return
     */
    private AccessDoor createDeviceByCollectType(DeviceDoor door) {
        if (door == null || door.getCollectType() == null) {
            return null;
        }
        AccessDoor device;
        if (DoorConstants.PORT_TYPE_NETWORK.equals(String.valueOf(door.getPortType()))) {
            // TCP
            switch (door.getCollectType()) {
                case DoorConstants.DOOR_COLLECT_TYPE_GAOYOU:
                    // 高优 BM系列
                    return new GaoYouAccessDoor(door);
                default:
                    log.warn("未知的门禁采集类型: {}", door.getCollectType());
                    return null;
            }
        } else if (DoorConstants.PORT_TYPE_SERIAL.equals(String.valueOf(door.getPortType()))) {
           // UDP
            switch (door.getCollectType()) {
                case DoorConstants.DOOR_COLLECT_TYPE_GAOYOU_BL:
                    // 高优 BL系列
                    return new GaoYouBLAccessDevice(door);
                default:
                    log.warn("未知的门禁采集类型: {}", door.getCollectType());
                    return null;
            }
        } else if (DoorConstants.PORT_TYPE_HTTP.equals(String.valueOf(door.getPortType()))) {
            // HTTP
        }
        return null;
    }

}

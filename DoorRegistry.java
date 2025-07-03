package com.bro.binterface.door;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册门禁(TCP)
 * 获取所有注册门禁
 */
@Component
public class DoorRegistry {
    private static final Map<String, AccessDoor> devices = new ConcurrentHashMap<>();

    /**
     * 注册所有的连接门禁
     * -key:fsuId-deviceId
     * @param id
     * @param device
     */
    public void register(String id, AccessDoor device) {
        devices.put(id, device);
    }

    public AccessDoor getDevice(String id) {
        return devices.get(id);
    }

    public Collection<AccessDoor> getAllDevicesCollection() {
        Collection<AccessDoor> list = devices.values();
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>(devices.values());
        }
        return list;
    }
}

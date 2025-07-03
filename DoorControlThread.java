package com.bro.binterface.door;

import com.bro.binterface.door.utils.DoorDataExtraction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
public class DoorControlThread implements Runnable{
    @Autowired
    private DoorDataExtraction doorDataExtraction;
    @Autowired
    private DoorRegistry deviceRegistry;

    @EventListener(ContextRefreshedEvent.class)
    public void startThreadAfterContextInitialized() {
        // 启动线程
        new Thread(this::run, "门禁初始化线程").start();
    }

    @Override
    public void run() {
        // 读取门禁配置数据,并根据端口连接方式选择对应的采集方式
        doorDataExtraction.doorDataConfig();

        //  5秒钟检测一次门禁连接状态
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.warn("门禁状态检查线程被中断", e);
            }

            // 检查门禁连接状态,如果连接断了，重新连接
            Collection<AccessDoor> devices = deviceRegistry.getAllDevicesCollection();
            if (devices == null || devices.isEmpty()) {
                continue;
            }
            devices.forEach((device) -> {
                if (!device.isConnected()) {
                    try {
                        log.info("[{}]重新连接门禁设备: {}", Thread.currentThread().getName(), device.getFsuIdAndDeviceId());
                        device.init();
                    } catch (Exception e) {
                        log.error("[{}]重新连接门禁设备失败 {}", Thread.currentThread().getName(), device.getFsuIdAndDeviceId(), e);
                    }
                }
            });
        }
    }


}

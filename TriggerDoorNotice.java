package com.bro.binterface.door.utils;

import com.bro.binterface.message.AlarmPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class TriggerDoorNotice {
    @Resource
    private AlarmPublisher alarmPublisher;

    /**
     * 发送门禁上报信息
     */
    public void triggerDoorReport(String doorReportMsg) {

    }
}

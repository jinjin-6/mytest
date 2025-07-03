package com.bro.binterface.door.tcp.gaoyou.utils;

import com.bro.binterface.domain.ScDoorRecord;
import com.bro.binterface.domain.ScDoorReport;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouReportMessage;
import com.bro.binterface.door.udp.gaoyoubl.mesaage.GaoYouBLReportMessage;

/**
 * 高优门禁BM系列 数据转换工具类
 */
public class GaoYouDataExtractionUtils {
    /**
     * 高优上报事件数据格式转换
     * @param message
     * @return
     */
    public static ScDoorReport exchangeGaoYouReportData(String fsuId,
                                                        String deviceId,
                                                        GaoYouReportMessage message) {
        if (message == null) {
            return null;
        }
        return ScDoorReport.builder()
                .fsuId(fsuId)
                .deviceId(deviceId)
                .cardNo(message.getCarNo())
                .doorNo(message.getDoorNum())
                .recordType(message.getType())
                .status(message.getStatus())
                .time(message.getTime())
                .build();
    }

    /**
     * 高优记录数据
     * @param fsuId
     * @param deviceId
     * @param message
     * @return
     */
    public static ScDoorRecord exchangeGaoYouRecordData(String fsuId,
                                                        String deviceId,
                                                        GaoYouReportMessage message) {
        if (message == null) {
            return null;
        }
        return ScDoorRecord.builder()
                .fsuId(fsuId)
                .deviceId(deviceId)
                .cardNo(message.getCarNo())
                .doorNo(message.getDoorNum())
                .recordType(message.getType())
                .status(message.getStatus())
                .time(message.getTime())
                .build();
    }

    /**
     * 高优BL上报事件数据格式转换
     * @param message
     * @return
     */
    public static ScDoorReport exchangeGaoYouBLReportData(String fsuId,
                                                          String deviceId,
                                                          GaoYouBLReportMessage message) {
        if (message == null) {
            return null;
        }
        return ScDoorReport.builder()
                .fsuId(fsuId)
                .deviceId(deviceId)
                .cardNo(message.getCarNo())
                .doorNo(message.getDoorNum())
                .recordType(message.getType())
                .status(message.getStatus())
                .time(message.getTime())
                .build();
    }
}

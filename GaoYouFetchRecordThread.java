package com.bro.binterface.door.tcp.gaoyou.thread;

import com.bro.binterface.domain.ScDoorRecord;
import com.bro.binterface.door.tcp.gaoyou.GaoYouAccessDoor;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolParser;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolUtils;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouDoorRecordInfo;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouReportMessage;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouDataExtractionUtils;
import com.bro.binterface.door.utils.SerialGen;
import com.bro.binterface.door.utils.Utils;
import com.bro.binterface.service.IScDoorRecordService;
import com.bro.common.core.constant.Constants;
import com.bro.common.core.domain.R;
import com.bro.common.core.utils.SpringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 高优门禁请求记录数据线程
 */
@Data
@Slf4j
@Component
public class GaoYouFetchRecordThread implements Runnable {

    private GaoYouAccessDoor device;

    public GaoYouFetchRecordThread(GaoYouAccessDoor device) {
        this.device = device;
        Thread.currentThread().setName("高优门禁获取记录数据线程-" + device.getFsuIdAndDeviceId());
    }

    @Override
    public void run() {

        int serial = 0;
        try {
            while (true) {

                // 等待1秒
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    continue;
                }

                // 连接检测
                if (!device.isConnected()) {
                    continue;
                }

                // 读取记录指针
                serial = SerialGen.genPacketSerial();
                R resp = sendGetRecordIndex(serial);

                if (resp.getCode()== Constants.FAIL) {
                    continue;
                }


                // 检查是否有新记录
                GaoYouDoorRecordInfo dri = new GaoYouDoorRecordInfo();
                if (!GaoYouBMProtocolUtils.haveNewRecord((byte[]) resp.getData(), dri)) {
                    continue; // 没有新记录
                }

                // 读记录
                serial = SerialGen.genPacketSerial();
                resp = sendGetRecord(serial, dri);
                if (resp.getCode() == 1) {
                    continue;
                }
                byte[] data = (byte[]) resp.getData();
                if (data[27] == 0x00) {

                    GaoYouReportMessage record =  GaoYouBMProtocolParser.procRecord(data, dri.recType);
                    // 更新记录指针
                    serial = SerialGen.genPacketSerial();
                    sendUpdateRecordIndex(serial, dri.recType, dri.recIndex, dri.loopFlag);
                    // 将数据保存进数据库
                    ScDoorRecord recordGaoyou = GaoYouDataExtractionUtils.exchangeGaoYouRecordData(device.getFsuId(),
                            device.getDeviceId(), record);
                    recordGaoyou.setTableName(device.getCollectType());
                    if (recordGaoyou != null) {
                        IScDoorRecordService recordGaoyouService =
                                SpringUtils.getBean(IScDoorRecordService.class);
                        recordGaoyouService.insertScDoorRecord(recordGaoyou);
                    }
                }
            }
        } catch (Exception e) {
            device.setConnected(false);
            serial = 0;
            log.error("[{}]高优门禁获取记录数据线程: {}", Thread.currentThread().getName(), e.getMessage());
        }
    }

    /**
     * 获取记录指针
     */
    private R sendGetRecordIndex(int serial) {
        byte[] command = GaoYouBMProtocolUtils.buildGetRecordIndexCommand(device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(), serial);
        log.info("send-读取记录指针：" + Utils.bytesToHex(command));
        return device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
    }

    /**
     * 获取记录
     * 
     * @param serial
     * @param dri
     * @return
     * @throws TimeoutException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private R sendGetRecord(int serial, GaoYouDoorRecordInfo dri) {
        byte[] command = GaoYouBMProtocolUtils.buildGetRecordCommand(device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(), serial, dri.recType, dri.recIndex, 1);
        log.info("send-读记录：" + Utils.bytesToHex(command));
        return device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
    }

    /**
     * 更新记录指针
     * 
     * @param serial
     * @param recType
     * @param recordIndex
     * @param loopFlag
     * @return
     */
    private R sendUpdateRecordIndex(int serial, int recType, int recordIndex, int loopFlag) {
        byte[] command = GaoYouBMProtocolUtils.buildUpdateRecordIndexCommand(device.getConfig().getDeviceSn(),
                device.getConfig().getPasswd(), serial, recType, recordIndex,
                loopFlag);
        log.info("send-更新记录指针：" + Utils.bytesToHex(command));
        return device.sendCommandWithResponse(serial, command, 10, TimeUnit.SECONDS);
    }

}

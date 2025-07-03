package com.bro.binterface.door.tcp.gaoyou.thread;

import com.bro.binterface.domain.ScDeviceDoorSignal;
import com.bro.binterface.domain.ScDoorReport;
import com.bro.binterface.door.tcp.gaoyou.GaoYouAccessDoor;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolParser;
import com.bro.binterface.door.tcp.gaoyou.constants.GaoYouConstants;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouDeviceMessage;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouReportMessage;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouDataExtractionUtils;
import com.bro.binterface.door.utils.DataCache;
import com.bro.binterface.door.utils.Utils;
import com.bro.binterface.message.AlarmPublisher;
import com.bro.binterface.service.IScDeviceDoorSignalService;
import com.bro.binterface.service.IScDoorReportService;
import com.bro.common.core.constant.DoorAccessDeviceCmdCode;
import com.bro.common.core.domain.R;
import com.bro.common.core.utils.SpringUtils;
import com.bro.common.core.utils.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/**
 * 高优门禁接收数据线程
 */
@Data
@Slf4j
@Component
public class GaoYouFetchDataThread implements Runnable {

    private GaoYouAccessDoor device;

    public GaoYouFetchDataThread(GaoYouAccessDoor device) {
        this.device = device;
        Thread.currentThread().setName("高优门禁接收数据线程-" + device.getFsuIdAndDeviceId());
    }

    @Override
    public void run() {
        byte[] buffer = new byte[2048];
        int len = 0;
        int headCount = 0;
        try {
            // 循环接收数据，直到接收到一个数据包
            while (true) {

                try {

                    // 检测连接状态
                    if (!device.isConnected()) {
                        Thread.sleep(100);
                        continue;
                    }

                    // 读字节，每次一个字节
                    int b = device.getIn().read();
                    if (b == -1) {
                        device.setConnected(false);
                        log.warn("[{}]门禁主动断开连接", Thread.currentThread().getName());
                        return;
                    }

                    // 检测0x7E
                    if (b == GaoYouConstants.FRAME_DELIMITER) {

                        // 存储
                        buffer[len++] = (byte) b;
                        headCount++;

                        // 报文尾部检测
                        if (headCount == 2) {
                            // 校验长度
                            if (len < GaoYouConstants.PACKET_MIN_LEN) {
                                log.debug(Utils.bytesToHex(buffer, 0, len));
                                // 长度清零
                                len = 0;
                                continue;
                            }

                            // 反转义
                            byte[] packet = decodeData(buffer, len);

                            // 计算校验和,不包含首尾FE字节和检验码字节
                            byte calcChecksum = 0;
                            for (int i = 1; i < packet.length - 2; ++i) {
                                calcChecksum += packet[i];
                            }

                            // 检查校验和
                            byte checkSum = packet[packet.length - 2];
                            if (calcChecksum != checkSum) {
                                log.warn("[{}]报文校验失败！", Thread.currentThread().getName());
                                continue;
                            }

                            // 解析
                            GaoYouDeviceMessage msg = GaoYouBMProtocolParser.parseProtocol(packet);

                            // 判断是否为主动上报事件
                            if (isEventMessage(msg)) {
                                // 事件上报
                                log.debug("[{}]主动上报 hex：" + Utils.bytesToHex(packet), Thread.currentThread().getName());
                                GaoYouReportMessage reportMessage = GaoYouBMProtocolParser.parseEventReportResponse(packet);
                                // 保存上报事件进数据库
                                ScDoorReport reportGaoyou = GaoYouDataExtractionUtils.exchangeGaoYouReportData(device.getFsuId(),
                                        device.getDeviceId(), reportMessage);
                                // 添加表名
                                reportGaoyou.setTableName(device.getCollectType());
                                // 推送门禁上报数据
                                AlarmPublisher alarmPublisher = SpringUtils.getBean(AlarmPublisher.class);
                                alarmPublisher.sendDoorAlarm(reportGaoyou.toString());

                                if (reportGaoyou != null) {
                                    // 保存上报事件数据
                                    IScDoorReportService scDoorReportGaoyouService =
                                            SpringUtils.getBean(IScDoorReportService.class);
                                    scDoorReportGaoyouService.insertScDoorReport(reportGaoyou);
                                    // 修改门状态点号数据
                                    if (!StringUtils.isEmpty(reportMessage.getDoorStatus())) {
                                        IScDeviceDoorSignalService doorSignalService =
                                                SpringUtils.getBean(IScDeviceDoorSignalService.class);
                                        ScDeviceDoorSignal scDeviceDoorSignal = new ScDeviceDoorSignal();
                                        scDeviceDoorSignal.setFsuId(device.getFsuId());
                                        scDeviceDoorSignal.setDeviceId(device.getDeviceId());
                                        scDeviceDoorSignal.setSignalId(DoorAccessDeviceCmdCode.GAOYOU_DOOR_STATUS + "_" +
                                                reportMessage.getDoorNum());
                                        scDeviceDoorSignal.setMeasuredValue(reportMessage.getDoorStatus());
                                        doorSignalService.updateScDeviceDoorSignal(scDeviceDoorSignal);
                                    }
                                }

                            } else {
                                // 命令响应
                                handleCmdResponse(msg, packet);
                                log.debug("[{}]receive hex：" + Utils.bytesToHex(packet), Thread.currentThread().getName());
                            }
                            // 处置完成后重置状态
                            headCount = 0;
                            len = 0;
                        }
                    } else {
                        if (headCount > 0) {
                            buffer[len++] = (byte) b;
                            // 等待报文尾过久,抛弃收到的数据
                            if (len >= 1024) {
                                headCount = 0;
                                len = 0;
                            }
                        }
                    }
                } catch (Exception ex) {
                    device.setConnected(false);
                    len = 0;
                    headCount = 0;
                    log.error(ex.getMessage());

                }
            }
        } catch (Exception e) {
            device.setConnected(false);
            len = 0;
            headCount = 0;
            log.error("[{}]高优门禁数据接收异常: {}", Thread.currentThread().getName(), e.getMessage());
        }
    }

    /**
     * 是否为主动上报事件
     * 
     * @param msg
     * @return
     */
    private boolean isEventMessage(GaoYouDeviceMessage msg) {
        // 实时监控事件 (分类0x19)
        if (msg.getCategory() == 0x19) {
            return true;
        }
        // 报警事件 (分类0x01, 命令0x0C)
        if (msg.getCategory() == 0x01 && msg.getCommand() == 0x0C) {
            return true;
        }
        return false;
    }

    /**
     * 解析命令响应
     * 
     * @param msg
     * @param packet
     */
    private void handleCmdResponse(GaoYouDeviceMessage msg, byte[] packet) {
        // 获取信息码
        int serial = Utils.getIntFromBuf(msg.getInfoCode(), 0);
        R result = R.ok(packet, "接收成功");
        DataCache.completeResponse(serial, result);
    }

    /**
     * 报文反转义
     * 
     * @param packet
     * @param pkLen
     * @return
     */
    public static byte[] decodeData(byte[] packet, int pkLen) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean isEscaped = false;
        for (int i = 0; i < pkLen; i++) {
            byte current = packet[i];
            if (isEscaped) {
                if (current == 0x01) {
                    out.write(GaoYouConstants.FRAME_DELIMITER);
                    isEscaped = false;
                } else {
                    out.write(GaoYouConstants.ESCAPE_CHAR);
                    isEscaped = false;
                }
            } else if (current == GaoYouConstants.ESCAPE_CHAR) {
                isEscaped = true;
            } else {
                out.write(current);
            }
        }
        return out.toByteArray();
    }
}

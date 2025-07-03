package com.bro.binterface.door.udp.gaoyoubl;

import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.bro.binterface.domain.ScDeviceDoorSignal;
import com.bro.binterface.domain.ScDoorReport;
import com.bro.binterface.door.AccessDoor;
import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.tcp.gaoyou.message.GaoYouDeviceMessage;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouBMProtocolParser;
import com.bro.binterface.door.tcp.gaoyou.utils.GaoYouDataExtractionUtils;
import com.bro.binterface.door.udp.domain.TransparentPacket;
import com.bro.binterface.door.udp.gaoyoubl.cmd.GaoYouBLCmdHandleManager;
import com.bro.binterface.door.udp.gaoyoubl.cmd.GaoYouBLCmdHandler;
import com.bro.binterface.door.udp.gaoyoubl.mesaage.GaoYouBLReportMessage;
import com.bro.binterface.door.udp.gaoyoubl.utils.GaoYouBLProtocolParser;
import com.bro.binterface.door.udp.utils.TransparentPacketUtils;
import com.bro.binterface.door.udp.utils.UdpUtils;
import com.bro.binterface.door.utils.DataCache;
import com.bro.binterface.door.utils.Utils;
import com.bro.binterface.message.AlarmPublisher;
import com.bro.binterface.service.IScDeviceDoorSignalService;
import com.bro.binterface.service.IScDoorReportService;
import com.bro.common.core.constant.DoorAccessDeviceCmdCode;
import com.bro.common.core.domain.R;
import com.bro.common.core.utils.SpringUtils;
import com.bro.common.core.utils.StringUtils;
import com.bro.common.fsu.domain.vo.DeviceDoor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 高优门禁：BL系列
 */
@Data
@Slf4j
@Component
public class GaoYouBLAccessDevice extends DeviceDoor implements AccessDoor {
    private GaoYouBLDoorConfig config;
    private DatagramSocket socket;
    private Thread receiveThread;
    private boolean connected;

    // 用于存储命令响应回调
    private static final ConcurrentHashMap<Integer, CompletableFuture<R>> responseCallbacks = new ConcurrentHashMap<>();

    // 心跳配置（单位：毫秒）
    private static final int HEARTBEAT_INTERVAL = 120000; // 120秒
    private static final int HEARTBEAT_TIMEOUT = 360000; // 360秒

    private volatile long lastHeartbeatTime;

    public GaoYouBLAccessDevice() {

    }

    public GaoYouBLAccessDevice(DeviceDoor door) {
        Objects.requireNonNull(door, "DeviceDoor不能为null");
        org.springframework.beans.BeanUtils.copyProperties(door, this);
        if (door.getCollectParam() != null) {
            try {
                this.config = new Gson().fromJson(door.getCollectParam(), GaoYouBLDoorConfig.class);
            } catch (Exception e) {
                log.error("解析collectParam失败: 门禁id[{}] 采集参数[{}]",
                        getFsuIdAndDeviceId() + door.getCollectParam(),
                        e);
                this.config = new GaoYouBLDoorConfig(); // 默认配置
            }
        }
    }

    @Override
    public void init() throws Exception {
        dispose();
        try {
            // 创建UDP socket并绑定到指定端口
            socket = new DatagramSocket(this.config.getLocalPort());
            socket.setSoTimeout(5000); // 设置接收超时时间

            // 启动接收线程
            if (receiveThread == null || receiveThread.isAlive()) {
                receiveThread = new Thread(this::receiveLoop, "UDP-Receiver-" + getFsuIdAndDeviceId());
                receiveThread.setDaemon(true);  // 设置为守护线程
                receiveThread.start();
            }

            connected = true;
            log.info("UDP门禁设备初始化成功: 门禁id[{}] 监听端口[{}]", getFsuIdAndDeviceId(), this.config.getLocalPort());
        } catch (SocketException e) {
            connected = false;
            throw new Exception("创建UDP socket失败", e);
        }
    }

    @Override
    public void dispose() {
        connected = false;
        try {
            if (socket != null)
                socket.close();
        } catch (Exception ex) {
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
    }

    @Override
    public String getFsuIdAndDeviceId() {
        return super.getFsuId() + ":" + super.getDeviceId();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public R execCmd(CmdParam param) {
        GaoYouBLCmdHandler cmdHandler = GaoYouBLCmdHandleManager.getHandler(param.getCmd());
        if (cmdHandler != null) {
            return cmdHandler.exec(this, param);
        }
        return R.fail("命令不存在");
    }

    /**
     * 发送命令并等待响应
     */
    public R sendCommandWithResponse(int commandId, byte[] command, long timeout, TimeUnit unit) {
        if (!connected) {
            return R.fail("设备未连接");
        }
        CompletableFuture<R> future = new CompletableFuture<>();
        DataCache.registerResponseCallback(commandId, future);

        try {
            DatagramPacket packet = new DatagramPacket(command, command.length, InetAddress.getByName(this.getIp()),
                    this.getPort());
            socket.send(packet);
        } catch (IOException e) {
            DataCache.removeCallback(commandId);
            return R.fail("发送命令失败: " + e.getMessage());
        }

        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            DataCache.removeCallback(commandId);
            return R.fail("响应超时");
        } catch (Exception e) {
            DataCache.removeCallback(commandId);
            return R.fail("等待响应失败: " + e.getMessage());
        }
    }

    /**
     * 接收循环
     */
    private void receiveLoop() {
        byte[] buffer = new byte[2048];
        while (connected) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // 处理接收到的数据
                byte[] receivedData = new byte[packet.getLength()];
                System.arraycopy(buffer, 0, receivedData, 0, packet.getLength());

                processReceivedData(receivedData);
            } catch (java.net.SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                if (connected) { // 如果是主动断开，不记录错误
                    log.error("接收数据异常", e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * 处理接收到的数据
     */
    private void processReceivedData(byte[] data) {
        // 解析协议头
        if (data.length < 40) {
            log.warn("接收到的数据长度不足");
            return;
        }

        // 检查是否是心跳包
        if (UdpUtils.isHeartbeatPacket(data)) {
            handleHeartbeat(data);
            return;
        }

        // 检查是否为主动上报事件
        if (UdpUtils.isCommandResponse(data)) {
            handleCommandResponse(data);
            return;
        }
    }

    /**
     * 处理返回数据
     */
    private void handleCommandResponse(byte[] data) {
        TransparentPacket packet = TransparentPacketUtils.parseTransparentPacket(data);
        byte[] transparentData = packet.getData();

        if (transparentData.length < 34) {
            return;
        }
        // 反转义
        byte[] packetData = GaoYouBLProtocolParser.decodeData(transparentData, transparentData.length);

        // 计算校验和,不包含首尾FE字节和检验码字节
        byte calcChecksum = 0;
        for (int i = 1; i < packetData.length - 2; ++i) {
            calcChecksum += packetData[i];
        }

        // 检查校验和
        byte checkSum = packetData[packetData.length - 2];
        if (calcChecksum != checkSum) {
            log.warn("[{}]报文校验失败！", Utils.bytesToHex(data));
            return;
        }
        // 解析
        GaoYouDeviceMessage msg = GaoYouBMProtocolParser.parseProtocol(packetData);

        // 实时监控事件 (分类0x19)
        if (msg.getCategory() != 0x19) {
            // 命令响应
            // 获取信息码
            int serial = Utils.getIntFromBuf(msg.getInfoCode(), 0);
            R result = R.ok(packetData, "接收成功");
            DataCache.completeResponse(serial, result);
            return;
        } else {
            // 事件上报
            log.debug("[{}]主动上报 hex：" + Utils.bytesToHex(packetData), Thread.currentThread().getName());
            GaoYouBLReportMessage reportMessage = GaoYouBLProtocolParser.parseEventReportResponse(packetData);
            // 保存上报事件进数据库
            ScDoorReport reportGaoyou = GaoYouDataExtractionUtils.exchangeGaoYouBLReportData(this.getFsuId(),
                    this.getDeviceId(), reportMessage);
            // 推送门禁上报数据
            AlarmPublisher alarmPublisher = SpringUtils.getBean(AlarmPublisher.class);
            alarmPublisher.sendDoorAlarm(reportGaoyou.toString());
            // 添加表名
            reportGaoyou.setTableName(this.getCollectType());
            if (reportGaoyou != null) {
                // 保存上报事件数据
                IScDoorReportService scDoorReportService =
                        SpringUtils.getBean(IScDoorReportService.class);
                scDoorReportService.insertScDoorReport(reportGaoyou);
                // 修改门状态点号数据
                if (!StringUtils.isEmpty(reportMessage.getDoorStatus())) {
                    IScDeviceDoorSignalService doorSignalService =
                            SpringUtils.getBean(IScDeviceDoorSignalService.class);
                    ScDeviceDoorSignal scDeviceDoorSignal = new ScDeviceDoorSignal();
                    scDeviceDoorSignal.setFsuId(this.getFsuId());
                    scDeviceDoorSignal.setDeviceId(this.getDeviceId());
                    scDeviceDoorSignal.setSignalId(DoorAccessDeviceCmdCode.GAOYOU_DOOR_STATUS + "_" +
                            reportMessage.getDoorNum());
                    scDeviceDoorSignal.setMeasuredValue(reportMessage.getDoorStatus());
                    doorSignalService.updateScDeviceDoorSignal(scDeviceDoorSignal);
                }
            }
        }

    }

    /**
     * 处理心跳包
     * @param data
     */
    private void handleHeartbeat(byte[] data) {
        log.debug("收到心跳包 from [{}]", getFsuIdAndDeviceId());
        // 更新心跳时间，用于判断连接
        Long previousTime = UdpUtils.updateHeartbeat(getFsuIdAndDeviceId());
        // 判断是否为恢复状态（需要确保HEARTBEAT_TIMEOUT是毫秒级）
        if (isConnectionRecovered(previousTime)) {
            // 发送通道恢复通知
            UdpUtils.notifyConnectionRecovery(this.getFsuId(), this.getDeviceName());
        }
    }

    /**
     * 判断是否连接恢复
     */
    private boolean isConnectionRecovered(Long previousTime) {
        return previousTime == null ||
                (System.currentTimeMillis() - previousTime) > HEARTBEAT_TIMEOUT;
    }



}

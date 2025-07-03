package com.bro.binterface.door.tcp.gaoyou;

import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.bro.binterface.door.AccessDoor;
import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandleManager;
import com.bro.binterface.door.tcp.gaoyou.cmd.GaoYouCmdHandler;
import com.bro.binterface.door.tcp.gaoyou.thread.GaoYouFetchDataThread;
import com.bro.binterface.door.tcp.gaoyou.thread.GaoYouFetchRecordThread;
import com.bro.binterface.door.utils.DataCache;
import com.bro.common.core.domain.R;
import com.bro.common.fsu.domain.vo.DeviceDoor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 高优门禁 BM系列
 */
@Data
@Slf4j
@Component
public class GaoYouAccessDoor extends DeviceDoor implements AccessDoor {
    private boolean connected;
    private Socket socket = null;
    private InputStream in = null;
    private OutputStream out = null;
    private Thread fetchDataThread = null;
    private Thread fetchRecordThread = null;
    private GaoYouDoorConfig config;

    public GaoYouAccessDoor() {

    }

    public GaoYouAccessDoor(DeviceDoor door) {
        Objects.requireNonNull(door, "DeviceDoor不能为null");
        org.springframework.beans.BeanUtils.copyProperties(door, this);
        if (door.getCollectParam() != null) {
            try {
                this.config = new Gson().fromJson(door.getCollectParam(), GaoYouDoorConfig.class);
            } catch (Exception e) {
                log.error("解析collectParam失败: 门禁id[{}] 采集参数[{}]",
                        getFsuIdAndDeviceId() + door.getCollectParam(),
                        e);
                this.config = new GaoYouDoorConfig(); // 默认配置
            }
        }
    }

    @Override
    public void init() throws Exception {
        dispose();
        socket = new Socket(this.getIp(), this.getPort());
        in = socket.getInputStream();
        out = socket.getOutputStream();
        // 启动接收线程
        if (fetchDataThread == null || !fetchDataThread.isAlive()) {
            fetchDataThread = new Thread(new GaoYouFetchDataThread(this));
            fetchDataThread.start();
        }

        // 记录FETCH
        if (fetchRecordThread == null || !fetchRecordThread.isAlive()) {
            fetchRecordThread = new Thread(new GaoYouFetchRecordThread(this));
            fetchRecordThread.start();
        }

        connected = true;
    }

    /**
     * 发送命令，等待响应
     *
     * @param commandId
     * @param command
     * @param timeout
     * @param unit
     * @return
     * @throws TimeoutException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public R sendCommandWithResponse(int commandId, byte[] command, long timeout, TimeUnit unit) {

        // 检测连接
        if (!connected) {
            return R.fail("设备未连接");
        }

        CompletableFuture<R> future = new CompletableFuture<>();
        DataCache.registerResponseCallback(commandId, future);

        // 发送数据包
        try {
            out.write(command);
        } catch (IOException ex) {
            DataCache.removeCallback(commandId);
            connected = false;
            return R.fail(ex.getMessage());
        }

        // 等待响应
        try {
            return future.get(timeout, unit);
        } catch (InterruptedException e) {
            DataCache.removeCallback(commandId);
            return R.fail("中断异常");
        } catch (ExecutionException e) {
            DataCache.removeCallback(commandId);
            return R.fail("执行异常");
        } catch (TimeoutException e) {
            DataCache.removeCallback(commandId);
            return R.fail("设备响应超时");
        }
    }

    @Override
    public void dispose() {
        try {
            if (socket != null)
                socket.close();
        } catch (Exception ex) {
        }
        try {
            if (in != null)
                in.close();
        } catch (Exception ex) {
        }
        try {
            if (out != null)
                out.close();
        } catch (Exception ex) {
        }

        // 添加线程中断逻辑
        if (fetchDataThread != null && fetchDataThread.isAlive()) {
            fetchDataThread.interrupt();
        }
        if (fetchRecordThread != null && fetchRecordThread.isAlive()) {
            fetchRecordThread.interrupt();
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
        GaoYouCmdHandler cmdHandler = GaoYouCmdHandleManager.getHandler(param.getCmd());
        if (cmdHandler != null) {
            return cmdHandler.exec(this, param);
        }
        return R.fail("命令不存在");
    }
}

package com.bro.binterface.door;

import com.bro.common.core.domain.R;

/**
 * 门禁接口
 */
public interface AccessDoor {

    /**
     * 初始化
     */
    void init() throws Exception;

    /**
     * 清理
     */
    void dispose();

    /**
     * 设备ID
     * 
     * @return
     */
    String getFsuIdAndDeviceId();

    /**
     * 端口连接方式
     * @return
     */
    int getPortType();

    /**
     * 是否已连接
     * 
     * @return
     */
    boolean isConnected();

    /**
     * 执行命令
     * 
     * @param param
     */
    R execCmd(CmdParam param);
}
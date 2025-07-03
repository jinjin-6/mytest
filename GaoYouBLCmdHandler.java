package com.bro.binterface.door.udp.gaoyoubl.cmd;

import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.udp.gaoyoubl.GaoYouBLAccessDevice;
import com.bro.common.core.domain.R;

/**
 * 命令接口
 */
public interface GaoYouBLCmdHandler {

    /**
     * 命令执行
     * 
     * @param param
     */
    R exec(GaoYouBLAccessDevice device, CmdParam param);
}

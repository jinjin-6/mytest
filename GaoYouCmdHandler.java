package com.bro.binterface.door.tcp.gaoyou.cmd;

import com.bro.binterface.door.CmdParam;
import com.bro.binterface.door.tcp.gaoyou.GaoYouAccessDoor;
import com.bro.common.core.domain.R;

/**
 * 命令接口
 */
public interface GaoYouCmdHandler {

    /**
     * 命令执行
     * 
     * @param param
     */
    R exec(GaoYouAccessDoor device, CmdParam param);
}

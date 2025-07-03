package com.bro.binterface.door;

import lombok.Data;

import java.util.Map;

@Data
public class CmdParam {
    // 命令码
    private String cmd;

    // 参数
    private Map<String, Object> params;
}

package com.bro.binterface.door.udp.gaoyoubl.cmd;

import java.util.HashMap;
import java.util.Map;

public final class GaoYouBLCmdHandleManager {
    private static Map<String, GaoYouBLCmdHandler> handlers = new HashMap<>();

    public static void register(String cmdCode, GaoYouBLCmdHandler handler) {
        handlers.put(cmdCode, handler);
    }

    public static GaoYouBLCmdHandler getHandler(String cmdCode) {
        return handlers.get(cmdCode);
    }
}

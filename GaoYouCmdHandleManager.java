package com.bro.binterface.door.tcp.gaoyou.cmd;

import java.util.HashMap;
import java.util.Map;

public final class GaoYouCmdHandleManager {
    private static Map<String, GaoYouCmdHandler> handlers = new HashMap<>();

    public static void register(String cmdCode, GaoYouCmdHandler handler) {
        handlers.put(cmdCode, handler);
    }

    public static GaoYouCmdHandler getHandler(String cmdCode) {
        return handlers.get(cmdCode);
    }
}

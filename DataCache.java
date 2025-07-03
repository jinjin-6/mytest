package com.bro.binterface.door.utils;

import com.bro.common.core.domain.R;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

// 数据缓存管理器
@Component
public class DataCache {
    private static final Map<Integer, CompletableFuture<R>> responseMap = new ConcurrentHashMap<>();

    public static void registerResponseCallback(Integer commandId, CompletableFuture<R> future) {
        responseMap.put(commandId, future);
    }

    public static void completeResponse(Integer commandId, R result) {
        CompletableFuture<R> future = responseMap.get(commandId);
        if (future != null) {
            future.complete(result);
            responseMap.remove(commandId);
        }
    }

    public static void removeCallback(Integer commandId) {
        responseMap.remove(commandId);
    }
}

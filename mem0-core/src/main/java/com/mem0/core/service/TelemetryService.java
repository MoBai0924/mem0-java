package com.mem0.core.service;

import com.mem0.core.config.MemoryConfig;
import com.mem0.core.domain.model.MemoryInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TelemetryService {

    @Autowired
    private MemoryConfig memoryConfig;

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    /**
     * 捕获遥测事件
     *
     * @param eventName      事件名称
     * @param memoryInstance Mem0核心实例（对应Python memory_instance）
     * @param additionalData 附加数据（可为null）
     */
    public void captureEvent(String eventName, MemoryInstance memoryInstance, Map<String, Object> additionalData) {
        // 1. 遥测开关关闭，直接返回（等价Python if not MEM0_TELEMETRY）
        if (!memoryConfig.getMem0Telemetry()) {
            return;
        }

        try {
            //todo  2. 获取遥测客户端，为空则返回

            // todo 补充完整

            //todo 5. 发送遥测事件

        } catch (Exception e) {
            // 6. 异常仅打印DEBUG日志，绝不抛出（核心要求）
            log.debug("Failed to capture OSS telemetry event {}: {}", eventName, e.getMessage(), e);
        }
    }

}

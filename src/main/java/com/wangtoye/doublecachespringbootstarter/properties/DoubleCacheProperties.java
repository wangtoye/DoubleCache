package com.wangtoye.doublecachespringbootstarter.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author wangtoye
 * @date 2019-12-05
 * Description: 从配置文件读取配置信息
 */
@Data
@Component
@ConfigurationProperties(prefix = "double-cache")
public class DoubleCacheProperties {
    private Map<String, CacheProperties.Caffeine> caffeine;
    private Map<String, CacheProperties.Redis> redis;
    private String topic="cache:redis:caffeine:topic";
    private boolean allowNullValues = false;
    private boolean useL1Cache = false;
}
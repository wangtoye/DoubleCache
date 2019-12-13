package com.wangtoye.doublecachespringbootstarter.configuration;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA.
 *
 * @author wangtoye
 * @date 2019-12-05
 * Description:
 */
@Data
@Accessors(chain = true)
public class CaffeineCacheConfiguration {
    private String spec;

    public CaffeineCacheConfiguration(String spec) {
        this.spec = spec;
    }

    public static CaffeineCacheConfiguration defaultCacheConfig() {
        // 初始的缓存空间大小
        // 缓存的最大条数
        // 最后一次写入后经过固定时间过期
        // 创建缓存或者最近一次更新缓存后经过固定的时间间隔刷新缓存
        return new CaffeineCacheConfiguration(
                "initialCapacity=10"
                        + ",maximumSize=100"
                        + ",expireAfterWrite=60s"
                //                        + ",refreshAfterWrite=60s"
        );
    }
}
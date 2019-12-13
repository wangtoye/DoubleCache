package com.wangtoye.doublecachespringbootstarter.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 *
 * @author wangtoye
 * @date 2019-12-09
 * Description:
 */
public class CacheUtils {

    /**
     * 创建GenericJackson2JsonRedisSerializer
     *
     * @return GenericJackson2JsonRedisSerializer
     */
    public static GenericJackson2JsonRedisSerializer getGenericJackson2JsonRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(new ObjectMapper() {{
            setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
                    .activateDefaultTyping(getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL,
                            JsonTypeInfo.As.WRAPPER_ARRAY);
        }});
    }

    /**
     * 构造用户自定义的RedisCache配置
     *
     * @param redisProperties    自定义配置
     * @param defaultCacheConfig 默认配置
     * @return 整合默认配置和自定义配置之后的最终配置
     */
    public static RedisCacheConfiguration handleRedisCacheConfiguration(CacheProperties.Redis redisProperties,
                                                                  RedisCacheConfiguration defaultCacheConfig) {
        if (Objects.isNull(redisProperties)) {
            return defaultCacheConfig;
        }

        //过期时间
        if (redisProperties.getTimeToLive() != null) {
            defaultCacheConfig = defaultCacheConfig.entryTtl(redisProperties.getTimeToLive());
        }
        //是否允许存储null值
        if (!redisProperties.isCacheNullValues()) {
            defaultCacheConfig = defaultCacheConfig.disableCachingNullValues();
        }
        //前缀
        if (redisProperties.getKeyPrefix() != null) {
            defaultCacheConfig =
                    defaultCacheConfig.computePrefixWith(cacheName -> cacheName + redisProperties.getKeyPrefix());
        }
        //是否允许使用前缀
        if (!redisProperties.isUseKeyPrefix()) {
            defaultCacheConfig = defaultCacheConfig.disableKeyPrefix();
        }
        return defaultCacheConfig;
    }
}

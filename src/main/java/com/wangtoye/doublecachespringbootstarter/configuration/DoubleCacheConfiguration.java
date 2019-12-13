package com.wangtoye.doublecachespringbootstarter.configuration;

import com.google.common.collect.Lists;
import com.wangtoye.doublecachespringbootstarter.properties.DoubleCacheProperties;
import com.wangtoye.doublecachespringbootstarter.utils.CacheUtils;
import lombok.Data;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created with IntelliJ IDEA.
 *
 * @author wangtoye
 * @date 2019-12-04
 * Description:
 */
@Data
public class DoubleCacheConfiguration {
    private CaffeineCacheConfiguration caffeineCacheConfiguration;
    private RedisCacheConfiguration redisCacheConfiguration;

    public DoubleCacheConfiguration(CaffeineCacheConfiguration caffeineCacheConfiguration,
                                    RedisCacheConfiguration redisCacheConfiguration) {
        this.caffeineCacheConfiguration = caffeineCacheConfiguration;
        this.redisCacheConfiguration = redisCacheConfiguration;
    }

    /**
     * 构造默认的DoubleCacheConfiguration
     *
     * @return doubleCacheConfiguration
     */
    public static DoubleCacheConfiguration defaultCacheConfig() {
        CaffeineCacheConfiguration defaultCaffeineCacheConfig =
                CaffeineCacheConfiguration.defaultCacheConfig().setSpec("initialCapacity=10" +
                        ",maximumSize=50" +
                        ",expireAfterWrite=60s");
        RedisCacheConfiguration defaultRedisCacheConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                // key 默认 StringRedisSerializer
                // value 默认 JdkSerializationRedisSerializer，此处修改为 GenericJackson2JsonRedisSerializer
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(CacheUtils.getGenericJackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofSeconds(60))
                // 默认两个::多了一层空文件夹
                .computePrefixWith(name -> name + ":")
                .disableCachingNullValues();
        return new DoubleCacheConfiguration(defaultCaffeineCacheConfig, defaultRedisCacheConfig);
    }

    /**
     * 构造DoubleCacheConfiguration映射关系
     *
     * @param defaultCustomCaffeineCacheConfig 用户自定义的caffeine默认配置
     * @param defaultCustomRedisCacheConfig    用户自定义的redis默认配置
     * @param doubleCacheProperties            用户自定义的所有配置
     * @return DoubleCacheConfiguration map
     */
    public static Map<String, DoubleCacheConfiguration> buildDoubleCacheConfigurationMap(
            CaffeineCacheConfiguration defaultCustomCaffeineCacheConfig
            , RedisCacheConfiguration defaultCustomRedisCacheConfig
            , DoubleCacheProperties doubleCacheProperties) {
        //默认配置校验
        final CaffeineCacheConfiguration defaultCaffeineCacheConfig =
                checkDefaultCaffeineCacheConfig(defaultCustomCaffeineCacheConfig, doubleCacheProperties.isUseL1Cache());
        final RedisCacheConfiguration defaultRedisCacheConfig =
                checkDefaultRedisCacheConfig(defaultCustomRedisCacheConfig);

        //依据key将两个自定义缓存配置合并
        Map<String, List<Object>> cachePropertiesMap =
                Stream.concat(Optional.ofNullable(doubleCacheProperties.getCaffeine()).orElseGet(HashMap::new).entrySet().stream(),
                        Optional.ofNullable(doubleCacheProperties.getRedis()).orElseGet(HashMap::new).entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, value -> Lists.newArrayList(value.getValue())
                                , (List<Object> list1, List<Object> list2) -> {
                                    list1.addAll(list2);
                                    return list1;
                                }));

        //生成doubleCacheConfiguration
        Map<String, DoubleCacheConfiguration> doubleCacheConfigurationMap = new HashMap<>(cachePropertiesMap.size());
        cachePropertiesMap.forEach((k, v) -> {
            if (v.size() == 0) {
                //没有缓存配置，使用默认缓存配置
                doubleCacheConfigurationMap.put(k, new DoubleCacheConfiguration(defaultCaffeineCacheConfig,
                        defaultRedisCacheConfig));
            } else if (v.size() == 1) {
                //有一个缓存配置，依据缓存配置类型进行构造
                Object config = v.get(0);
                if (config instanceof CacheProperties.Redis) {
                    //此配置是二级缓存配置
                    RedisCacheConfiguration customRedisCacheConfig =
                            CacheUtils.handleRedisCacheConfiguration((CacheProperties.Redis) config,
                                    defaultRedisCacheConfig);
                    doubleCacheConfigurationMap.put(k, new DoubleCacheConfiguration(defaultCaffeineCacheConfig,
                            customRedisCacheConfig));
                } else if (config instanceof CacheProperties.Caffeine) {
                    //此配置是一级缓存配置
                    CaffeineCacheConfiguration customCaffeineCacheConfig =
                            new CaffeineCacheConfiguration(((CacheProperties.Caffeine) config).getSpec());
                    doubleCacheConfigurationMap.put(k, new DoubleCacheConfiguration(customCaffeineCacheConfig,
                            defaultRedisCacheConfig));
                }
            } else if (v.size() == 2) {
                //两个缓存配置都存在，配置顺序只会是[一级，二级]
                CaffeineCacheConfiguration customCaffeineCacheConfig =
                        new CaffeineCacheConfiguration(((CacheProperties.Caffeine) v.get(0)).getSpec());
                RedisCacheConfiguration customRedisCacheConfig =
                        CacheUtils.handleRedisCacheConfiguration((CacheProperties.Redis) v.get(1),
                                defaultRedisCacheConfig);
                doubleCacheConfigurationMap.put(k, new DoubleCacheConfiguration(customCaffeineCacheConfig,
                        customRedisCacheConfig));
            }
        });
        return doubleCacheConfigurationMap;
    }

    /**
     * 用户设定的默认caffeine配置检查
     *
     * @param defaultCustomCaffeineCacheConfig 用户设定的默认配置
     * @param useL1Cache                       是否开启一级缓存
     * @return 配置信息
     */
    private static CaffeineCacheConfiguration checkDefaultCaffeineCacheConfig(CaffeineCacheConfiguration defaultCustomCaffeineCacheConfig, boolean useL1Cache) {
        if (useL1Cache) {
            //开启一级缓存，没有手动设置默认配置，则使用默认配置
            if (defaultCustomCaffeineCacheConfig == null) {
                return CaffeineCacheConfiguration.defaultCacheConfig();
            }
        }
        return defaultCustomCaffeineCacheConfig;
    }

    /**
     * 用户设定的默认redis配置检查
     *
     * @param defaultCustomRedisCacheConfig 用户设定的默认配置
     * @return 配置信息
     */
    private static RedisCacheConfiguration checkDefaultRedisCacheConfig(RedisCacheConfiguration defaultCustomRedisCacheConfig) {
        return defaultCustomRedisCacheConfig == null ? RedisCacheConfiguration.defaultCacheConfig() :
                defaultCustomRedisCacheConfig;
    }
}

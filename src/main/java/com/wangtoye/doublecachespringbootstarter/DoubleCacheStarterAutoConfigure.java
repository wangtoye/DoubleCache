package com.wangtoye.doublecachespringbootstarter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.wangtoye.doublecachespringbootstarter.cache.manager.DoubleCacheManager;
import com.wangtoye.doublecachespringbootstarter.cache.writer.RedisCallbackCacheWriter;
import com.wangtoye.doublecachespringbootstarter.configuration.CaffeineCacheConfiguration;
import com.wangtoye.doublecachespringbootstarter.configuration.DoubleCacheConfiguration;
import com.wangtoye.doublecachespringbootstarter.listener.DoubleCacheMessageListener;
import com.wangtoye.doublecachespringbootstarter.properties.DoubleCacheProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * 双缓存默认配置
 *
 * @author wangtoye
 * @date 2019-12-12
 * Description:
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({Caffeine.class, RedisConnectionFactory.class})
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@EnableConfigurationProperties(DoubleCacheProperties.class)
public class DoubleCacheStarterAutoConfigure {

    /**
     * 使用Spring CacheManager管理缓存
     *
     * @return RedisCacheManager 配置
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public DoubleCacheManager cacheManager(
            RedisConnectionFactory connectionFactory, DoubleCacheProperties doubleCacheProperties,
            RedisTemplate redisTemplate) {
        //自定义redis缓存操作类-支持监听失效事件
        RedisCacheWriter redisCacheWriter = new RedisCallbackCacheWriter(connectionFactory, Duration.ofMillis(50),
                redisTemplate);
        //构造双缓存配置
        Map<String, DoubleCacheConfiguration> doubleCacheConfigurationMap =
                buildDoubleCacheConfigurationMap(doubleCacheProperties);

        return new DoubleCacheManager(redisCacheWriter,
                doubleCacheConfigurationMap,
                doubleCacheProperties.isAllowNullValues(), doubleCacheProperties.isUseL1Cache(),
                doubleCacheProperties.getTopic());
    }

    /**
     * 设置监听器容器
     *
     * @param redisTemplate         redis模板
     * @param doubleCacheManager    双缓存管理类
     * @param doubleCacheProperties 双缓存配置文件
     * @return 监听器容器
     */
    @Bean
    @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisTemplate<String, Object> redisTemplate,
                                                                       DoubleCacheManager doubleCacheManager,
                                                                       DoubleCacheProperties doubleCacheProperties) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(Objects.requireNonNull(redisTemplate.getConnectionFactory()));
        DoubleCacheMessageListener doubleCacheMessageListener =
                new DoubleCacheMessageListener(redisTemplate, doubleCacheManager);
        redisMessageListenerContainer.addMessageListener(doubleCacheMessageListener,
                new ChannelTopic(doubleCacheProperties.getTopic()));
        return redisMessageListenerContainer;
    }

    /**
     * 构造doubleCacheConfiguration
     *
     * @param doubleCacheProperties 自定义的配置
     * @return doubleCacheConfiguration map
     */
    private Map<String, DoubleCacheConfiguration> buildDoubleCacheConfigurationMap(DoubleCacheProperties doubleCacheProperties) {
        // 默认配置,这边可以自定义
        DoubleCacheConfiguration doubleCacheConfiguration = DoubleCacheConfiguration.defaultCacheConfig();
        CaffeineCacheConfiguration defaultCaffeineCacheConfig =
                doubleCacheConfiguration.getCaffeineCacheConfiguration();
        RedisCacheConfiguration defaultRedisCacheConfig = doubleCacheConfiguration.getRedisCacheConfiguration();
        return DoubleCacheConfiguration.buildDoubleCacheConfigurationMap(defaultCaffeineCacheConfig,
                defaultRedisCacheConfig, doubleCacheProperties);
    }
}

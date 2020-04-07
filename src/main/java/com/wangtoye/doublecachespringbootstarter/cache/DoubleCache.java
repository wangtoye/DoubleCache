package com.wangtoye.doublecachespringbootstarter.cache;


import com.github.benmanes.caffeine.cache.Cache;
import com.wangtoye.doublecachespringbootstarter.listener.DoubleCacheMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.concurrent.Callable;

/**
 * 使用caffeine作为一级缓存 redis作为二级缓存
 *
 * @author wangtoye
 * @date 2019-12-04
 * Description:
 */
public class DoubleCache extends AbstractValueAdaptingCache {
    private static final Logger logger = LoggerFactory.getLogger(DoubleCache.class);

    private String cacheName;
    private boolean useL1Cache;
    private CaffeineCache caffeineCache;
    private RedisCache redisCache;
    private String topic;

    /**
     * @param cacheName           缓存名
     * @param caffeineCacheWriter caffeine操作缓存的工具
     * @param redisCache          redis操作缓存的工具
     * @param allowNullValues     是否允许存储null值
     */
    public DoubleCache(String cacheName, Cache<Object, Object> caffeineCacheWriter, RedisCache redisCache,
                       boolean allowNullValues, String topic) {
        super(allowNullValues);
        this.cacheName = cacheName;
        if (caffeineCacheWriter == null) {
            this.useL1Cache = false;
        } else {
            this.useL1Cache = true;
            this.caffeineCache = new CaffeineCache(cacheName, caffeineCacheWriter, allowNullValues);
        }
        this.redisCache = redisCache;
        this.topic = topic;
    }

    /**
     * 获取缓存名称
     *
     * @return cacheName
     */
    @Override
    public String getName() {
        return this.cacheName;
    }

    /**
     * 获取真正的缓存操作
     * 对于caffeineCache是 {@link Cache}
     * 对于redisCache是 {@link RedisCacheWriter}
     * 所以这个函数没有太多意义，只是开放了一个直接操作原始缓存的口子
     *
     * @return 真是缓存
     */
    @Override
    public Object getNativeCache() {
        //对于聚合一二级缓存的本类来说，真实缓存的操作只有本类，所以返回this
        return this;
    }


    /**
     * 从缓存中获取 key 对应的值（包含在一个 ValueWrapper 中）
     *
     * @param key 键
     * @return ValueWrapper（值）
     */
    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper wrapper;
        if (useL1Cache) {
            logger.info("从一级缓存获取{}的值", key);
            wrapper = caffeineCache.get(key);
            if (wrapper != null) {
                return wrapper;
            }
        }
        logger.info("从二级缓存获取{}的值", key);
        return redisCache.get(key);
    }

    /**
     * 从缓存中获取 key 对应的指定类型的值（4.0版本新增）
     *
     * @param key  键
     * @param type 类型
     * @param <T>  类型
     * @return 指定类型的数据
     */
    @Override
    public <T> T get(Object key, Class<T> type) {
        T value;
        if (useL1Cache) {
            logger.info("从一级缓存获取{}的值", key);
            value = caffeineCache.get(key, type);
            if (value != null) {
                return value;
            }
        }
        logger.info("从二级缓存获取{}的值", key);
        return redisCache.get(key, type);
    }

    /**
     * 从缓存中获取 key 对应的值，如果缓存没有命中，则添加缓存，
     * 此时可异步地从 valueLoader 中获取对应的值（4.3版本新增）
     *
     * @param key         键
     * @param valueLoader 异步获取值
     * @param <T>         类型
     * @return 返回指定类型的值
     */
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        T value;
        if (useL1Cache) {
            logger.info("从一级缓存获取{}的值", key);
            value = caffeineCache.get(key, valueLoader);
            if (value != null) {
                return value;
            }
        }
        logger.info("从二级缓存获取{}的值", key);
        return redisCache.get(key, valueLoader);
    }


    /**
     * 和get方法类似，不过是返回object类型的缓存数据
     *
     * @param key 键
     * @return object 值
     */
    @Override
    protected Object lookup(Object key) {
        Object value;
        if (useL1Cache) {
            logger.info("从一级缓存获取{}的值", key);
            value = caffeineCache.get(key);
            if (value != null) {
                return value;
            }
        }
        logger.info("从二级缓存获取{}的值", key);
        return redisCache.get(key);
    }

    /**
     * 添加一二级缓存
     *
     * @param key   键
     * @param value 值
     */
    @Override
    public void put(Object key, Object value) {
        if (useL1Cache) {
            caffeineCache.put(key, value);
        }
        redisCache.put(key, value);

        //通知其他节点清空本地一级缓存
        push(new DoubleCacheMessage(this.cacheName, key));
    }

    /**
     * 缓存 key-value，如果缓存中已经有对应的 key，则返回已有的 value，不做替换
     *
     * @param key   键
     * @param value 值
     * @return ValueWrapper（值）
     */
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        if (useL1Cache) {
            //此处只更新L1，但是不会把L1的结果返回，因为L2也需要操作，所以直接返回L2的结果，L1只做一次调用
            caffeineCache.putIfAbsent(key, value);
        }

        ValueWrapper wrapper = redisCache.putIfAbsent(key, value);

        //通知其他节点清空本地一级缓存
        push(new DoubleCacheMessage(this.cacheName, key));
        return wrapper;
    }

    /**
     * 删除指定key的缓存
     *
     * @param key 键
     */
    @Override
    public void evict(Object key) {
        //删除的时候要先删除L2再删除L1，否则有并发问题
        redisCache.evict(key);
        if (useL1Cache) {
            caffeineCache.evict(key);
        }

        //通知其他节点清空本地一级缓存
        push(new DoubleCacheMessage(this.cacheName, key));
    }

    /**
     * 清空缓存
     */
    @Override
    public void clear() {
        //清空的时候要先清空L2再清空L1，否则有并发问题
        redisCache.clear();
        if (useL1Cache) {
            caffeineCache.clear();
        }

        //通知其他节点清空本地一级缓存
        push(new DoubleCacheMessage(this.cacheName, null));
    }

    /**
     * 推送消息给订阅的系统
     *
     * @param message 消息
     */
    private void push(DoubleCacheMessage message) {
        ((RedisCacheExt)redisCache).convertAndSend(topic, message);
    }

    /**
     * 清理一级缓存
     *
     * @param key 键
     */
    public void clearL1Cache(Object key) {
        if (key == null) {
            caffeineCache.clear();
        } else {
            caffeineCache.evict(key);
        }
    }
}

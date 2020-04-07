package com.wangtoye.doublecachespringbootstarter.cache.loader;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.redis.cache.RedisCache;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 *
 * @author wangtoye
 * @date 2019-12-10
 * Description:
 */
public class DoubleCacheLoader implements CacheLoader<Object, Object> {

    private RedisCache redisCache;

    public DoubleCacheLoader(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    /**
     * 如果值没有过期，则不会调用这个函数，如果过期，则会调用这个函数去load一份新缓存
     * 因为集成了redis，所以可以交给redis去获取缓存，而此处直接返回null即可
     *
     * @param key 键
     * @return 返回值
     */
    @Nullable
    @Override
    public Object load(@NonNull Object key) {
        System.out.println(key);
        return null;
    }

    /**
     * 如果配置refreshAfterWrite则需要这个方法
     *
     * @param key      键
     * @param oldValue 旧值
     * @return 值
     */
    @Nullable
    @Override
    public Object reload(@NonNull Object key, @NonNull Object oldValue) {
        System.out.println(key);
        System.out.println(oldValue);
        return Objects.requireNonNull(redisCache.get(key)).get();
    }
}

package com.wangtoye.doublecachespringbootstarter.cache;

/**
 * @author wangtoye
 * @date 2020/4/2
 * @description
 */
public interface RedisCacheExt {

    /**
     * 推送消息给订阅的系统
     *
     * @param topicName topicName
     * @param message   键
     */
    void convertAndSend(String topicName, Object message);
}

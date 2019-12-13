package com.wangtoye.doublecachespringbootstarter.listener;

import com.wangtoye.doublecachespringbootstarter.cache.manager.DoubleCacheManager;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 *
 * @author wangtoye
 * @date 2019-12-11
 * Description:
 */
@Data
public class DoubleCacheMessage implements Serializable {
    private String cacheName;
    private Object key;
    private String uuid;

    public DoubleCacheMessage() {
    }

    public DoubleCacheMessage(String cacheName, Object key) {
        this.cacheName = cacheName;
        this.key = key;
        //自动创建一个uuid，防止订阅到自己发布的消息
        this.uuid = UUID.randomUUID().toString();
        DoubleCacheManager.uuidList.add(this.uuid);
    }
}

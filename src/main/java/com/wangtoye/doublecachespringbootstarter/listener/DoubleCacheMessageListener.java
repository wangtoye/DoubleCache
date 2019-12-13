package com.wangtoye.doublecachespringbootstarter.listener;

import com.wangtoye.doublecachespringbootstarter.cache.manager.DoubleCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

/**
 * 通知节点同步数据
 *
 * @author wangtoye
 * @date 2019-12-11
 * Description:
 */
public class DoubleCacheMessageListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(DoubleCacheMessageListener.class);

    private RedisTemplate<String, Object> redisTemplate;
    private DoubleCacheManager doubleCacheManager;

    public DoubleCacheMessageListener(RedisTemplate<String, Object> redisTemplate,
                                      DoubleCacheManager doubleCacheManager) {
        this.redisTemplate = redisTemplate;
        this.doubleCacheManager = doubleCacheManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        DoubleCacheMessage doubleCacheMessage =
                (DoubleCacheMessage) redisTemplate.getValueSerializer().deserialize(message.getBody());
        if (doubleCacheMessage != null) {
            logger.info("接受到订阅的消息，内容是{}", doubleCacheMessage.toString());
            String uuid = doubleCacheMessage.getUuid();
            if (!StringUtils.isEmpty(uuid) && DoubleCacheManager.uuidList.contains(uuid)) {
                DoubleCacheManager.uuidList.remove(uuid);
                logger.info("接受到当前节点发布的信息，不需要清除一级缓存");
            } else {
                logger.info("开始清除一级缓存");
                doubleCacheManager.clearL1Cache(doubleCacheMessage.getCacheName(), doubleCacheMessage.getKey());
            }
        }
    }
}

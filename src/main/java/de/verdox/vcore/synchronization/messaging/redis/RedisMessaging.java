/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcore.synchronization.messaging.redis;

import de.verdox.vcore.synchronization.messaging.MessagingService;
import de.verdox.vcore.synchronization.messaging.event.MessageEvent;
import de.verdox.vcore.synchronization.messaging.messages.Message;
import de.verdox.vcore.plugin.VCorePlugin;
import de.verdox.vcore.synchronization.redisson.RedisConnection;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.redisson.codec.SerializationCodec;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 25.06.2021 22:09
 */
public class RedisMessaging extends RedisConnection implements MessagingService<RedisMessageBuilder> {
    private final RTopic rTopic;
    private final MessageListener<Message> messageListener;
    private boolean loaded;

    public RedisMessaging(@Nonnull VCorePlugin<?, ?> plugin, boolean clusterMode, @Nonnull String[] addressArray, String redisPassword) {
        super(plugin, clusterMode, addressArray, redisPassword);
        rTopic = redissonClient.getTopic("VCoreMessagingChannel", new SerializationCodec());
        this.messageListener = (channel, msg) -> {
            if(!(msg instanceof SimpleRedisMessage))
                return;
            plugin.getServices().eventBus.post(new MessageEvent(msg));
        };
        rTopic.addListener(Message.class, messageListener);
        loaded = true;
    }

    @Override
    public RedisMessageBuilder constructMessage() {
        return new RedisMessageBuilder(getSessionUUID(), getSenderName());
    }

    @Override
    public void publishMessage(Message message) {
        rTopic.publish(message);
    }

    @Override
    public String getSenderName() {
        return plugin.getPluginName();
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void shutdown() {
        plugin.consoleMessage("&eShutting down Redis Messenger",false);
        rTopic.removeListener(messageListener);
        plugin.consoleMessage("&eRedis Messenger shut down successfully",false);
    }
}

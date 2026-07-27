package com.biometitle;

import com.biometitle.listener.ChunkBiomeListener;
import com.biometitle.listener.ConfigRegistryListener;
import com.biometitle.listener.PlayerMoveBiomeTracker;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "biometitle",
    name = "BiomeTitle",
    version = "1.0.0",
    description = "Show biome titles when crossing biome boundaries",
    authors = {"windy664"}
)
public class BiomeTitlePlugin {

    private final ProxyServer server;
    private final PluginContainer pluginContainer;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public BiomeTitlePlugin(ProxyServer server, PluginContainer pluginContainer,
                            Logger logger, @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory) {
        this.server = server;
        this.pluginContainer = pluginContainer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Load config
        BiomeTitleConfig config = new BiomeTitleConfig();
        config.load(dataDirectory);
        TitleSender.setConfig(config);

        // packetevents: 复用已有的，或自己初始化
        // VelocityPacketEventsBuilder.build() 内部有缓存，
        // 如果其他插件（VelocityHologram/XingtuBot）已调用过，这里返回同一个实例
        PacketEvents.setAPI(
            VelocityPacketEventsBuilder.build(server, pluginContainer, logger, dataDirectory)
        );
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().init();

        // 注册监听器（幂等，不会重复注册）
        PacketEvents.getAPI().getEventManager().registerListener(
            new ChunkBiomeListener(), PacketListenerPriority.NORMAL
        );
        PacketEvents.getAPI().getEventManager().registerListener(
            new PlayerMoveBiomeTracker(), PacketListenerPriority.NORMAL
        );
        // 拦截 REGISTRY_DATA 配置包，获取服务器的群系注册表（绕过 packetevents 静态映射）
        PacketEvents.getAPI().getEventManager().registerListener(
            new ConfigRegistryListener(), PacketListenerPriority.NORMAL
        );

        logger.info("BiomeTitle initialized");
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        BiomeCache.remove(event.getPlayer().getUniqueId());
        ServerBiomeRegistry.remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        BiomeCache.remove(event.getPlayer().getUniqueId());
    }
}

package com.biometitle.listener;

import com.biometitle.ServerBiomeRegistry;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerRegistryData;

/**
 * 拦截 Configuration 阶段的 REGISTRY_DATA 包，
 * 提取 minecraft:worldgen/biome 注册表，建立服务器原生的 index→biome 映射。
 */
public final class ConfigRegistryListener implements PacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Configuration.Server.REGISTRY_DATA) {
            WrapperConfigServerRegistryData packet = new WrapperConfigServerRegistryData(event);
            ServerBiomeRegistry.handleRegistryData(event.getUser().getUUID(), packet);
        }
    }
}

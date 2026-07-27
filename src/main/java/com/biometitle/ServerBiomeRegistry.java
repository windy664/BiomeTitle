package com.biometitle;

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerRegistryData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拦截服务器发的 REGISTRY_DATA 配置包，
 * 从 minecraft:worldgen/biome 注册表中建立 index→ResourceLocation 映射。
 * 彻底绕过 packetevents 的静态 biome ID 映射（对 26.2 不准）。
 */
public final class ServerBiomeRegistry {

    /** 每个玩家的群系注册表：server-assigned index → biome ResourceLocation */
    private static final Map<UUID, List<ResourceLocation>> PLAYER_REGISTRIES = new ConcurrentHashMap<>();

    private ServerBiomeRegistry() {}

    /**
     * 处理 REGISTRY_DATA 包。如果是群系注册表，提取所有 biome 名称（按服务器分配的顺序 = 数字ID）。
     */
    public static void handleRegistryData(UUID uuid, WrapperConfigServerRegistryData packet) {
        ResourceLocation registryKey = packet.getRegistryKey();
        if (!"minecraft".equals(registryKey.getNamespace()) || !"worldgen/biome".equals(registryKey.getKey())) {
            return;
        }

        List<WrapperConfigServerRegistryData.RegistryElement> elements = packet.getElements();
        List<ResourceLocation> biomeList = new ArrayList<>(elements.size());

        for (WrapperConfigServerRegistryData.RegistryElement element : elements) {
            // element.getId() 就是 biome 的 ResourceLocation，如 minecraft:plains
            biomeList.add(element.getId());
        }

        PLAYER_REGISTRIES.put(uuid, biomeList);
    }

    /**
     * 用服务器注册表解析 biome index → ResourceLocation。
     * 如果该玩家没有收到过注册表数据，返回 null（回退到旧逻辑）。
     */
    public static ResourceLocation getBiome(UUID uuid, int biomeIndex) {
        List<ResourceLocation> registry = PLAYER_REGISTRIES.get(uuid);
        if (registry == null || biomeIndex < 0 || biomeIndex >= registry.size()) {
            return null;
        }
        return registry.get(biomeIndex);
    }

    public static void remove(UUID uuid) {
        PLAYER_REGISTRIES.remove(uuid);
    }
}

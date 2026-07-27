package com.biometitle;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.biome.Biome;
import com.github.retrooper.packetevents.protocol.world.biome.Biomes;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.SingletonPalette;
import com.github.retrooper.packetevents.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player biome cache.
 *
 * 1.18+：从 section 的 biome palette 懒解析（per-section biome）
 * 1.8-1.17：从 Column 的列级 biome 数组读取（per-column biome）
 */
public final class BiomeCache {

    private static final Map<UUID, PlayerBiomeData> DATA = new ConcurrentHashMap<>();

    private BiomeCache() {}

    public static PlayerBiomeData getOrCreate(UUID uuid) {
        return DATA.computeIfAbsent(uuid, PlayerBiomeData::new);
    }

    public static void remove(UUID uuid) {
        DATA.remove(uuid);
    }

    /**
     * 根据区块坐标查找 biome 的 ResourceLocation。
     * 优先用服务器发来的注册表（REGISTRY_DATA），绕过 packetevents 静态映射；
     * 没有服务器注册表时回退到 Biomes.getRegistry().getById()。
     *
     * @return biome 的 ResourceLocation（如 minecraft:plains），找不到返回 null
     */
    public static ResourceLocation getBiomeAtChunk(PlayerBiomeData data, int chunkX, int chunkZ,
                                                    ClientVersion version) {
        // ── 1.18+：per-section biome palette ──
        for (int i = 4; i >= 0; i--) {
            long key = encodeKey(chunkX, chunkZ, i);
            BaseChunk raw = data.rawSections.get(key);
            if (raw instanceof com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18) {
                DataPalette biomePalette =
                    ((com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18) raw).getBiomeData();
                int biomeId;
                if (biomePalette.palette instanceof SingletonPalette) {
                    biomeId = biomePalette.palette.idToState(0);
                } else {
                    int paletteIdx = biomePalette.get(2, 0, 2);
                    biomeId = biomePalette.palette.idToState(paletteIdx);
                }

                // 优先：用服务器发来的注册表解析
                ResourceLocation fromServer = ServerBiomeRegistry.getBiome(data.uuid, biomeId);
                if (fromServer != null) {
                    return fromServer;
                }

                // 回退：packetevents 静态映射（26.2 可能不准）
                Biome biome = Biomes.getRegistry().getById(version, biomeId);
                if (biome != null) {
                    return biome.getName();
                }
                return null;
            }
        }

        // ── 1.8-1.17：per-column biome（从 Column 缓存读取）──
        Column column = data.columns.get(chunkColumnKey(chunkX, chunkZ));
        if (column == null || !column.hasBiomeData()) {
            return null;
        }
        return getColumnBiome(column, version);
    }

    /**
     * 从 Column 的列级 biome 数据中读取中心点 biome。
     * 1.15+ 用 int[]（全局 biome ID），1.8-1.14 用 byte[]。
     */
    private static ResourceLocation getColumnBiome(Column column, ClientVersion version) {
        int centerX = 8, centerZ = 8; // 区块中心 (8,8)
        int index = centerZ * 16 + centerX;

        int biomeId;
        int[] ints = column.getBiomeDataInts();
        if (ints != null && ints.length > index) {
            biomeId = ints[index]; // 1.15+：全局 biome registry ID
        } else {
            byte[] bytes = column.getBiomeDataBytes();
            if (bytes == null || bytes.length <= index) {
                return null;
            }
            biomeId = bytes[index] & 0xFF; // 1.8-1.14：byte biome ID
        }

        Biome biome = Biomes.getRegistry().getById(version, biomeId);
        return biome != null ? biome.getName() : null;
    }

    /** 用于在 columns map 中索引的 key */
    public static long chunkColumnKey(int chunkX, int chunkZ) {
        return (long) chunkX & 0xFFFFFFFFL | ((long) chunkZ & 0xFFFFFFFFL) << 32;
    }

    public static long encodeKey(int chunkX, int chunkZ, int sectionY) {
        return (long) (chunkX & 0x3FFFFF)
                | ((long) (chunkZ & 0x3FFFFF) << 22)
                | ((long) (sectionY + 4) << 44);
    }

    public static final class PlayerBiomeData {
        /** 玩家 UUID，用于查 ServerBiomeRegistry */
        public final UUID uuid;
        /** 1.18+：原始 section 引用 */
        public final Map<Long, BaseChunk> rawSections = new ConcurrentHashMap<>();
        /** 1.8-1.17：列级 Column 引用 */
        public final Map<Long, Column> columns = new ConcurrentHashMap<>();

        public volatile int lastChunkX = Integer.MIN_VALUE;
        public volatile int lastChunkZ = Integer.MIN_VALUE;
        public volatile String lastBiomeKey = null;
        public final Set<String> shownBiomes = ConcurrentHashMap.newKeySet();
        public volatile long lastCheckTime = 0L;

        PlayerBiomeData(UUID uuid) {
            this.uuid = uuid;
        }
    }
}

package com.biometitle.listener;

import com.biometitle.BiomeCache;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;

import java.util.UUID;

/**
 * 只负责缓存区块数据，不做 biome 判断。
 * biome 检测交给 PlayerMoveBiomeTracker（基于玩家真实坐标）。
 */
public final class ChunkBiomeListener implements PacketListener {

    private static final int SECTION_COUNT = 24;

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            handleChunkData(event);
        } else if (event.getPacketType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            handleUnloadChunk(event);
        }
    }

    private void handleChunkData(PacketSendEvent event) {
        UUID uuid = event.getUser().getUUID();
        WrapperPlayServerChunkData wrapper = new WrapperPlayServerChunkData(event);
        Column column = wrapper.getColumn();
        int chunkX = column.getX();
        int chunkZ = column.getZ();

        BaseChunk[] chunks = column.getChunks();
        BiomeCache.PlayerBiomeData data = BiomeCache.getOrCreate(uuid);

        // 1.18+：存 section 引用
        for (int i = 0; i < chunks.length && i < SECTION_COUNT; i++) {
            long key = BiomeCache.encodeKey(chunkX, chunkZ, i);
            data.rawSections.put(key, chunks[i]);
        }

        // 1.8-1.17：存 Column 引用（列级 biome 数据）
        if (column.hasBiomeData()) {
            data.columns.put(BiomeCache.chunkColumnKey(chunkX, chunkZ), column);
        }
    }

    private void handleUnloadChunk(PacketSendEvent event) {
        UUID uuid = event.getUser().getUUID();
        WrapperPlayServerUnloadChunk wrapper = new WrapperPlayServerUnloadChunk(event);
        BiomeCache.PlayerBiomeData data = BiomeCache.getOrCreate(uuid);

        int chunkX = wrapper.getChunkX();
        int chunkZ = wrapper.getChunkZ();
        for (int i = 0; i < SECTION_COUNT; i++) {
            data.rawSections.remove(BiomeCache.encodeKey(chunkX, chunkZ, i));
        }
        data.columns.remove(BiomeCache.chunkColumnKey(chunkX, chunkZ));
    }
}

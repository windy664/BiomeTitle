package com.biometitle.listener;

import com.biometitle.BiomeCache;
import com.biometitle.TitleSender;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * 基于玩家真实坐标检测 biome 变化。
 *
 * 性能优化：
 * - chunk 级过滤：同区块直接 return，绝大部分 20Hz 包走快速路径
 * - 500ms 冷却：跨区块边界抖动时不会频繁触发检测
 * - shownBiomes 去重：同一群系只弹一次
 */
public final class PlayerMoveBiomeTracker implements PacketListener {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger("BiomeTitle");
    /** 跨区块 biome 检查的最小间隔（毫秒） */
    private static final long COOLDOWN_MS = 500L;

    // ── C→S: 正常行走 ──────────────────────────────────────────────
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            WrapperPlayClientPlayerPosition pkt = new WrapperPlayClientPlayerPosition(event);
            handleMove(event, pkt.getPosition().getX(), pkt.getPosition().getZ());
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerPositionAndRotation pkt = new WrapperPlayClientPlayerPositionAndRotation(event);
            handleMove(event, pkt.getPosition().getX(), pkt.getPosition().getZ());
        }
    }

    // ── S→C: 传送/重生 ─────────────────────────────────────────────
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            WrapperPlayServerPlayerPositionAndLook pkt = new WrapperPlayServerPlayerPositionAndLook(event);
            handleMove(event, pkt.getX(), pkt.getZ());
        }
    }

    // ── 统一处理 ───────────────────────────────────────────────────
    private void handleMove(ProtocolPacketEvent event, double x, double z) {
        BiomeCache.PlayerBiomeData data = BiomeCache.getOrCreate(event.getUser().getUUID());
        int chunkX = ((int) Math.floor(x)) >> 4;
        int chunkZ = ((int) Math.floor(z)) >> 4;

        // 同区块 → 跳过（快速路径，覆盖 99%+ 的包）
        if (chunkX == data.lastChunkX && chunkZ == data.lastChunkZ) {
            return;
        }

        // 跨区块但在冷却期内 → 跳过（防边界抖动）
        long now = System.currentTimeMillis();
        if (now - data.lastCheckTime < COOLDOWN_MS) {
            return;
        }

        data.lastChunkX = chunkX;
        data.lastChunkZ = chunkZ;
        data.lastCheckTime = now;

        ClientVersion version = event.getUser().getClientVersion();
        ResourceLocation biome = BiomeCache.getBiomeAtChunk(data, chunkX, chunkZ, version);
        if (biome == null) {
            return;
        }

        String biomeKey = biome.toString();
        if (biomeKey.equals(data.lastBiomeKey)) {
            return;
        }

        data.lastBiomeKey = biomeKey;

        // 同一群系只弹一次标题
        if (!data.shownBiomes.add(biomeKey)) {
            return;
        }

        String translationKey = "biome." + biome.getNamespace() + "." + biome.getKey();
        log.info("[BiomeTitle] {} → {} ({})", event.getUser().getUUID(), translationKey, biomeKey);
        TitleSender.sendBiomeTitle(event.getUser(), translationKey);
    }
}

package com.biometitle;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleSubtitle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleText;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleTimes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TitleSender {

    private static final Logger log = LoggerFactory.getLogger("BiomeTitle");
    private static BiomeTitleConfig config;

    private TitleSender() {}

    public static void setConfig(BiomeTitleConfig cfg) {
        config = cfg;
    }

    /**
     * Show biome title with decorative styling.
     * @param translationKey "biome.minecraft.plains" — client auto-translates
     */
    public static void sendBiomeTitle(User user, String translationKey) {
        if (config != null && config.isBlacklisted(translationKey)) {
            return;
        }

        TextColor titleColor = TextColor.color(config != null ? config.titleColor : 0xFFAA00);
        TextColor dividerColor = TextColor.color(config != null ? config.dividerColor : 0x888888);
        int fadeIn = config != null ? config.fadeIn : 15;
        int stay = config != null ? config.stay : 50;
        int fadeOut = config != null ? config.fadeOut : 20;

        // Main title: biome name in gold, bold, no italic
        Component title = Component.translatable(translationKey)
            .color(titleColor)
            .decorate(TextDecoration.BOLD)
            .style(Style.style().decoration(TextDecoration.ITALIC, false).build());

        // Subtitle: decorative divider
        Component subtitle = Component.empty();
        if (config == null || config.showDivider) {
            String dividerStr = config != null ? config.dividerText : "───── ✦ ─────";
            subtitle = Component.text(dividerStr)
                .color(dividerColor)
                .style(Style.style().decoration(TextDecoration.ITALIC, false).build());
        }

        // Send: times → subtitle → title
        user.sendPacket(new WrapperPlayServerSetTitleTimes(fadeIn, stay, fadeOut));
        user.sendPacket(new WrapperPlayServerSetTitleSubtitle(subtitle));
        user.sendPacket(new WrapperPlayServerSetTitleText(title));

        log.info("[BiomeTitle] sent title: {} → {}", user.getUUID(), translationKey);
    }
}

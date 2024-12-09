package cn.paper_card.qq_group_chat_sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

class SenderParser {

    private final @NotNull JsonObject senderObj;

    private final OfflinePlayer offlinePlayer;
    private final Player onlinePlayer;

    private final String card;
    private final String nickname;

    private final long userId;

    SenderParser(@NotNull JsonObject dataObj, @NotNull Server server) {
        this.senderObj = dataObj.get("sender").getAsJsonObject();

        var uuid = this.parseMcUuid();
        if (uuid != null) {
            this.offlinePlayer = server.getOfflinePlayer(uuid);
            this.onlinePlayer = this.offlinePlayer.getPlayer();
        } else {
            this.offlinePlayer = null;
            this.onlinePlayer = null;
        }

        // 还有role其实，不过用不上

        this.card = this.senderObj.get("card").getAsString();
        this.nickname = this.senderObj.get("nickname").getAsString();
        this.userId = this.senderObj.get("user_id").getAsLong();
    }

    @NotNull String displayName() {

        if (this.offlinePlayer != null) {
            final String name = this.offlinePlayer.getName();
            if (name != null) return name;
        }

        if (!"".equals(this.card)) return this.card;

        if (!"".equals(this.nickname)) return this.nickname;

        return "%d".formatted(this.userId);
    }

    @NotNull Component displayComponent() {

        if (this.onlinePlayer != null) {
            return this.onlinePlayer.displayName();
        }

        // 不在线

        String name = null;
        if (this.offlinePlayer != null) {
            name = this.offlinePlayer.getName();
        }

        if (name == null) {
            name = this.card; // 群名片

            if ("".equals(name)) {
                name = this.nickname; // QQ昵称

                if ("".equals(name)) {
                    name = "%d".formatted(this.userId); // QQ号码
                }
            }
        }

        return Component.text(name).color(NamedTextColor.GRAY);
    }

    private @NotNull TextComponent hoverComponent() {
        final TextComponent.Builder text = Component.text();

        text.append(Component.text("QQ: %d".formatted(this.userId)));

        text.appendNewline();
        text.append(Component.text("群名片: %s".formatted(this.card)));

        text.appendNewline();
        text.append(Component.text("昵称: %s".formatted(this.nickname)));

        if (this.offlinePlayer == null) {
            text.appendNewline();
            text.append(Component.text("未绑定MC"));
        } else {
            text.appendNewline();
            text.append(Component.text("已绑定MC: %s".formatted(this.offlinePlayer.getName())));
        }

        return text.build();
    }

    @NotNull TextComponent component() {
        final TextComponent.Builder text = Component.text();
        text.append(Component.text("<"));
        text.append(this.displayComponent().hoverEvent(HoverEvent.showText(this.hoverComponent())));
        text.append(Component.text(">"));
        return text.build();
    }

    private @Nullable UUID parseMcUuid() {
        final JsonElement ele = this.senderObj.get("mc_uuid");
        if (ele == null || ele.isJsonNull()) {
            return null;
        }
        final String str = ele.getAsString();
        if ("".equals(str)) return null;

        return UUID.fromString(ele.getAsString());
    }

    @Nullable Player getOnlinePlayer() {
        return this.onlinePlayer;
    }

    @Nullable OfflinePlayer getOfflinePlayer() {
        return this.offlinePlayer;
    }
}
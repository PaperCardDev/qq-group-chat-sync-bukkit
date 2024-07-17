package cn.paper_card.qq_group_chat_sync;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

class ConfigManager {

    private final @NotNull ThePlugin plugin;

    ConfigManager(@NotNull ThePlugin plugin) {
        this.plugin = plugin;
    }

    @NotNull String getGameChatPrefix() {

        final String path = "game-chat-prefix";
        final String def = "";

        final FileConfiguration c = this.plugin.getConfig();

        if (!c.contains(path, true)) {
            c.set(path, def);
            c.setComments(path, Collections.singletonList("游戏内聊天消息以什么开头会同步到群内，默认值是空字符串，也就是全部消息，推荐设置为#"));
        }

        return c.getString(path, def);
    }

    void getAll() {
        this.getGameChatPrefix();
    }


    void save() {
        this.plugin.saveConfig();
    }

    void reload() {
        this.plugin.reloadConfig();
    }
}

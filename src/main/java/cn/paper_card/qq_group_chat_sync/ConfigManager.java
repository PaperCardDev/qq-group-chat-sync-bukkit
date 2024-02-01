package cn.paper_card.qq_group_chat_sync;

import org.jetbrains.annotations.NotNull;

class ConfigManager {

    private final @NotNull ThePlugin plugin;

    private final static String PATH_GAME_CHAT_PREFIX = "game-chat-prefix";

    ConfigManager(@NotNull ThePlugin plugin) {
        this.plugin = plugin;
    }

    @NotNull String getGameChatPrefix() {
        return this.plugin.getConfig().getString(PATH_GAME_CHAT_PREFIX, "");
    }

    void setGameChatPrefix(@NotNull String v) {
        this.plugin.getConfig().set(PATH_GAME_CHAT_PREFIX, v);
    }


    void setDefaults() {
        this.setGameChatPrefix(this.getGameChatPrefix());
    }

    void save() {
        this.plugin.saveConfig();
    }

    void reload() {
        this.plugin.reloadConfig();
    }
}

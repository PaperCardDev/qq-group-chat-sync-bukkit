package cn.paper_card.qq_group_chat_sync;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

class PlayerSettingService implements Listener {

    private final @NotNull HashMap<UUID, PlayerSetting> settings;

    PlayerSettingService() {
        this.settings = new HashMap<>();
    }

    void register(@NotNull ThePlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @NotNull PlayerSetting getPlayerSetting(@NotNull Player player) {
        final UUID id = player.getUniqueId();
        synchronized (this.settings) {
            PlayerSetting s = this.settings.get(id);
            if (s != null) return s;
            s = new PlayerSetting();
            this.settings.put(id, s);
            return s;
        }
    }

    @EventHandler
    void onQuit(@NotNull PlayerQuitEvent event) {
        synchronized (this.settings) {
            this.settings.remove(event.getPlayer().getUniqueId());
        }
    }
}

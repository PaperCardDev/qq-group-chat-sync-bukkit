package cn.paper_card.qq_group_chat_sync;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

class MyPlayerService implements Listener {

    private final @NotNull HashMap<UUID, MyPlayer> cache;

    MyPlayerService() {
        this.cache = new HashMap<>();
    }

    void register(@NotNull ThePlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @NotNull MyPlayer getMyPlayer(@NotNull Player player) {
        final UUID id = player.getUniqueId();
        synchronized (this.cache) {
            MyPlayer s = this.cache.get(id);
            if (s != null) return s;
            s = new MyPlayer();
            this.cache.put(id, s);
            return s;
        }
    }

    @EventHandler
    void onQuit(@NotNull PlayerQuitEvent event) {
        synchronized (this.cache) {
            this.cache.remove(event.getPlayer().getUniqueId());
        }
    }
}

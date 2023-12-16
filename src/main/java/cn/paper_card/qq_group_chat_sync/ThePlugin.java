package cn.paper_card.qq_group_chat_sync;

import cn.paper_card.qq_bind.api.QqBindApi;
import cn.paper_card.qq_group_chat_sync.api.QqGroupChatSyncApi;
import cn.paper_card.qq_group_chat_sync.api.QqGroupMessageSender;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ThePlugin extends JavaPlugin implements Listener {

    private QqGroupChatSyncApiImpl qqGroupChatSyncApi = null;

    private QqBindApi qqBindApi = null;

    private final @NotNull TaskScheduler taskScheduler;

    public ThePlugin() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
    }

    private @NotNull String getSyncPrefix() {
        return this.getConfig().getString("sync-prefix", "#");
    }

    private void setSyncPrefix(@NotNull String prefix) {
        this.getConfig().set("sync-prefix", prefix);
    }

    @EventHandler
    public void onChat(@NotNull AsyncChatEvent event) {

        final QqGroupMessageSender sender = this.qqGroupChatSyncApi.getMessageSender();
        if (sender == null) return;

        if (!(event.message() instanceof final TextComponent message)) return;

        final String content = message.content();

        if (!content.startsWith(this.getSyncPrefix())) return;

        if (!(event.getPlayer().displayName() instanceof TextComponent displayName)) return;

        String name = displayName.content();
        if (name.isEmpty()) {
            final StringBuilder display = new StringBuilder();
            for (final Component child : displayName.children()) {
                if (child instanceof final TextComponent text) {
                    display.append(text.content());
                }
            }
            name = display.toString();
        }

        if (name.isEmpty()) name = event.getPlayer().getName();

        try {
            sender.sendNormal("<%s> %s".formatted(name, content));
        } catch (Exception e) {
            handleException("sendNormalMessage", e);
        }
    }

    void handleException(@NotNull String msg, @NotNull Throwable e) {
        this.getSLF4JLogger().error(msg, e);
    }

    @Override
    public void onLoad() {
        this.qqGroupChatSyncApi = new QqGroupChatSyncApiImpl(this);

        this.getSLF4JLogger().info("注册%s...".formatted(QqGroupChatSyncApi.class.getSimpleName()));
        this.getServer().getServicesManager().register(QqGroupChatSyncApi.class, this.qqGroupChatSyncApi, this, ServicePriority.Highest);
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        this.setSyncPrefix(this.getSyncPrefix());
        this.saveConfig();

        this.qqBindApi = this.getServer().getServicesManager().load(QqBindApi.class);
        if (this.qqBindApi != null) {
            this.getSLF4JLogger().info("已连接到" + QqBindApi.class.getSimpleName());
        } else {
            this.getSLF4JLogger().warn("无法连接到" + QqBindApi.class.getSimpleName());
        }
    }

    @Override
    public void onDisable() {
        this.getServer().getServicesManager().unregisterAll(this);
    }

    @Nullable QqBindApi getQqBindApi() {
        return this.qqBindApi;
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }
}

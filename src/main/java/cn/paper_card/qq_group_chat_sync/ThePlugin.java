package cn.paper_card.qq_group_chat_sync;

import cn.paper_card.qq_bind.api.QqBindApi;
import cn.paper_card.qq_group_chat_sync.api.QqGroupChatSyncApi;
import cn.paper_card.qq_group_chat_sync.api.QqGroupMessageSender;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ThePlugin extends JavaPlugin implements Listener {

    private QqGroupChatSyncApiImpl qqGroupChatSyncApi = null;

    private QqBindApi qqBindApi = null;

    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull ConfigManager configManager;

    private final @NotNull PlayerSettingService playerSettingService;

    public ThePlugin() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
        this.configManager = new ConfigManager(this);
        this.playerSettingService = new PlayerSettingService();
    }

    @EventHandler
    public void onChat(@NotNull AsyncChatEvent event) {

        final QqGroupMessageSender sender = this.qqGroupChatSyncApi.getMessageSender();
        if (sender == null) return;

        if (!(event.message() instanceof final TextComponent message)) return;

        final String content = message.content();

        if (!content.startsWith(this.configManager.getGameChatPrefix())) return;

        final Player player = event.getPlayer();

//        if (!(player.displayName() instanceof TextComponent displayName)) return;

        final String name = player.getName();
        /*
//        if (name.isEmpty()) {
//            final StringBuilder display = new StringBuilder();
//            for (final Component child : displayName.children()) {
//                if (child instanceof final TextComponent text) {
//                    display.append(text.content());
//                }
//            }
//            name = display.toString();
//        }
//
//        if (name.isEmpty()) name = event.getPlayer().getName();
         */

        try {
            sender.sendNormal(player.getUniqueId(), player.getName(), "<%s> %s".formatted(name, content));
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
        this.playerSettingService.register(this);

        this.configManager.setDefaults();
        this.configManager.save();

        this.qqBindApi = this.getServer().getServicesManager().load(QqBindApi.class);
        if (this.qqBindApi != null) {
            this.getSLF4JLogger().info("已连接到" + QqBindApi.class.getSimpleName());
        } else {
            this.getSLF4JLogger().warn("无法连接到" + QqBindApi.class.getSimpleName());
        }

        new MainCommand(this);
    }

    @Override
    public void onDisable() {
        this.getServer().getServicesManager().unregisterAll(this);
        this.taskScheduler.cancelTasks(this);
        this.configManager.save();
    }

    @NotNull Permission addPermission(@NotNull String name) {
        final Permission p = new Permission(name);
        this.getServer().getPluginManager().addPermission(p);
        return p;
    }

    @Nullable QqBindApi getQqBindApi() {
        return this.qqBindApi;
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull ConfigManager getConfigManager() {
        return this.configManager;
    }

    @NotNull PlayerSettingService getPlayerSettingService() {
        return this.playerSettingService;
    }

    void appendPrefix(@NotNull TextComponent.Builder text) {
        text.append(Component.text("[").color(NamedTextColor.GRAY));
        text.append(Component.text(this.getName()).color(NamedTextColor.DARK_AQUA));
        text.append(Component.text("]").color(NamedTextColor.GRAY));
    }

    void sendError(@NotNull CommandSender sender) {
        final TextComponent.Builder text = Component.text();
        text.appendSpace();
        text.append(Component.text("该命令只能由玩家来执行！").color(NamedTextColor.RED));
        sender.sendMessage(text.build());
    }

    void broadcast(@NotNull TextComponent text) {
        // 控制台
        this.getServer().getConsoleSender().sendMessage(text);

        // 接收消息的在线玩家
        for (final Player player : this.getServer().getOnlinePlayers()) {
            final PlayerSetting setting = this.playerSettingService.getPlayerSetting(player);
            if (!setting.isReceiveGroupMsg()) continue;
            player.sendMessage(text);
        }
    }

}

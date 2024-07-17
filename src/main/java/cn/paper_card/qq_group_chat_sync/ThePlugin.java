package cn.paper_card.qq_group_chat_sync;

import cn.paper_card.client.api.PaperClientApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.google.gson.JsonObject;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ThePlugin extends JavaPlugin implements Listener {

    private PaperClientApi paperClientApi = null;

    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull ConfigManager configManager;

    private final @NotNull MyPlayerService myPlayerService;

    public ThePlugin() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
        this.configManager = new ConfigManager(this);
        this.myPlayerService = new MyPlayerService();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(@NotNull AsyncChatEvent event) {

        final PaperClientApi api = this.paperClientApi;

        if (api == null) return;

        if (!(event.message() instanceof final TextComponent message)) return;

        final String content = message.content();

        if (content.isEmpty()) return;
        if (!content.startsWith(this.configManager.getGameChatPrefix())) return;

        final Player player = event.getPlayer();

        final String name = player.getName();

        final JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("uuid", player.getUniqueId().toString());
        data.addProperty("content", content);

        // 发送事件
        api.postEventAsync("chat.sync.req", System.currentTimeMillis() / 1000, data, null);

        // 提示玩家
        player.sendActionBar(Component.text("此消息将同步到QQ主群内").color(NamedTextColor.GREEN));
    }

    @Override
    public void onEnable() {

        this.getServer().getPluginManager().registerEvents(this, this);
        this.myPlayerService.register(this);

        this.configManager.getAll();
        this.configManager.save();

        this.paperClientApi = this.getServer().getServicesManager().load(PaperClientApi.class);
        final PaperClientApi api = this.paperClientApi;
        if (api != null) api.addEventListener(new OnGroupChat(this));

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

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull ConfigManager getConfigManager() {
        return this.configManager;
    }

    @NotNull MyPlayerService getMyPlayerService() {
        return this.myPlayerService;
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
}

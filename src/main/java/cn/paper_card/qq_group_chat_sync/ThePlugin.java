package cn.paper_card.qq_group_chat_sync;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.qq_group_chat_sync.api.QqGroupChatSyncApi;
import cn.paper_card.qq_group_chat_sync.api.QqGroupMessageSender;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ThePlugin extends JavaPlugin implements Listener {

    private QqGroupChatSyncApiImpl qqGroupChatSyncApi = null;

    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull ConfigManager configManager;

    private final @NotNull MyPlayerService myPlayerService;

    public ThePlugin() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
        this.configManager = new ConfigManager(this);
        this.myPlayerService = new MyPlayerService();
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
        this.myPlayerService.register(this);

        this.configManager.setDefaults();
        this.configManager.save();

        PaperClientApi paperClientApi = this.getServer().getServicesManager().load(PaperClientApi.class);
        if (paperClientApi != null) {
            paperClientApi.addEventListener(event -> {

                if (!"main-group.chat".equals(event.getType())) return;

                final JsonElement data = event.getData();
                if (data == null) return;

//                getSLF4JLogger().info("主群消息: " + data);

                final JsonObject obj = data.getAsJsonObject();

                final JsonObject senderObj = obj.get("sender").getAsJsonObject();

                final JsonArray messageArray = obj.get("message").getAsJsonArray();

                final long senderQq = obj.get("user_id").getAsLong();

                // 查询消息发送者
                OfflinePlayer senderPlayerOffline;
                Player senderPlayer = null;

                try {
                    final String uuidStr = senderObj.get("mc_uuid").getAsString();
                    final UUID uuid = UUID.fromString(uuidStr);
                    senderPlayerOffline = getServer().getOfflinePlayer(uuid);
                    senderPlayer = senderPlayerOffline.getPlayer();
                } catch (Exception ignored) {
                    senderPlayerOffline = null;
                }

                // 解析消息
                for (Player onlinePlayer : getServer().getOnlinePlayers()) {

                    final MyPlayer myPlayer = getMyPlayerService().getMyPlayer(onlinePlayer);

                    final TextComponent.Builder text = Component.text();

                    text.append(Component.text("[QQ群] ").color(NamedTextColor.GRAY));

                    String displayName;

                    try {
                        displayName = senderObj.get("card").getAsString();
                        if ("".equals(displayName)) {
                            displayName = senderObj.get("nickname").getAsString();
                        }
                    } catch (Exception ignored) {
                        displayName = "%d".formatted(senderQq);
                    }

                    final Component displayCom;
                    if (senderPlayer != null) {
                        displayCom = senderPlayer.displayName();
                    } else if (senderPlayerOffline != null) {
                        final String name = senderPlayerOffline.getName();
                        if (name != null) {
                            displayCom = Component.text(name).color(NamedTextColor.GRAY);
                        } else {
                            displayCom = Component.text(displayName).color(NamedTextColor.GRAY);
                        }
                    } else {
                        displayCom = Component.text(displayName).color(NamedTextColor.GRAY);
                    }

                    text.append(Component.text("<"));
                    text.append(displayCom);
                    text.append(Component.text("> "));

                    boolean isMeSent = false;
                    boolean isAtMe = false;
                    boolean isReplyMe = false;
                    boolean isAtAll = false;

                    // 自己发送的消息
                    if (senderPlayerOffline != null &&
                            senderPlayerOffline.getUniqueId().equals(onlinePlayer.getUniqueId())) {
                        isMeSent = true;
                    }

                    for (JsonElement jsonElement : messageArray) {
                        final JsonObject o = jsonElement.getAsJsonObject();

                        final String type = o.get("type").getAsString();
                        final JsonObject msgData = o.get("data").getAsJsonObject();


                        // 纯文本
                        if ("text".equals(type)) {

                            text.append(Component.text(msgData.get("text").getAsString()));

                        } else if ("image".equals(type)) { // 图片

                            final int subType = o.get("subType").getAsInt();

                            final String tag = subType == 0 ? "[图片]" : "[动画表情]";

                            final String url = msgData.get("url").getAsString();

                            text.append(Component.text(tag).decorate(TextDecoration.UNDERLINED)
                                    .color(NamedTextColor.DARK_AQUA)
                                    .hoverEvent(HoverEvent.showText(Component.text("点击查看")))
                                    .clickEvent(ClickEvent.openUrl(url)));

                        } else if ("mface".equals(type)) {

                            final String summary = msgData.get("summary").getAsString();

                            final String url = msgData.get("url").getAsString();

                            String tag = summary;
                            if (!tag.startsWith("[")) {
                                tag = "[" + summary;
                            }
                            if (!tag.endsWith("]")) {
                                tag = summary + "]";
                            }

                            // 表情包
                            text.append(Component.text(tag).color(NamedTextColor.DARK_AQUA)
                                    .decorate(TextDecoration.UNDERLINED)
                                    .hoverEvent(HoverEvent.showText(Component.text("点击查看")))
                                    .clickEvent(ClickEvent.openUrl(url))

                            );

                        } else if ("face".equals(type)) {

                            final String id = msgData.get("id").getAsString();

                            String name = "表情";

                            try {
                                name = msgData.get("name").getAsString();
                            } catch (Exception ignored) {
                            }

                            text.append(Component.text("[%s]".formatted(name)).color(NamedTextColor.DARK_AQUA)
                                    .hoverEvent(HoverEvent.showText(Component.text("id: %s".formatted(id))))
                            );

                        } else if ("reply".equals(type)) {

//                            final String id = msgData.get("id").getAsString();

                            // 获取回复的QQ和昵称
                            String replyTarget;
                            try {
                                final JsonObject json = msgData.get("json").getAsJsonObject();
                                final JsonObject sender = json.get("sender").getAsJsonObject();
                                final String card = sender.get("card").getAsString();
                                final String nickname = sender.get("nickname").getAsString();
                                replyTarget = card;
                                if ("".equals(replyTarget)) {
                                    replyTarget = nickname;
                                }

                                // 判断是不是回复自己
                                final String mcUuid = sender.get("mc_uuid").getAsString();
                                final UUID uuid = UUID.fromString(mcUuid);

                                if (uuid.equals(onlinePlayer.getUniqueId())) {
                                    isReplyMe = true;
                                }

                            } catch (Exception ignored) {
                                replyTarget = null;
                            }

                            final TextComponent.Builder a = Component.text();
                            a.append(Component.text("[回复"));
                            if (replyTarget != null) {
                                a.append(Component.text(" => "));
                                a.append(Component.text(replyTarget));
                            }
                            a.append(Component.text("]"));

                            // todo: 支持悬浮预览

                            text.append(a.build().color(NamedTextColor.GRAY));

                        } else if ("at".equals(type)) {

                            final String qqStr = msgData.get("qq").getAsString();

                            String display;

                            // AT全体
                            if ("all".equalsIgnoreCase(qqStr)) {
                                isAtAll = true;
                                display = "全体成员";
                            } else {
                                try {
                                    display = msgData.get("display").getAsString();
                                } catch (Exception ignore) {
                                    display = qqStr;
                                }
                            }

                            // 判断是不是at自己
                            try {
                                final String mcUUid = msgData.get("mc_uuid").getAsString();
                                final UUID uuid = UUID.fromString(mcUUid);
                                if (uuid.equals(onlinePlayer.getUniqueId())) {
                                    isAtMe = true;
                                }
                            } catch (Exception ignored) {
                            }


                            text.append(Component.text("@%s".formatted(display))
                                    .color(isAtMe ? NamedTextColor.RED : NamedTextColor.GOLD)
                                    .decorate(TextDecoration.BOLD)
                                    .clickEvent(ClickEvent.copyToClipboard(qqStr))
                                    .hoverEvent(HoverEvent.showText(Component.text("QQ:%s".formatted(qqStr))))
                            );


                        } else {
                            text.append(Component.text("[暂不支持的消息:%s]".formatted(type))
                                    .color(NamedTextColor.GRAY)
                                    .hoverEvent(HoverEvent.showText(Component.text(o.toString())))
                            );
                        }

                        text.appendSpace();
                    }

                    final String finalDisplayName = displayName;

                    if (isAtMe) {
                        getTaskScheduler().runTaskLater(() -> {

                            onlinePlayer.sendTitlePart(TitlePart.TITLE,
                                    Component.text("有人在群里@你").color(NamedTextColor.GOLD)
                            );

                            onlinePlayer.sendTitlePart(TitlePart.SUBTITLE,
                                    Component.text()
                                            .append(Component.text(finalDisplayName).color(NamedTextColor.DARK_AQUA))
                                            .append(Component.text(" 可能找你有事"))
                                            .build().color(NamedTextColor.GREEN)
                            );

                        }, 1);
                    } else if (isReplyMe) {
                        getTaskScheduler().runTaskLater(() -> {

                            onlinePlayer.sendTitlePart(TitlePart.TITLE, Component.text()
                                    .append(Component.text("有消息回复").color(NamedTextColor.GOLD))
                                    .build());

                            onlinePlayer.sendTitlePart(TitlePart.SUBTITLE, Component.text()
                                    .append(Component.text(finalDisplayName).color(NamedTextColor.DARK_AQUA))
                                    .append(Component.text(" 在主群回复了你的消息"))
                                    .build().color(NamedTextColor.GREEN));

                        }, 1);
                    }

                    if (isAtAll) {
                        getTaskScheduler().runTaskLater(() -> {

                            onlinePlayer.sendTitlePart(TitlePart.TITLE, Component.text()
                                    .append(Component.text("@全体成员").color(NamedTextColor.RED))
                                    .build());

                            onlinePlayer.sendTitlePart(TitlePart.SUBTITLE, Component.text()
                                    .append(Component.text(finalDisplayName).color(NamedTextColor.DARK_AQUA))
                                    .append(Component.text(" 在主群@全体"))
                                    .build().color(NamedTextColor.GREEN));

                        }, 1);
                    }

                    if (myPlayer.isReceiveGroupMsg() || isMeSent || isAtMe || isReplyMe) {

                        final NamedTextColor color;
                        if (senderPlayer != null) {
                            color = NamedTextColor.WHITE;
                        } else {
                            color = NamedTextColor.GRAY;
                        }

                        onlinePlayer.sendMessage(text.build().color(color));
                    }
                }
            });
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

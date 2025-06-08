package cn.paper_card.qq_group_chat_sync;

import cn.paper_card.client.api.EventListener;
import cn.paper_card.client.api.PaperEvent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

class OnGroupChat implements EventListener {

    private final @NotNull ThePlugin plugin;

    OnGroupChat(@NotNull ThePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void on(@NotNull PaperEvent event) {

        if (!"main-group.chat".equals(event.getType())) return;

        final JsonElement data = event.getData();

        if (data == null) return;

        final JsonObject dataObj;

        try {
            dataObj = data.getAsJsonObject();
        } catch (Exception e) {
            this.plugin.getSLF4JLogger().error("data obj: ", e);
            return;
        }

        // 解析消息发送者
        final SenderParser senderParser;
        try {
            senderParser = new SenderParser(dataObj, this.plugin.getServer());
        } catch (Exception e) {
            this.plugin.getSLF4JLogger().error("parse message sender: ", e);
            return;
        }

        // 解析消息
        final MessageParser messageParser;

        try {
            messageParser = new MessageParser(dataObj, this.plugin.getServer());
        } catch (Exception e) {
            this.plugin.getSLF4JLogger().error("parse message: ", e);
            return;
        }

        final Component senderComponent = senderParser.component();

        // 发送给玩家的消息
        final Component messageComponent;
        {
            final TextComponent.Builder text = Component.text();

            text.append(Component.text("[QQ群] ").color(NamedTextColor.GRAY));

            text.append(senderComponent);

            text.append(Component.space());
            text.append(messageParser.toComponent().color(
                    senderParser.getOnlinePlayer() != null ? NamedTextColor.WHITE : NamedTextColor.GRAY));

            messageComponent = text.build().color(NamedTextColor.WHITE);
        }

        // 播报到控制台
        {
            final TextComponent.Builder text = Component.text();
            text.append(Component.text("[QQ群] ").color(NamedTextColor.GOLD));
            text.append(senderComponent);
            text.append(Component.space());
            text.append(messageParser.toComponent().color(NamedTextColor.GREEN));
            this.plugin.getServer().getConsoleSender().sendMessage(text.build().color(NamedTextColor.WHITE));
        }

        // 发送给每一个玩家
        for (Player onlinePlayer : this.plugin.getServer().getOnlinePlayers()) {

            final MyPlayer myPlayer = this.plugin.getMyPlayerService().getMyPlayer(onlinePlayer);

            // 判断自己发送的消息
            boolean isMeSent = false;
            {
                final OfflinePlayer senderOffline = senderParser.getOfflinePlayer();

                if (senderOffline != null && senderOffline.getUniqueId().equals(onlinePlayer.getUniqueId())) {
                    isMeSent = true;
                }
            }

            // 判断是否at该玩家
            final boolean isAtMe = messageParser.hasAtPlayer(onlinePlayer.getUniqueId());

            // 判断是否回复at该玩家
            final boolean isReplyMe = messageParser.hasReplyPlayer(onlinePlayer.getUniqueId());

            // 判断是否at全体
            final boolean isAtAll = messageParser.hasAtAll();

            if (myPlayer.isReceiveGroupMsg() || isMeSent || isAtMe || isReplyMe || isAtAll) {
                onlinePlayer.sendMessage(messageComponent);
            }

            if (isAtMe) {
                // 额外处理，艾特玩家

                this.plugin.getTaskScheduler().runTaskLater(() -> {

                    onlinePlayer.sendTitlePart(TitlePart.TITLE,
                            Component.text("有人在群里@你").color(NamedTextColor.GOLD)
                    );

                    final Component msg = Component.text()
                            .append(Component.text(senderParser.displayName()).color(NamedTextColor.DARK_AQUA))
                            .append(Component.text(" 可能找你有事"))
                            .build().color(NamedTextColor.GREEN);

                    onlinePlayer.sendTitlePart(TitlePart.SUBTITLE, msg);

                }, 1);


            } else if (isReplyMe) {
                // 额外处理，回复消息

                this.plugin.getTaskScheduler().runTaskLater(() -> {

                    onlinePlayer.sendTitlePart(TitlePart.TITLE, Component.text()
                            .append(Component.text("有消息回复").color(NamedTextColor.GOLD))
                            .build());

                    onlinePlayer.sendTitlePart(TitlePart.SUBTITLE, Component.text()
                            .append(Component.text(senderParser.displayName()).color(NamedTextColor.DARK_AQUA))
                            .append(Component.text(" 在主群回复了你的消息"))
                            .build().color(NamedTextColor.GREEN));

                }, 1);
            }

            if (isAtAll) {
                // 额外处理：艾特全体
                this.plugin.getTaskScheduler().runTaskLater(() -> {

                    onlinePlayer.sendTitlePart(TitlePart.TITLE, Component.text("@全体成员")
                            .color(NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD)
                    );

                    onlinePlayer.sendTitlePart(TitlePart.SUBTITLE, Component.text()
                            .append(Component.text(senderParser.displayName()).color(NamedTextColor.DARK_AQUA))
                            .append(Component.text(" 在主群@全体"))
                            .build().color(NamedTextColor.GREEN));

                }, 1);
            }
        }
    }
}

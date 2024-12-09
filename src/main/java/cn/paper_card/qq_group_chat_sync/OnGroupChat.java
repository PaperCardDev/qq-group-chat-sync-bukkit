package cn.paper_card.qq_group_chat_sync;

import cn.paper_card.client.api.EventListener;
import cn.paper_card.client.api.PaperEvent;
import com.google.gson.JsonArray;
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
            messageParser = new MessageParser(dataObj);
        } catch (Exception e) {
            this.plugin.getSLF4JLogger().error("parse message: ", e);
            return;
        }

        final Component senderComponent = senderParser.component();
        final Component messageComponent;

        {
            final TextComponent.Builder text = Component.text();

            text.append(Component.text("[QQ群] ").color(NamedTextColor.GRAY));
            text.append(senderComponent);
            text.append(Component.space());
            text.append(messageParser.toComponent());

            messageComponent = text.build().color(
                    senderParser.getOnlinePlayer() == null ?
                            NamedTextColor.WHITE : NamedTextColor.GRAY
            );
        }

        // 播报到控制台
        this.plugin.getServer().getConsoleSender().sendMessage(messageComponent);


        // 发送给每一个玩家
        for (Player onlinePlayer : this.plugin.getServer().getOnlinePlayers()) {

            boolean isMeSent = false;
            boolean isReplyMe = false;

            final MyPlayer myPlayer = this.plugin.getMyPlayerService().getMyPlayer(onlinePlayer);

            // 自己发送的消息
            final OfflinePlayer senderOffline = senderParser.getOfflinePlayer();

            if (senderOffline != null && senderOffline.getUniqueId().equals(onlinePlayer.getUniqueId())) {
                isMeSent = true;
            }

            // 判断是否at该玩家
            final boolean isAtMe = messageParser.hasAtPlayer(onlinePlayer.getUniqueId());

            // 判断是否回复at该玩家

            // 判断是否at全体
            final boolean isAtAll = messageParser.hasAtAll();

//            for (JsonElement jsonElement : messageArray) {
//                final JsonObject o = jsonElement.getAsJsonObject();
//
//                final String type = o.get("type").getAsString();
//                final JsonObject msgData = o.get("data").getAsJsonObject();
//
//
//                if ("reply".equals(type)) {
//                    // 获取回复的QQ和昵称
//                    String replyTarget;
//                    try {
//                        final JsonObject json = msgData.get("json").getAsJsonObject();
//                        final JsonObject sender = json.get("sender").getAsJsonObject();
//                        final String card = sender.get("card").getAsString();
//                        final String nickname = sender.get("nickname").getAsString();
//                        replyTarget = card;
//                        if ("".equals(replyTarget)) {
//                            replyTarget = nickname;
//                        }
//
//                        // 判断是不是回复自己
//                        final String mcUuid = sender.get("mc_uuid").getAsString();
//                        final UUID uuid = UUID.fromString(mcUuid);
//
//                        if (uuid.equals(onlinePlayer.getUniqueId())) {
//                            isReplyMe = true;
//                        }
//
//                    } catch (Exception ignored) {
//                        replyTarget = null;
//                    }
//
//                    final TextComponent.Builder a = Component.text();
//                    a.append(Component.text("[回复"));
//                    if (replyTarget != null) {
//                        a.append(Component.text(" => "));
//                        a.append(Component.text(replyTarget));
//                    }
//                    a.append(Component.text("]"));
//
//                    // todo: 支持悬浮预览
//                }
//            }

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

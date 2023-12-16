package cn.paper_card.qq_group_chat_sync;

import cn.paper_card.qq_bind.api.BindInfo;
import cn.paper_card.qq_bind.api.QqBindApi;
import cn.paper_card.qq_group_chat_sync.api.QqGroupChatSyncApi;
import cn.paper_card.qq_group_chat_sync.api.QqGroupMessageSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class QqGroupChatSyncApiImpl implements QqGroupChatSyncApi {

    private QqGroupMessageSender sender = null;
    private final @NotNull ThePlugin plugin;

    QqGroupChatSyncApiImpl(@NotNull ThePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable String onGroupMessage(long qq, @NotNull String name, @NotNull String content) {
        // 查询QQ绑定
        final QqBindApi api = plugin.getQqBindApi();
        if (api == null) return null;

        final BindInfo qqBind;

        try {
            qqBind = api.getBindService().queryByQq(qq);
        } catch (Exception e) {
            plugin.handleException("query qq bind by qq", e);
            return e.toString();
        }

        if (qqBind == null) {
            // TODO：提示
            return null;
        }

        final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(qqBind.uuid());

        // 如果太久没有上线

        // 如果没有上线过
        final long lastSeen = offlinePlayer.getLastSeen();
        if (lastSeen < 0) {
            // TODO：提示
            return null;
        }

        final long cur = System.currentTimeMillis();
        final long delta = cur - lastSeen - 7 * 24 * 60 * 60 * 1000L;
        if (delta > 0) {
            // TODO：提示
            return null;
        }

        final TextComponent.Builder builder = Component.text();

        final Player player = offlinePlayer.getPlayer();
        final boolean isOnline = player != null && player.isOnline();

        builder.append(Component.text("[QQ群] ").color(NamedTextColor.GRAY));

        builder.append(Component.text("<").color(NamedTextColor.GOLD));
        if (isOnline) {
            builder.append(player.displayName());
        } else {
            String playerName = offlinePlayer.getName();
            if (playerName == null || playerName.isEmpty()) playerName = qqBind.name();
            builder.append(Component.text(playerName).color(NamedTextColor.GRAY));
        }
        builder.append(Component.text("> ").color(NamedTextColor.GOLD));

        builder.append(Component.text(content));

        // 广播
        plugin.getTaskScheduler().runTask(() -> plugin.getServer().broadcast(builder.build()));

        return null;
    }

    @Override
    public @Nullable String onAtMessage(long qq, @NotNull String name, long target, @NotNull String content) {

        final QqBindApi api = plugin.getQqBindApi();

        if (api == null) return null;

        final BindInfo bindInfo;

        try {
            bindInfo = api.getBindService().queryByQq(target);
        } catch (Exception e) {
            plugin.handleException("qq bind service -> query by qq", e);
            return e.toString();
        }

        if (bindInfo == null) return null;

        final Player player = plugin.getServer().getPlayer(bindInfo.uuid());

        if (player == null) return null;
        if (!player.isOnline()) return null;

        plugin.getTaskScheduler().runTask(() -> {

            player.sendTitlePart(TitlePart.TITLE, Component.text()
                    .append(Component.text(name).color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD))
                    .append(Component.text(" 在群里@你").color(NamedTextColor.GOLD))
                    .build());

            player.sendTitlePart(TitlePart.SUBTITLE, Component.text("他可能找你有事").color(NamedTextColor.GREEN));
        });

        return null;
    }

    @Override
    public @Nullable String onAtAllMessage(long qq, @NotNull String name, @NotNull String content) {
        plugin.getTaskScheduler().runTask(() -> {
            for (final Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendTitlePart(TitlePart.TITLE, Component.text()
                        .append(Component.text("@全体成员").color(NamedTextColor.GOLD))
                        .build());
            }
        });

        return null;
    }

    @Override
    public @Nullable String onReplySyncMessage(long qq, @NotNull String name, long target, @NotNull String content) {
        return null;
    }

    @Override
    public void setMessageSender(@Nullable QqGroupMessageSender qqGroupMessageSender) {
        synchronized (this) {
            this.sender = qqGroupMessageSender;
        }
    }

    @Override
    public boolean setMessageSenderIfNull(@NotNull QqGroupMessageSender qqGroupMessageSender) {
        synchronized (this) {
            if (this.sender == null) {
                this.sender = qqGroupMessageSender;
                return true;
            }
            return false;
        }
    }

    @Override
    public @Nullable QqGroupMessageSender getMessageSender() {
        return this.sender;
    }
}

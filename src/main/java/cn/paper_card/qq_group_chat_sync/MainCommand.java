package cn.paper_card.qq_group_chat_sync;

import cn.paper_card.mc_command.TheMcCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

class MainCommand extends TheMcCommand.HasSub {

    private final @NotNull ThePlugin plugin;
    private final @NotNull Permission permission;

    public MainCommand(@NotNull ThePlugin plugin) {
        super("qq-group-chat-sync");
        this.plugin = plugin;

        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission(this.getLabel() + "." + "command"));

        final PluginCommand c = plugin.getCommand(this.getLabel());
        assert c != null;
        c.setExecutor(this);
        c.setTabCompleter(this);

        this.addSubCommand(new Reload());
        this.addSubCommand(new Toggle());
    }

    @Override
    protected boolean canNotExecute(@NotNull CommandSender commandSender) {
        return !commandSender.hasPermission(this.permission);
    }

    class Reload extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Reload() {
            super("reload");
            this.permission = plugin.addPermission(MainCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            plugin.getConfigManager().reload();
            final TextComponent.Builder text = Component.text();
            plugin.appendPrefix(text);
            text.append(Component.text(" 已重载配置"));
            commandSender.sendMessage(text.build().color(NamedTextColor.GREEN));
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            return null;
        }
    }

    class Toggle extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Toggle() {
            super("toggle");
            this.permission = plugin.addPermission(MainCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (!(commandSender instanceof final Player player)) {
                plugin.sendError(commandSender);
                return true;
            }

            final MyPlayer setting = plugin.getMyPlayerService().getMyPlayer(player);
            setting.setReceiveGroupMsg(!setting.isReceiveGroupMsg());

            final TextComponent.Builder text = Component.text();
            plugin.appendPrefix(text);
            text.appendSpace();
            text.append(Component.text("个人设置变更，接收QQ群同步消息："));
            text.append(Component.text(setting.isReceiveGroupMsg() ? "是" : "否").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

            player.sendMessage(text.build().color(NamedTextColor.GREEN));
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            return null;
        }
    }

}

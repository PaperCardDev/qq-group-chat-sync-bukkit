package cn.paper_card.qq_group_chat_sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.UUID;

interface MessageElement {

    @NotNull Component toComponent();
}

class TextMessage implements MessageElement {

    private final @NotNull String text;

    TextMessage(final @NotNull JsonObject data) {
        this.text = data.get("text").getAsString();
    }

    @Override
    public @NotNull Component toComponent() {
        return Component.text(this.text);
    }
}

class ImageMessage implements MessageElement {

    private final String url;
    private final String summary;

    private final boolean isMarketFace;

    ImageMessage(@NotNull JsonObject data) {
        this.url = data.get("url").getAsString();
        this.summary = data.get("summary").getAsString();

        final String file = data.get("file").getAsString();

        this.isMarketFace = "marketface".equals(file);
    }

    @Override
    public @NotNull Component toComponent() {
        String a = this.summary;

        if ("".equals(a)) {
            a = "[图片]";
        } else {
            if (!a.startsWith("[")) {
                a = "[" + a;
            }
            if (!a.endsWith("]")) {
                a = a + "]";
            }
        }

        return Component.text(a).color(NamedTextColor.DARK_AQUA)
                .hoverEvent(HoverEvent.showText(Component.text("点击查看")))
                .clickEvent(ClickEvent.openUrl(this.url));
    }

    boolean isMarketFace() {
        return this.isMarketFace;
    }
}

class AtMessage implements MessageElement {

    private final String qq;
    private final @Nullable String display;

    private final boolean isAtAll;

    private final @Nullable UUID mcUuid;

    AtMessage(@NotNull JsonObject data) {

        this.qq = data.get("qq").getAsString();

        this.isAtAll = "all".equals(this.qq);

        final JsonElement displayEle = data.get("display");
        if (displayEle != null && !displayEle.isJsonNull()) {
            this.display = displayEle.getAsString();
        } else {
            this.display = null;
        }

        final JsonElement mcUuidEle = data.get("mc_uuid");
        if (mcUuidEle != null && !mcUuidEle.isJsonNull()) {
            this.mcUuid = UUID.fromString(mcUuidEle.getAsString());
        } else {
            this.mcUuid = null;
        }
    }

    @Override
    public @NotNull Component toComponent() {

        boolean showHoverQQ = true;

        String dis = this.display;
        if (dis == null || dis.isEmpty()) {
            if (this.isAtAll) {
                dis = "全体成员";
            } else {
                dis = this.qq;
            }
            showHoverQQ = false;
        }

        return Component.text("@" + dis)
                .color(this.isAtAll ? NamedTextColor.RED : NamedTextColor.GOLD)
                .decorate(TextDecoration.ITALIC)
                .hoverEvent(!showHoverQQ ? null : HoverEvent.showText(Component.text("QQ: " + this.qq)));
    }

    boolean isAtAll() {
        return this.isAtAll;
    }

    @Nullable UUID getMcUuid() {
        return this.mcUuid;
    }
}

class FaceMessage implements MessageElement {

    private final @NotNull String id;

    FaceMessage(@NotNull JsonObject data) {
        this.id = data.get("id").getAsString();
    }

    @Override
    public @NotNull Component toComponent() {
        return Component.text("[表情]")
                .color(NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text("表情ID: " + this.id)));
    }
}

class ReplyMessage implements MessageElement {

    private final @Nullable MessageParser originMsg;

    private final @NotNull String id;

    ReplyMessage(@NotNull JsonObject data) {

        this.id = data.get("id").getAsString();

        final JsonElement jsonEle = data.get("json");
        if (jsonEle != null && jsonEle.isJsonObject()) {
            final JsonObject jsonObj = jsonEle.getAsJsonObject();
            // 解析消息
            this.originMsg = new MessageParser(jsonObj);
        } else {
            this.originMsg = null;
        }
    }

    @Override
    public @NotNull Component toComponent() {
        return Component.text("[回复]")
                .color(NamedTextColor.GRAY)
                .hoverEvent(this.originMsg == null ?
                        HoverEvent.showText(Component.text("消息ID: " + this.id)) :
                        HoverEvent.showText(this.originMsg.toComponent()));
    }
}

class UnsupportedMessage implements MessageElement {

    private final @NotNull String type;
    private final @NotNull JsonObject data;

    UnsupportedMessage(@NotNull String type, @NotNull JsonObject data) {
        this.type = type;
        this.data = data;
    }

    @NotNull
    private String getType() {
        return this.type;
    }

    @Override
    public @NotNull Component toComponent() {

        final String text = "不支持或尚未支持的消息类型: " + this.getType() + "\nData: " + this.data;

        return Component.text("[%s]".formatted(this.getType().toUpperCase()))
                .hoverEvent(HoverEvent.showText(Component.text(text)));
    }
}

class MessageParser {

    private final @NotNull LinkedList<MessageElement> messageElements = new LinkedList<>();

    private final @NotNull LinkedList<UUID> atPlayers = new LinkedList<>();

    private boolean hasAtAll = false;

    MessageParser(@NotNull JsonObject dataObj) {
        final JsonArray array = dataObj.get("message").getAsJsonArray();

        for (JsonElement jsonElement : array) {
            final JsonObject msgObj = jsonElement.getAsJsonObject();

            final String type = msgObj.get("type").getAsString();
            final JsonObject msgData = msgObj.get("data").getAsJsonObject();

            switch (type) {
                case "text":
                    this.messageElements.add(new TextMessage(msgData));
                    break;

                case "image":
                    this.messageElements.add(new ImageMessage(msgData));
                    break;

                case "at":
                    final AtMessage atMessage = new AtMessage(msgData);
                    this.messageElements.add(atMessage);
                    final UUID mcUuid = atMessage.getMcUuid();
                    if (mcUuid != null) {
                        this.atPlayers.add(mcUuid);
                    }
                    if (atMessage.isAtAll()) {
                        this.hasAtAll = true;
                    }
                    break;

                case "face":
                    final FaceMessage fm = new FaceMessage(msgData);
                    this.messageElements.add(fm);
                    break;

                case "reply":
                    final ReplyMessage rm = new ReplyMessage(msgData);
                    this.messageElements.add(rm);
                    break;

                default:
                    this.messageElements.add(new UnsupportedMessage(type, msgData));
                    break;
            }
        }
    }

    boolean hasAtAll() {
        return this.hasAtAll;
    }

    boolean hasAtPlayer(@NotNull UUID uuid) {
        return this.atPlayers.contains(uuid);
    }

    @NotNull LinkedList<MessageElement> getMessageElements() {
        return this.messageElements;
    }

    @NotNull TextComponent toComponent() {
        final TextComponent.Builder text = Component.text();
        boolean first = true;
        for (MessageElement messageElement : this.messageElements) {
            if (first) {
                first = false;
            } else {
                text.appendSpace();
            }
            text.append(messageElement.toComponent());
        }
        return text.build();
    }
}
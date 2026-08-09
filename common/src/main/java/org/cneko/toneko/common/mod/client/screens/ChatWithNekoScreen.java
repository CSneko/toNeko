package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.packets.interactives.ChatHistoryRequestPayload;
import org.cneko.toneko.common.mod.packets.interactives.ChatWithNekoPayload;
import org.cneko.toneko.common.util.TTSUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChatWithNekoScreen extends Screen implements INekoScreen {
    private final NekoEntity neko;
    private EditBox textField;
    private double scrollAmount;
    private int contentHeight;
    /** 吸底模式：打开屏幕/发送消息后为 true，新内容自动滚到最底部；用户手动滚动离开后为 false */
    private boolean stickToBottom = true;
    /** 滑块拖动状态 */
    private boolean draggingScrollbar = false;
    /** 按下时鼠标在滑块上的相对位置（0~1），拖动时保持该相对位置 */
    private double dragRatio = 0;

    private static final int MAX_HISTORY = 200;
    private static final int CHAT_PAD = 16;
    // ===== 消息块样式（气泡布局，区分用户/AI） =====
    private static final int BUBBLE_X_PAD = 6;   // 气泡左右内边距
    private static final int BUBBLE_Y_PAD = 3;   // 气泡上下内边距
    private static final int BLOCK_GAP = 4;      // 消息块间距
    private static final int COLOR_USER = 0xFFE8B060;   // 用户：暖橙
    private static final int COLOR_AI = 0xFFB0C8E8;     // AI：浅蓝
    private static final int COLOR_ERROR = 0xFFFF6B6B;  // 错误：红
    private static final int COLOR_PLAIN = 0xCCCCCC;    // 无角色：灰
    private static final int BG_USER = 0x40E8B060;      // 用户气泡（25% 橙）
    private static final int BG_AI = 0x40304048;        // AI 气泡（25% 深蓝灰）
    private static final int BG_NONE = 0;               // 无背景

    /** 历史行的消息角色（前缀标记，见 receiveHistory/sendMessage/receiveStreamChunk） */
    private enum MsgRole { USER, AI, ERROR, NONE }

    // Server-sourced history, persisted globally by neko UUID
    private static final Map<UUID, List<String>> HISTORY = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, List<String>> e) { return size() > 50; }
    };

    /** 流式回复（打字机）状态，静态 + 按 UUID key：中途关屏重开不丢打字行 */
    private static final Map<UUID, StreamState> STREAMS = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, StreamState> e) { return size() > 50; }
    };
    private static final int MAX_STREAM_LENGTH = 2000;

    private static class StreamState {
        final StringBuilder full = new StringBuilder();
    }

    /**
     * 接收流式增量（ClientNetworkEvents 调用，已在客户端主线程）。
     * finished=false → 追加增量；finished=true → 落定：成功把完整文本提交进 HISTORY，
     * 失败保留已得部分并追加错误行。
     */
    public static void receiveStreamChunk(UUID nekoUuid, String chunk, boolean finished, String error) {
        if (!finished) {
            StreamState state = STREAMS.computeIfAbsent(nekoUuid, k -> new StreamState());
            if (state.full.length() < MAX_STREAM_LENGTH) {
                state.full.append(chunk);
            }
            return;
        }
        // 收尾：屏幕打开期间才有流状态（未打开时历史由服务端 pushChatHistory 同步，无需本地维护）
        StreamState state = STREAMS.remove(nekoUuid);
        if (state == null) return;
        List<String> h = HISTORY.computeIfAbsent(nekoUuid, k -> new ArrayList<>());
        if (state.full.length() > 0) {
            h.add("§d< §f" + state.full);
            if (h.size() > MAX_HISTORY) h.remove(0);
        }
        if (error != null) {
            h.add("§c" + error);
            if (h.size() > MAX_HISTORY) h.remove(0);
        }
    }

    public ChatWithNekoScreen(NekoEntity neko) {
        super(Component.translatable("screen.toneko.chat_with_neko.title", neko.getName()));
        this.neko = neko;
    }

    @Override public NekoEntity getNeko() { return neko; }

    public static void receiveHistory(UUID nekoUuid, List<String> messages) {
        List<String> h = HISTORY.computeIfAbsent(nekoUuid, k -> new ArrayList<>());
        h.clear();
        for (String msg : messages) {
            int colon = msg.indexOf(':');
            if (colon > 0) {
                String role = msg.substring(0, colon);
                String text = msg.substring(colon + 1);
                String prefix = role.equals("user") ? "§6> §f" : "§d< §f";
                h.add(prefix + text);
            }
        }
    }

    private List<String> history() {
        return HISTORY.computeIfAbsent(neko.getUUID(), k -> new ArrayList<>());
    }

    private void refreshHistory() {
        ClientPlayNetworking.send(new ChatHistoryRequestPayload(neko.getUUID().toString()));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int inputY = height - 38;

        textField = new EditBox(font, centerX - 155, inputY, 230, 20, Component.empty());
        textField.setMaxLength(1000);
        addRenderableWidget(textField);

        addRenderableWidget(Button.builder(
                Component.translatable("screen.toneko.chat_with_neko.button.send"), b -> sendMessage())
                .size(60, 20).pos(centerX + 80, inputY).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.toneko.chat_with_neko.button.end"), b -> onClose())
                .size(200, 20).pos(centerX - 100, inputY + 24).build());

        setFocused(textField);
        textField.setFocused(true);
        refreshHistory();
    }

    private void sendMessage() {
        String msg = textField.getValue().trim();
        if (msg.isEmpty()) return;
        // 玩家发新消息时立即打断当前 TTS 语音（后一条消息胜出）
        TTSUtil.stopTTS();
        ClientPlayNetworking.send(new ChatWithNekoPayload(neko.getUUID().toString(), msg));
        // Add to local display immediately (server response comes as system chat, not to this screen)
        List<String> h = history();
        h.add("§6> §f" + msg);
        if (h.size() > MAX_HISTORY) h.remove(0);
        textField.setValue("");
        textField.setFocused(true);
        // 发出消息后吸底，让新消息立即可见
        stickToBottom = true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { sendMessage(); return true; }
        if (keyCode == 256) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        double maxScroll = Math.max(0, contentHeight - chatH());
        scrollAmount = Mth.clamp(scrollAmount - dy * 10, 0, maxScroll);
        // 滚回最底部时恢复吸底，否则离开吸底模式
        stickToBottom = scrollAmount >= maxScroll - 1;
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && contentHeight > chatH()) {
            int sx = chatX() + chatW() - 3;
            int sh = scrollbarHeight();
            int sy = scrollbarY();
            // 命中滑块或稍宽的热区（细滑块容易点不中）
            if (mx >= sx - 3 && mx <= sx + 6 && my >= sy - 2 && my <= sy + sh + 2) {
                draggingScrollbar = true;
                dragRatio = Mth.clamp((my - sy) / Math.max(1, sh), 0, 1);
                stickToBottom = false;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScrollbar) {
            double maxScroll = Math.max(0, contentHeight - chatH());
            double track = chatH();
            // 保持按下时的相对位置：滑块顶部 = my - dragRatio * sh，按比例换算内容滚动量
            double desiredTop = my - dragRatio * scrollbarHeight();
            scrollAmount = Mth.clamp(desiredTop / track * maxScroll, 0, maxScroll);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            double maxScroll = Math.max(0, contentHeight - chatH());
            stickToBottom = scrollAmount >= maxScroll - 1;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x80000000);
        g.fill(0, 0, width, 30, 0xC0000000);
        g.drawCenteredString(font, getTitle(), width / 2, 8, 0xFFFFFF);

        int chatX = chatX();
        int chatY = chatY();
        int chatW = chatW();
        int chatH = chatH(); // leave room for input row + buttons at bottom

        // 流式回复（打字机）行：空文本显示 …，尾部闪烁光标
        StreamState streamState = STREAMS.get(neko.getUUID());
        String streamLine = null;
        if (streamState != null) {
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            String cursor = blink ? "▍" : "";
            String text = streamState.full.length() == 0 ? "…" : streamState.full.toString();
            streamLine = "§d< §f" + text + cursor;
        }

        // Pre-calculate total content height (independent of scroll)
        int totalH = 0;
        for (String line : history()) {
            totalH += blockHeight(line, chatW);
        }
        if (streamLine != null) {
            totalH += blockHeight(streamLine, chatW);
        }
        contentHeight = totalH;

        // 吸底逻辑：流式期间自动吸底（打字行始终可见）；打开屏幕/发消息后（stickToBottom）吸底；
        // 用户手动滚动离开后保持当前位置；拖动滑块期间不被流式吸底打断
        double maxScroll = Math.max(0, contentHeight - chatH);
        if (streamState != null && !draggingScrollbar) {
            scrollAmount = maxScroll;
            stickToBottom = true;
        } else if (stickToBottom) {
            scrollAmount = maxScroll;
        } else {
            scrollAmount = Mth.clamp(scrollAmount, 0, maxScroll);
        }

        g.enableScissor(chatX, chatY, chatX + chatW, chatY + chatH);

        List<String> h = history();
        int renderY = chatY - (int) scrollAmount;
        for (String line : h) {
            renderBlock(g, line, chatX, chatW, renderY);
            renderY += blockHeight(line, chatW);
        }
        if (streamLine != null) {
            renderBlock(g, streamLine, chatX, chatW, renderY);
        }

        g.disableScissor();

        // Scrollbar
        if (contentHeight > chatH) {
            g.fill(scrollbarX(), scrollbarY(), scrollbarX() + 3, scrollbarY() + scrollbarHeight(), 0x80FFB6C1);
        }

        super.render(g, mx, my, pt);
    }

    // ===== 消息块渲染（角色解析 + 气泡绘制） =====

    /** 解析历史行的角色（前缀标记：§6> = 用户，§d< = AI，§c = 错误） */
    private static MsgRole roleOf(String line) {
        if (line.startsWith("§6> ")) return MsgRole.USER;
        if (line.startsWith("§d< ")) return MsgRole.AI;
        if (line.startsWith("§c")) return MsgRole.ERROR;
        return MsgRole.NONE;
    }

    /** 去掉行前缀（含前缀末尾的 §f 复位码），返回纯文本内容 */
    private static String textOf(String line) {
        if (line.startsWith("§6> §f") || line.startsWith("§d< §f")) return line.substring(6);
        if (line.startsWith("§c")) return line.substring(2);
        return line;
    }

    /** 单条消息块高度（含气泡内边距与块间距），wrap 宽度与渲染一致 */
    private int blockHeight(String line, int chatW) {
        int lines = wrapLine(textOf(line), chatW - 4 - BUBBLE_X_PAD * 2).size();
        return lines * font.lineHeight + BUBBLE_Y_PAD * 2 + BLOCK_GAP;
    }

    /**
     * 绘制一条消息块：AI 消息左对齐带深色气泡，用户消息右对齐带橙色气泡，错误/无角色无背景。
     * 历史行前缀中的 § 码在 Component.literal 下不生效，颜色由这里按角色显式指定。
     */
    private void renderBlock(GuiGraphics g, String line, int chatX, int chatW, int renderY) {
        MsgRole role = roleOf(line);
        String text = textOf(line);
        int chatY = chatY();
        int chatH = chatH();
        int lineH = font.lineHeight;
        int innerW = chatW - 4 - BUBBLE_X_PAD * 2;
        List<String> wrapped = wrapLine(text, innerW);
        int contentH = wrapped.size() * lineH;
        int y0 = renderY + BUBBLE_Y_PAD;
        int y1 = y0 + contentH + BUBBLE_Y_PAD;
        int x0 = chatX + 2;
        int x1 = chatX + chatW - 2;

        int color = switch (role) {
            case USER -> COLOR_USER;
            case AI -> COLOR_AI;
            case ERROR -> COLOR_ERROR;
            case NONE -> COLOR_PLAIN;
        };
        int bg = switch (role) {
            case USER -> BG_USER;
            case AI -> BG_AI;
            default -> BG_NONE;
        };

        // 气泡背景（半透明，先画再画文字）
        if (bg != BG_NONE && y1 > chatY && y0 < chatY + chatH) {
            g.fill(x0, y0, x1, y1, bg);
        }

        int ty = y0 + BUBBLE_Y_PAD;
        for (String w : wrapped) {
            if (ty + lineH > chatY && ty < chatY + chatH) {
                // 用户消息右对齐，AI 消息左对齐
                int tx = role == MsgRole.USER
                        ? x1 - BUBBLE_X_PAD - font.width(w)
                        : x0 + BUBBLE_X_PAD;
                g.drawString(font, Component.literal(w), tx, ty, color);
            }
            ty += lineH;
        }
    }

    // ===== 布局几何（render 与滑块交互共用，保证计算一致） =====

    private int chatX() { return CHAT_PAD; }
    private int chatY() { return 35; }
    private int chatW() { return width - CHAT_PAD * 2; }
    /** 聊天区高度（给底部输入行与按钮留空间） */
    private int chatH() { return height - 80; }
    private int scrollbarX() { return chatX() + chatW() - 3; }
    /** 滑块高度：内容越多滑块越短，最小 20px */
    private int scrollbarHeight() {
        return Math.max(20, (int) ((float) chatH() / contentHeight * chatH()));
    }
    /** 滑块顶部位置：按 scrollAmount 占最大滚动的比例换算（与拖动公式一致） */
    private int scrollbarY() {
        double maxScroll = Math.max(0, contentHeight - chatH());
        double ratio = maxScroll > 0 ? scrollAmount / maxScroll : 0;
        return chatY() + (int) (ratio * chatH());
    }

    /** Wrap text to fit within maxWidth, splitting on \n and word-wrapping long lines */
    private List<String> wrapLine(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        // Split on literal \n first
        String[] paragraphs = text.split("\\\\n|\\n");
        for (String para : paragraphs) {
            if (para.isEmpty()) {
                result.add("");
                continue;
            }
            // Word wrap
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < para.length(); i++) {
                char c = para.charAt(i);
                current.append(c);
                // Check width periodically
                if (font.width(current.toString()) > maxWidth) {
                    // Find last space to break
                    int breakAt = current.length() - 2;
                    while (breakAt > 0 && current.charAt(breakAt) != ' ') breakAt--;
                    if (breakAt > 0) {
                        result.add(current.substring(0, breakAt));
                        current = new StringBuilder(current.substring(breakAt + 1));
                    } else {
                        // No space found, force break
                        current.setLength(current.length() - 1);
                        result.add(current.toString());
                        current = new StringBuilder(String.valueOf(c));
                    }
                }
            }
            if (!current.isEmpty()) result.add(current.toString());
        }
        return result.isEmpty() ? List.of("") : result;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}
}

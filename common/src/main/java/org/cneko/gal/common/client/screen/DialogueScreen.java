package org.cneko.gal.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.cneko.gal.common.Gal;
import org.cneko.gal.common.client.parser.GalInfo; // 这个是用来对上剧情名字的，嗯！
import org.cneko.gal.common.client.parser.GalParser;
import org.cneko.gal.common.client.parser.PlotParser;
import org.cneko.gal.common.util.TextureUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List; // 要用那个 List<GalInfo.PlotInfo> 啦~
import java.util.Map;

public class DialogueScreen extends Screen {
    private final GalParser parser;
    private PlotParser.DialogueNode currentNode;
    private Map<String, PlotParser.DialogueNode> currentPlotNodes; // 这里放着现在这个剧情里所有的小故事点哦
    private int currentPlotIndex = 0; // 现在演到第几个剧情啦，记一下~

    private final Map<String, ResourceLocation> cachedTextures = new HashMap<>();

    // 这些东西换场景也不会不见哦
    private String activeStandPicturePath;
    private String activeCharacterName;
    private String activeMusicPath;

    // 字宝宝一个个蹦出来的设置~
    private int textDisplayProgress;
    private long lastUpdateTime;
    private static final int CHAR_DELAY = 20; // 每个字蹦出来要等这么久哦 (单位是毫秒啦)

    // 东西都放哪里的参数~
    private static final int TEXT_WINDOW_HEIGHT = 80; // 说话的框框多高
    private static final int PORTRAIT_WIDTH = 200; // 小人儿图片多宽
    private static final int PORTRAIT_HEIGHT = 269; // 小人儿图片多高
    private static final int PORTRAIT_X_OFFSET = 20; // 小人儿图片往右边挪多少
    private static final int TEXT_AREA_SIDE_MARGIN = 20; // 说话的框框，字离边边多远

    public DialogueScreen(GalParser parser) {
        super(Component.empty());
        this.parser = parser;

        // 一开始，这些东西都先空着~
        this.activeStandPicturePath = null;
        this.activeCharacterName = null;
        this.activeMusicPath = null;

        if (this.parser.getPlotParsers() != null && !this.parser.getPlotParsers().isEmpty()) {
            // 如果有很多剧情，就从第一个开始好啦
            this.currentPlotIndex = 0;
            this.currentPlotNodes = this.parser.getPlotParsers().get(this.currentPlotIndex);
        } else {
            Gal.LOGGER.error("哎呀！解析器里空空的，没有剧情数据耶。对话界面动不了啦！");
            this.currentPlotNodes = new HashMap<>(); // 弄个空的，这样就不会直接坏掉，不过屏幕可能要关掉或者卡住啦
        }
        loadNode("0"); // 从“0”号小故事点开始吧！
    }

    @Override
    protected void init() {
        super.init();
        // 如果屏幕大小变了啥的，字又刚好显示完，按钮要重新放上去哦。
        if (currentNode != null && currentNode.getText() != null && textDisplayProgress >= currentNode.getText().length()) {
            updateButtons();
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateTextProgress();
    }

    private void loadNode(String nodeId) {
        if (nodeId == null || nodeId.equalsIgnoreCase("end")) {
            onClose(); // 结束啦，关掉吧~
            return;
        }

        // 如果说“换个剧情！”，就要听话哦
        if (nodeId.startsWith("plot:")) {
            String plotName = nodeId.substring(5);
            Gal.LOGGER.info("嗯哼？想要换到 '{}' 这个剧情吗？试试看！", plotName);

            List<GalInfo.PlotInfo> plotInfos = parser.getGalInfo().getPlots();
            boolean switched = false;
            for (int i = 0; i < plotInfos.size(); i++) {
                if (plotInfos.get(i).getName().equals(plotName)) {
                    if (parser.getPlotParsers() != null && i < parser.getPlotParsers().size()) {
                        this.currentPlotIndex = i;
                        this.currentPlotNodes = parser.getPlotParsers().get(i);
                        Gal.LOGGER.info("换好啦！现在是剧情 '{}' (是第 {} 个哦)。正在加载它的 '0' 号小故事点。", plotName, i);
                        loadNode("0"); // 然后，再悄悄地喊一声 loadNode，加载新剧情的“0”号节点~
                        switched = true;
                    } else {
                        Gal.LOGGER.error("呜呜，剧情 '{}' 在 GalInfo 列表里找到了，但是在第 {} 个位置没有解析好的数据呀。", plotName, i);
                    }
                    break; // 找到剧情啦，不找了不找了。
                }
            }


            if (switched) {
                return; // 换剧情成功啦！新剧情的“0”号节点正在加载，或者已经加载好咯。
            } else {
                Gal.LOGGER.error("换到剧情 '{}' 失败了QAQ。可能 GalInfo 里没有这个剧情，或者数据不见了。还是继续现在的剧情吧。", plotName);
                // 没办法啦，那就把 "plot:..." 当成现在这个剧情里一个普通的小节点名字试试看。
            }
        }

        // 现在，从 currentPlotNodes（可能是原来的，也可能是刚换的那个剧情）里加载小节点吧
        if (this.currentPlotNodes == null) { // （这个应该在一开始就准备好的呀...）
            Gal.LOGGER.error("currentPlotNodes 居然是空的！加载不了 '{}' 这个节点了，屏幕要关掉啦。", nodeId);
            onClose();
            return;
        }

        if (this.currentPlotNodes.containsKey(nodeId)) {
            currentNode = this.currentPlotNodes.get(nodeId);
            resetTextDisplay();
            handleNodeResources(); // 这个会更新角色名、立绘路径、音乐路径什么的
            updateButtons(); // 把旧按钮清掉，如果字出完了又有选项，就加上新按钮
        } else {
            // 在现在的剧情里找不到这个小节点呢
            String currentPlotName = "不知道是哪个剧情 (编号 " + currentPlotIndex + ")";
            if (parser.getGalInfo() != null && parser.getGalInfo().getPlots().size() > currentPlotIndex) {
                currentPlotName = parser.getGalInfo().getPlots().get(currentPlotIndex).getName();
            }

            Gal.LOGGER.warn("嗯？节点 '{}' 在当前剧情 '{}' 里好像不见了耶。", nodeId, currentPlotName);
            if (!"0".equals(nodeId)) { // 要是连“0”号节点都丢了，可不能一直傻傻地找下去哦
                Gal.LOGGER.info("找不到的话，试着加载当前剧情 '{}' 的起始节点 '0' 看看吧，说不定行呢。", currentPlotName);
                loadNode("0"); // 没办法，退回一步，加载当前剧情的“0”号节点吧
            } else {
                // 坏了坏了！当前剧情的起始节点 '0' 都不见了！这个剧情没法用了！
                Gal.LOGGER.error("糟糕！初始节点 '0' 在当前剧情 '{}' 里找不到啦！这个剧情加载不了了呜。", currentPlotName);
                onClose(); // 这个剧情救不回来了...
            }
        }
    }


    private void resetTextDisplay() {
        textDisplayProgress = 0; // 字数进度清零~
        lastUpdateTime = System.currentTimeMillis(); // 更新一下时间~
    }

    private void handleNodeResources() {
        if (currentNode == null) return;

        // 第一步：看看是谁在说话~
        String newCharacterName = currentNode.getCharacter();
        if (newCharacterName != null) { // 如果小节点里写了角色名字...
            if (newCharacterName.equalsIgnoreCase("none")) {
                this.activeCharacterName = null; // 如果写的是 "none"，意思就是“暂时没人说话哦”
            } else {
                this.activeCharacterName = newCharacterName; // 换成新的名字！
            }
        }
        // 如果没写新名字，那就还是之前那个角色在说话啦。

        // 第二步：看看要不要换个小人儿图片~
        String newStandPicture = currentNode.getStandPicture();
        if (newStandPicture != null) { // 如果小节点里写了立绘路径...
            if (newStandPicture.equalsIgnoreCase("none")) {
                this.activeStandPicturePath = null; // "none" 就是不要图片啦
            } else {
                // 只有图片真的变了，才重新加载哦，不然多浪费呀~
                if (!newStandPicture.equals(this.activeStandPicturePath)) {
                    this.activeStandPicturePath = newStandPicture;
                    loadPortrait(this.activeStandPicturePath);
                }
            }
        }
        // 如果没写新立绘，那旧的立绘就继续摆着好啦。

        // 第三步：背景音乐时间！
        // 先看看播放器在不在岗...
        if (parser.getSoundPlayer() != null) {
            String newMusic = currentNode.getMusic();
            if (newMusic != null) { // 如果小节点里写了音乐...
                if (newMusic.equalsIgnoreCase("none")) {
                    if (this.activeMusicPath != null) { // 只有真的在放歌的时候才需要喊停嘛
                        parser.getSoundPlayer().stopMusic(); // （悄悄说：SoundPlayer应该有个stopMusic()方法的吧...）
                        this.activeMusicPath = null;
                    }
                } else {
                    // 如果是新歌，或者之前没歌，那就放起来！
                    if (!newMusic.equals(this.activeMusicPath)) {
                        parser.getSoundPlayer().playMusic(newMusic, true);
                        this.activeMusicPath = newMusic;
                    }
                }
            }
            // 如果没指定新音乐，那旧的音乐就继续放着好啦。
        }


        // 第四步：角色配音！这个是每个节点说一句就没啦，不会一直留着。
        if (currentNode.getVoice() != null && parser.getSoundPlayer() != null) {
            // 配音这里的 "none" 好像不是用来停掉那种一直响的配音的。
            // 如果是的话，就要像音乐和立绘那样处理了。
            // 现在呢，如果指定了配音就放，没指定的话，这个节点就不管配音的事儿啦。
            if (currentNode.getVoice().equalsIgnoreCase("none")) {
                // 如果 "none" 的意思是把现在正在说的配音停掉（就算不是一直放的那种）...
                parser.getSoundPlayer().stopVoice(); //
            } else {
                parser.getSoundPlayer().playVoice(currentNode.getVoice());
            }
        }
    }

    private void loadPortrait(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return;

        if (!cachedTextures.containsKey(imagePath)) {
            try (InputStream is = parser.getPictureReader().readStandPicture(imagePath)) {
                if (is == null) {
                    Gal.LOGGER.error("哎呀，立绘 '{}' 的输入流是空的，读不了图片啦！", imagePath);
                    cachedTextures.put(imagePath, null); // 记下来这个是坏的，下次不试了，免得又出错。
                    return;
                }
                ResourceLocation texture = TextureUtil.registerTexture(imagePath, is);
                cachedTextures.put(imagePath, texture);
            } catch (IOException e) {
                Gal.LOGGER.error("加载立绘 '{}' 失败了呢，呜呜呜。", imagePath, e);
                cachedTextures.put(imagePath, null); // 出错了，也记下来，下次不白费力气啦。
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // this.renderBackground(guiGraphics); // 看你想不想要那个默认的黑乎乎背景啦，可加可不加~

        // 把小人儿画出来~
        renderPortraits(guiGraphics);

        // 画那个说话的框框~
        renderTextWindow(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick); // 按钮和其他小部件也画一下
    }

    private void renderPortraits(GuiGraphics guiGraphics) {
        if (activeStandPicturePath != null) { // 用那个一直有效的图片路径哦
            ResourceLocation texture = cachedTextures.get(activeStandPicturePath);
            if (texture != null) { // 看看图片加载好了没...
                // 小人儿要画在说话框框的上面，靠左边一点。算算高度...
                int portraitActualY = this.height - TEXT_WINDOW_HEIGHT - PORTRAIT_HEIGHT;
                // 别让小人儿的头顶到屏幕外面去啦，万一窗口太矮了呢。
                portraitActualY = Math.max(0, portraitActualY);

                guiGraphics.blit(texture, PORTRAIT_X_OFFSET, portraitActualY,
                        PORTRAIT_WIDTH, PORTRAIT_HEIGHT,
                        0, 0, PORTRAIT_WIDTH, PORTRAIT_HEIGHT,
                        PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
            }
        }
    }

    private void renderTextWindow(GuiGraphics guiGraphics) {
        // 先给框框涂个底色！
        guiGraphics.fill(0, height - TEXT_WINDOW_HEIGHT, width, height, 0xCC000000);

        if (currentNode == null) return;

        int nameAndCvLineY = height - TEXT_WINDOW_HEIGHT + 10;
        // 说话的内容要在名字和CV的下面，还要给字体留点空地方。
        int dialogueTextY = nameAndCvLineY + font.lineHeight + 5; // 再加一点点空隙，好看！

        // 把说话的角色的名字显示出来（用的是那个一直有效的 activeCharacterName 哦）
        if (activeCharacterName != null && !activeCharacterName.isEmpty()) {
            guiGraphics.drawString(font, activeCharacterName,
                    TEXT_AREA_SIDE_MARGIN, nameAndCvLineY,
                    0xFFFFFF);
        }

        // 显示CV大大（这个是当前节点指定的）
        String cvName = currentNode.getCV();
        if (cvName != null && !cvName.isEmpty() && !cvName.equalsIgnoreCase("none")) {
            int cvNameWidth = font.width(cvName);
            // CV大大的名字放在说话框框的右边。
            int cvX = width - TEXT_AREA_SIDE_MARGIN - cvNameWidth;
            guiGraphics.drawString(font, "CV: "+cvName,
                    cvX, nameAndCvLineY,
                    0xCCCCCC); // CV名字用灰灰的颜色，低调奢华有内涵（不是）
        }


        // 说话的内容（一个字一个字蹦出来那种~）
        String fullText = currentNode.getText();
        if (fullText == null) fullText = "";
        String displayedText = fullText.substring(0, Math.min(textDisplayProgress, fullText.length()));

        guiGraphics.drawWordWrap(font, Component.literal(displayedText),
                TEXT_AREA_SIDE_MARGIN,
                dialogueTextY,
                width - (2 * TEXT_AREA_SIDE_MARGIN), // 字太多了会自动换行，这是最宽能写多少
                0xFFFFFF);
    }

    private void updateTextProgress() {
        if (currentNode == null || currentNode.getText() == null) return;

        if (textDisplayProgress < currentNode.getText().length()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime >= CHAR_DELAY) { // 用 >= 会更稳妥一点，万一不小心跳过了一点点时间呢~
                textDisplayProgress++;
                lastUpdateTime = currentTime;
                if (textDisplayProgress >= currentNode.getText().length()) {
                    updateButtons(); // 字全部显示完啦，如果有选项的话，快把它们拿出来！
                }
            }
        }
    }

    private void updateButtons() {
        clearWidgets(); // 先把旧的按钮擦掉~

        if (currentNode == null || currentNode.getText() == null) return;

        // 只有当所有字都显示完了，才把按钮摆出来哦。
        if (textDisplayProgress >= currentNode.getText().length()) {
            if (currentNode.getChoices() != null && !currentNode.getChoices().isEmpty()) {
                int buttonWidth = 180; // 按钮多宽
                int buttonHeight = 20; // 按钮多高
                int buttonMargin = 5; // 按钮和按钮之间，上下隔开一点点。
                int numChoices = currentNode.getChoices().size();

                // 按钮们靠右边站队，离屏幕右边留20像素的空。
                int buttonX = this.width - buttonWidth - 20;

                // 算一下所有按钮堆在一起有多高，这样好决定它们从哪里开始放。
                int totalButtonBlockHeight = numChoices * buttonHeight + Math.max(0, numChoices - 1) * buttonMargin;

                // 按钮们要放在说话框框的上面，排排坐。
                int topYForAllButtons = this.height - TEXT_WINDOW_HEIGHT - buttonMargin - totalButtonBlockHeight;
                // 别让按钮跑到屏幕顶上去啦，或者挡住其他东西就不好了。离顶上至少留10像素。
                topYForAllButtons = Math.max(10, topYForAllButtons);

                for (int i = 0; i < numChoices; i++) {
                    PlotParser.Choice choice = currentNode.getChoices().get(i);
                    int currentButtonY = topYForAllButtons + (i * (buttonHeight + buttonMargin));
                    addRenderableWidget(Button.builder(Component.literal(choice.getText()),
                                    button -> handleChoice(choice))
                            .pos(buttonX, currentButtonY)
                            .size(buttonWidth, buttonHeight)
                            .build());
                }
            }
        }
    }

    private void handleChoice(PlotParser.Choice choice) {
        // choice.nextShouldEnd() 这个方法会检查下一步是不是空、没有或者写着 "end" 哦。
        if (choice.nextShouldEnd()) {
            onClose();
        } else {
            // 把 'next' 里面写的东西（可能是节点ID，也可能是 "plot:剧情名"）直接丢给 loadNode 去处理。
            loadNode(choice.getNext());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonType) {
        // 先问问按钮它们要不要这个点击事件~
        if (super.mouseClicked(mouseX, mouseY, buttonType)) {
            return true;
        }

        // 如果按钮们都不要，那屏幕自己来处理吧。
        if (buttonType == 0) { // 如果点的是鼠标左键...
            if (currentNode == null || currentNode.getText() == null) {
                // 万一状态不对了，试着抢救一下...
                Gal.LOGGER.warn("咦？鼠标点点的时候，节点或者文字不太对劲。我试试加载 '0' 号起始节点看看...");
                loadNode("0");
                return true;
            }

            if (textDisplayProgress < currentNode.getText().length()) {
                // 如果字还在一个一个蹦，就让它们“咻”的一下全出来！
                textDisplayProgress = currentNode.getText().length();
                lastUpdateTime = System.currentTimeMillis(); // （这个对那个 CHAR_DELAY 计时器挺重要的，如果它要求很严格的话）
                updateButtons(); // 字出完了，赶紧看看是不是该显示按钮了。
            } else {
                // 字已经全部显示好啦。
                if (currentNode.getChoices() == null || currentNode.getChoices().isEmpty()) {
                    // 如果没有选项，那就点一下继续下一个小故事点。
                    proceedToNextNode();
                }
                // 如果有选项摆在那，点屏幕其他地方（不是点按钮）就啥也不干。
            }
            return true; // 左键点击事件被我吃掉啦！
        }
        return false; // 这个点击没人理...
    }

    private void proceedToNextNode() {
        if (currentNode == null) { // （保险起见啦，正常应该不会这样的...）
            Gal.LOGGER.warn("想要继续下一个节点，但是 currentNode 居然是空的！我试试加载 '0' 号节点...");
            loadNode("0");
            return;
        }
        // currentNode.nextShouldEnd() 这个方法会检查下一步是不是空、没有或者写着 "end" 哦。
        if (currentNode.nextShouldEnd()) {
            onClose();
        } else {
            // 把 'next' 里面写的东西（可能是节点ID，也可能是 "plot:剧情名"）直接丢给 loadNode 去处理。
            loadNode(currentNode.getNext());
        }
    }

    @Override
    public void onClose() {
        if (this.parser != null && this.parser.getSoundPlayer() != null) {
            this.parser.getSoundPlayer().stopAll(); // 把这个解析器弄出来的所有声音（音乐呀，配音呀）都关掉！
        }

        if (minecraft != null) {
            cachedTextures.values().forEach(texture -> {
                if (texture != null) minecraft.getTextureManager().release(texture); // 把用过的图片们都放回去~
            });
        }
        cachedTextures.clear(); // 清空缓存的小篮子~
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 我这个界面出来的时候，游戏不会暂停哦~
    }
}
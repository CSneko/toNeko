package org.cneko.gal.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.cneko.gal.common.Gal;
import org.cneko.gal.common.client.parser.GalInfo;
import org.cneko.gal.common.client.parser.GalParser;
import org.cneko.gal.common.client.parser.GalPictureReader;
import org.cneko.gal.common.client.parser.PlotParser;
import org.cneko.gal.common.util.TextureUtil;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DialogueScreen extends Screen {
    private final GalParser parser;
    private PlotParser.DialogueNode currentNode;
    private Map<String, PlotParser.DialogueNode> currentPlotNodes; // 这里放着现在这个剧情里所有的小故事点哦~
    private int currentPlotIndex = 0; // 现在演到第几个剧情啦，要记一下下~

    private final Map<String, ResourceLocation> cachedTextures = new HashMap<>();

    // 这些东东在换场景的时候也会保留，除非节点明确说要改掉它们
    private String activeStandPicturePath; // 当前活动立绘图片的路径哦
    private PlotParser.StandPicture lastValidStandPictureInfo; // 保存上一个有效的 StandPicture 对象，用来画画的细节~
    private int lastValidStandPictureHeight = 0;
    private int lastValidStandPictureWidth = 0;
    private String activeCharacterName;
    private String activeMusicPath;

    // 大图图模式
    private String currentBigPicturePath; // 当前活动大图图的路径哦
    private ResourceLocation activeBigPictureTexture; // 大图图的纹理~
    private boolean showTextWindow; // 在大图图模式下，右键可以切换这个哦

    // 文字蹦蹦跳设置
    private int textDisplayProgress;
    private long lastUpdateTime;
    private static final int CHAR_DELAY = 20; // ms，每个字蹦出来要等这么久

    // 布局参数~
    private static final int TEXT_WINDOW_HEIGHT = 80; // 说话的框框多高呀
    private static final int TEXT_AREA_SIDE_MARGIN = 40; // 说话的框框，字离边边多远呢
    private static final float TEXT_SCALE_FACTOR = 1.1f; // 文字放大 10% 哦~

    public DialogueScreen(GalParser parser) {
        super(Component.empty());
        this.parser = parser;

        // 一开始呀，这些东东都先空着好啦~
        this.activeStandPicturePath = null;
        this.lastValidStandPictureInfo = null;
        this.activeCharacterName = null;
        this.activeMusicPath = null;
        this.currentBigPicturePath = null;
        this.activeBigPictureTexture = null;
        this.showTextWindow = true;


        if (this.parser.getPlotParsers() != null && !this.parser.getPlotParsers().isEmpty()) {
            // 如果有很多剧情，就从第一个开始好啦
            this.currentPlotIndex = 0;
            this.currentPlotNodes = this.parser.getPlotParsers().get(this.currentPlotIndex);
        } else {
            Gal.LOGGER.error("哎呀呀！解析器里空空的，木有剧情数据耶。对话界面要罢工啦！");
            this.currentPlotNodes = new HashMap<>(); // 先弄个空的吧，这样就不会马上坏掉，不过屏幕可能会关掉或者卡住住哦。
        }
        loadNode("0"); // 就从“0”号小故事点开始吧！
    }

    @Override
    protected void init() {
        super.init();
        // 如果屏幕大小变了什么的，字又刚好显示完的话，按钮要重新放上去哦~
        // 嗯嗯，还要想想大图模式按钮怎么更新，updateButtons会搞定这个哒。
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        // 只有在不是大图模式，或者大图模式也显示文字框框的时候，才更新文字进度哦~
        if (!(currentBigPicturePath != null && activeBigPictureTexture != null && !showTextWindow)) {
            updateTextProgress();
        }
    }

    private void loadNode(String nodeId) {
        if (nodeId == null || nodeId.equalsIgnoreCase("end")) {
            onClose(); // 结束啦，关掉好啦~
            return;
        }

        // 如果说“换个剧情！”，那就要乖乖听话哦~
        if (nodeId.startsWith("plot:")) {
            String plotName = nodeId.substring(5);
            Gal.LOGGER.info("嗯哼？是想要换到 '{}' 这个剧情咩？窝来试试看！", plotName);

            List<GalInfo.PlotInfo> plotInfos = parser.getGalInfo().getPlots();
            boolean switched = false;
            for (int i = 0; i < plotInfos.size(); i++) {
                if (plotInfos.get(i).getName().equals(plotName)) {
                    if (parser.getPlotParsers() != null && i < parser.getPlotParsers().size()) {
                        this.currentPlotIndex = i;
                        this.currentPlotNodes = parser.getPlotParsers().get(i);
                        Gal.LOGGER.info("换好啦！现在是剧情 '{}' (是第 {} 个哦~)。正在加载它的 '0' 号小故事点点。", plotName, i);
                        loadNode("0"); // 然后呀，再悄悄地喊一声 loadNode，加载新剧情的“0”号小节点~
                        switched = true;
                    } else {
                        Gal.LOGGER.error("呜呜呜，剧情 '{}' 在 GalInfo 列表里是找到了，可是在第 {} 个位置木有解析好的数据呀QAQ。", plotName, i);
                    }
                    break; // 找到剧情啦，不找啦不找啦。
                }
            }

            if (switched) {
                return; // 换剧情成功啦！新剧情的“0”号节点正在加载，或者已经加载好咯~
            } else {
                Gal.LOGGER.error("换到剧情 '{}' 失败了QAQ。可能 GalInfo 里没有这个剧情，或者数据不见了。还是继续现在的剧情好啦。", plotName);
            }
        }

        if (this.currentPlotNodes == null) {
            Gal.LOGGER.error("currentPlotNodes 居然是空的耶！加载不了 '{}' 这个节点了，屏幕要关掉掉啦。", nodeId);
            onClose();
            return;
        }

        if (this.currentPlotNodes.containsKey(nodeId)) {
            currentNode = this.currentPlotNodes.get(nodeId);

            boolean isOldBigPictureAlive = currentBigPicturePath != null;

            // 把上一个节点的大图图状态清掉~
            this.currentBigPicturePath = null;
            this.activeBigPictureTexture = null;

            PlotParser.BigPicture bigPicture = currentNode.getBigPicture();
            if (bigPicture != null && !bigPicture.getName().isEmpty()) {
                this.currentBigPicturePath = bigPicture.getName();
                loadBigPicture(this.currentBigPicturePath);
                if (!bigPicture.isShowText()){
                    this.showTextWindow = false;
                }
            }else if (isOldBigPictureAlive){
                showTextWindow = true;
            }

            // 处理其他的资源，比如角色、立绘、音乐、语音啥的
            // 如果大图图开着，立绘会加载但默认不会画出来哦。
            handleNodeResources();
            resetTextDisplay();
            updateButtons();
        } else {
            String currentPlotName = "不知道是哪个剧情 (编号 " + currentPlotIndex + ")";
            if (parser.getGalInfo() != null && parser.getGalInfo().getPlots().size() > currentPlotIndex) {
                currentPlotName = parser.getGalInfo().getPlots().get(currentPlotIndex).getName();
            }

            Gal.LOGGER.warn("嗯？节点 '{}' 在当前剧情 '{}' 里好像不见了耶。", nodeId, currentPlotName);
            if (!"0".equals(nodeId)) {
                Gal.LOGGER.info("找不到的话，试着加载当前剧情 '{}' 的起始节点 '0' 看看吧，说不定可以呢。", currentPlotName);
                loadNode("0");
            } else {
                Gal.LOGGER.error("糟糕！初始节点 '0' 在当前剧情 '{}' 里找不到啦！这个剧情加载不了了呜呜呜。", currentPlotName);
                onClose();
            }
        }
    }


    private void resetTextDisplay() {
        textDisplayProgress = 0; // 字数进度清零零~
        lastUpdateTime = System.currentTimeMillis(); // 更新一下时间好啦~
    }

    private void handleNodeResources() {
        if (currentNode == null) return;

        // 角色名字~
        String newCharacterName = currentNode.getCharacter();
        if (newCharacterName != null) {
            if (newCharacterName.equalsIgnoreCase("none")) {
                this.activeCharacterName = null;
            } else {
                this.activeCharacterName = newCharacterName;
            }
        }

        // 立绘图片~
        // 这段逻辑还留着，不过如果大图模式开着，就不会画立绘啦。
        PlotParser.StandPicture newStandPicInfoFromNode = currentNode.getStandPicture();
        if (newStandPicInfoFromNode != null) {
            this.lastValidStandPictureInfo = newStandPicInfoFromNode;
            String newImageName = newStandPicInfoFromNode.getName();
            if (newImageName != null) {
                if (newImageName.equalsIgnoreCase("none")) {
                    this.activeStandPicturePath = null;
                    this.lastValidStandPictureInfo = null;
                } else {
                    if (!newImageName.equals(this.activeStandPicturePath)) { // 如果现在没有活动的立绘，也要加载哦~
                        this.activeStandPicturePath = newImageName;
                        loadPortrait(this.activeStandPicturePath);
                    }
                }
            }
        }


        // 背景音乐~
        if (parser.getSoundPlayer() != null) {
            String newMusic = currentNode.getMusic();
            if (newMusic != null) {
                if (newMusic.equalsIgnoreCase("none")) {
                    if (this.activeMusicPath != null) {
                        parser.getSoundPlayer().stopMusic();
                        this.activeMusicPath = null;
                    }
                } else {
                    if (!newMusic.equals(this.activeMusicPath)) {
                        parser.getSoundPlayer().playMusic(newMusic, true);
                        this.activeMusicPath = newMusic;
                    }
                }
            }
        }

        // 角色语音~
        if (currentNode.getVoice() != null && parser.getSoundPlayer() != null) {
            if (currentNode.getVoice().equalsIgnoreCase("none")) {
                parser.getSoundPlayer().stopVoice();
            } else {
                parser.getSoundPlayer().playVoice(currentNode.getVoice());
            }
        }
    }

    private void loadPortrait(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            Gal.LOGGER.warn("想要加载立绘，但是路径是空的或者空空的字符串耶~");
            return;
        }

        if (cachedTextures.containsKey(imagePath)) {
            if (cachedTextures.get(imagePath) == null) {
                Gal.LOGGER.debug("立绘'{}'之前加载失败过啦，这次就跳过，不试啦~", imagePath);
            }
            // 如果已经缓存过了（就算是空的缓存），就不要再加载啦。
            // 真正画画的时候，会从缓存里拿图片哒。
            return;
        }

        try {
            Gal.LOGGER.debug("正在努力加载立绘: {} ~", imagePath);
            GalPictureReader.Picture picture = parser.getPictureReader().readStandPicture(imagePath);
            if (picture == null || picture.stream() == null) {
                Gal.LOGGER.error("拿不到立绘'{}'的输入流或者图片对象耶，是不是路径写错啦？", imagePath);
                cachedTextures.put(imagePath, null);
                return;
            }
            InputStream is = picture.stream();
            lastValidStandPictureHeight = picture.height();
            lastValidStandPictureWidth = picture.width();

            try (is) { // 用 try-with-resources 来乖乖关掉流哦~
                ResourceLocation texture = TextureUtil.registerTexture(imagePath, is);
                cachedTextures.put(imagePath, texture);
                Gal.LOGGER.debug("立绘: {} 加载成功啦！好耶！", imagePath);
            } catch (IOException e) {
                Gal.LOGGER.error("关闭立绘 '{}' 输入流的时候，出错了呜呜~", imagePath, e);
                cachedTextures.put(imagePath, null);
            } catch (Exception e) {
                Gal.LOGGER.error("注册立绘纹理'{}'的时候，也出错了QAQ~", imagePath, e);
                cachedTextures.put(imagePath, null);
            }
        } catch (Exception e) {
            Gal.LOGGER.error("加载立绘'{}'的时候，发生了意想不到的状况呢~", imagePath, e);
            cachedTextures.put(imagePath, null);
        }
    }

    private void loadBigPicture(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            Gal.LOGGER.warn("想加载大图图，但是路径是空的或者空空的字符串耶~");
            this.activeBigPictureTexture = null;
            return;
        }

        if (cachedTextures.containsKey(imagePath)) {
            this.activeBigPictureTexture = cachedTextures.get(imagePath);
            if (this.activeBigPictureTexture == null) {
                Gal.LOGGER.debug("大图图'{}'之前加载失败过啦，这次就跳过，不试啦~", imagePath);
            }
            return;
        }

        try {
            Gal.LOGGER.debug("正在努力加载大图图: {} ~", imagePath);
            GalPictureReader.Picture picture = parser.getPictureReader().readBigPicture(imagePath); // 用 readBigPicture 哦~
            if (picture == null || picture.stream() == null) {
                Gal.LOGGER.error("拿不到大图图'{}'的输入流或者图片对象耶，是不是路径写错啦？", imagePath);
                cachedTextures.put(imagePath, null);
                this.activeBigPictureTexture = null;
                return;
            }

            try (InputStream is = picture.stream()) {
                ResourceLocation texture = TextureUtil.registerTexture(imagePath, is);
                cachedTextures.put(imagePath, texture);
                this.activeBigPictureTexture = texture;
                Gal.LOGGER.debug("大图图: {} 加载成功啦！棒棒！", imagePath);
            } catch (IOException e) {
                Gal.LOGGER.error("关闭大图图 '{}' 输入流的时候，出错了呜呜~", imagePath, e);
                cachedTextures.put(imagePath, null);
                this.activeBigPictureTexture = null;
            } catch (Exception e) {
                Gal.LOGGER.error("注册大图图纹理'{}'的时候，也出错了QAQ~", imagePath, e);
                cachedTextures.put(imagePath, null);
                this.activeBigPictureTexture = null;
            }
        } catch (Exception e) {
            Gal.LOGGER.error("加载大图图'{}'的时候，发生了意想不到的状况呢~", imagePath, e);
            cachedTextures.put(imagePath, null);
            this.activeBigPictureTexture = null;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (currentBigPicturePath != null && activeBigPictureTexture != null) {
            renderBigPicture(guiGraphics);
        } else {
            renderPortraits(guiGraphics);
        }
        if (showTextWindow) {
            renderTextWindow(guiGraphics); // 角色名字和CV也在这里面画~
        }

        // 在左上角显示音乐的名字哦~
        if (activeMusicPath != null && !activeMusicPath.isEmpty()) {
            String musicDisplayName = getMusicDisplayName();
            guiGraphics.drawString(font, "♪ " + musicDisplayName, 5, 5, 0xFFFFFF); // 白白色的字~
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick); // 按钮和其他小部件也要画一下下~
    }

    private void renderBigPicture(GuiGraphics guiGraphics) {
        if (activeBigPictureTexture != null) {
            guiGraphics.blit(activeBigPictureTexture, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        }
    }

    private @NotNull String getMusicDisplayName() {
        String musicDisplayName = activeMusicPath;
        if (musicDisplayName.contains("/")) {
            musicDisplayName = musicDisplayName.substring(musicDisplayName.lastIndexOf('/') + 1);
        }
        if (musicDisplayName.contains("\\")) { // 万一是 Windows 的路径呢~
            musicDisplayName = musicDisplayName.substring(musicDisplayName.lastIndexOf('\\') + 1);
        }
        int dotIndex = musicDisplayName.lastIndexOf('.');
        if (dotIndex > 0) { // 如果有点点后缀名，就把它去掉~
            musicDisplayName = musicDisplayName.substring(0, dotIndex);
        }
        return musicDisplayName;
    }

    private void renderPortraits(GuiGraphics guiGraphics) {
        // 这个方法呀，只有在不是大图模式的时候才会被叫到哦。
        if (activeStandPicturePath != null && this.lastValidStandPictureInfo != null) {
            ResourceLocation texture = cachedTextures.get(activeStandPicturePath); // 从缓存里拿出来~
            PlotParser.StandPicture standPictureInfo = this.lastValidStandPictureInfo;

            if (texture != null && standPictureInfo != null) {
                String nameInInfo = standPictureInfo.getName();
                if (nameInInfo != null && nameInInfo.equalsIgnoreCase("none")) {
                    return; // 如果逻辑没问题的话，这里应该不会发生，activeStandPicturePath 应该是空的才对。
                }

                int picWidth = standPictureInfo.getWidth();
                int picHeight = standPictureInfo.getHeight();

                // 如果给的尺寸不对，就用图片原来的大小好啦~
                if ((picWidth <= 0 || picHeight <= 0) && lastValidStandPictureWidth > 0 && lastValidStandPictureHeight > 0) {
                    picHeight = (int) (this.height * 0.9); // 默认是屏幕高度的 90% 哦~
                    picWidth = picHeight * lastValidStandPictureWidth / lastValidStandPictureHeight; // 保持原来的长宽比~
                } else if (picWidth <=0 || picHeight <=0) {
                    // 要是连图片原来的大小都不知道，那就没办法啦~
                    Gal.LOGGER.warn("立绘 '{}' 的尺寸信息不对耶 (宽:{}, 高:{}), 而且也没有缓存的尺寸，可能画不出来或者画得怪怪的哦。", activeStandPicturePath, picWidth, picHeight);
                    return; // 没有正确的尺寸，画不了啦~
                }


                float xOffsetPercent = standPictureInfo.getXOffset();
                float yOffsetPercent = standPictureInfo.getYOffset();

                if (xOffsetPercent <= 0){
                    float widthPercent = (float) picWidth / (float) this.width;
                    xOffsetPercent = (1- widthPercent)/2f; // 默认放中间~
                }
                if (yOffsetPercent <= 0){
                    yOffsetPercent = 0.1f; // 默认离下面 10% 的地方~
                }
                int portraitX = (int) (this.width * xOffsetPercent);
                int imageBottomY = (int) (this.height * (1.0f - yOffsetPercent));
                int portraitY = imageBottomY - picHeight;

                guiGraphics.blit(texture, portraitX, portraitY,
                        0, 0,
                        picWidth, picHeight,
                        picWidth, picHeight // 如果不用整张图的话，这里用 picWidth, picHeight 作为图集的大小~
                );
            }
        }
    }


    private void renderTextWindow(GuiGraphics guiGraphics) {
        // 这个方法现在是看情况叫的，要看是不是大图模式，还有文字框框是不是要显示。
        int topColor = 0xAAFFC0CB; // 淡淡的粉红色，几乎不透明~
        int bottomColor = 0x00FFC0CB; // 淡淡的粉红色，完全透明 (渐变到透明哦~)

        guiGraphics.fillGradient(0, this.height - TEXT_WINDOW_HEIGHT,
                this.width, this.height,
                topColor, bottomColor);

        if (currentNode == null) return;

        float scaledFontHeight = font.lineHeight * TEXT_SCALE_FACTOR;
        int nameAndCvLineY = height - TEXT_WINDOW_HEIGHT + 10;
        float actualNameLineHeight = (activeCharacterName != null && !activeCharacterName.isEmpty()) || (currentNode.getCV() != null && !currentNode.getCV().isEmpty() && !currentNode.getCV().equalsIgnoreCase("none")) ? scaledFontHeight : font.lineHeight;
        int dialogueTextY = nameAndCvLineY + (int)actualNameLineHeight + 5;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100);

        if (activeCharacterName != null && !activeCharacterName.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(TEXT_AREA_SIDE_MARGIN, nameAndCvLineY, 0);
            guiGraphics.pose().scale(TEXT_SCALE_FACTOR, TEXT_SCALE_FACTOR, 1.0f);
            guiGraphics.drawString(font, activeCharacterName, 0, 0, 0xFFFFFF);
            guiGraphics.pose().popPose();
        }

        String cvName = currentNode.getCV();
        if (cvName != null && !cvName.isEmpty() && !cvName.equalsIgnoreCase("none")) {
            String cvText = "CV: " + cvName;
            int cvNameWidth = (int)(font.width(cvText) * TEXT_SCALE_FACTOR);
            int cvX = width - TEXT_AREA_SIDE_MARGIN - cvNameWidth;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(cvX, nameAndCvLineY, 0);
            guiGraphics.pose().scale(TEXT_SCALE_FACTOR, TEXT_SCALE_FACTOR, 1.0f);
            guiGraphics.drawString(font, cvText, 0, 0, 0xCCCCCC);
            guiGraphics.pose().popPose();
        }

        String fullText = currentNode.getText();
        if (fullText == null) fullText = "";
        String displayedText = fullText.substring(0, Math.min(textDisplayProgress, fullText.length()));

        if (!displayedText.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(TEXT_AREA_SIDE_MARGIN, dialogueTextY, 0);
            guiGraphics.pose().scale(TEXT_SCALE_FACTOR, TEXT_SCALE_FACTOR, 1.0f);
            int wrapWidth = (int)((width - (2 * TEXT_AREA_SIDE_MARGIN)) / TEXT_SCALE_FACTOR);
            guiGraphics.drawWordWrap(font, Component.literal(displayedText),
                    0, 0, wrapWidth, 0xFFFFFF);
            guiGraphics.pose().popPose();
        }
        guiGraphics.pose().popPose();
    }

    private void updateTextProgress() {
        if (currentNode == null || currentNode.getText() == null) return;

        if (textDisplayProgress < currentNode.getText().length()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime >= CHAR_DELAY) {
                textDisplayProgress++;
                lastUpdateTime = currentTime;
                if (textDisplayProgress >= currentNode.getText().length()) {
                    updateButtons(); // 字显示完啦，更新一下按钮~
                }
            }
        }
    }

    private void updateButtons() {
        clearWidgets();

        // 如果这个节点正在显示大图图，那就不显示选项按钮啦，
        // 因为大图图节点会忽略文字选项/选择的哦。
        if (currentBigPicturePath != null && activeBigPictureTexture != null) {
            return;
        }

        if (currentNode == null || currentNode.getText() == null) return;

        if (textDisplayProgress >= currentNode.getText().length()) {
            if (currentNode.getChoices() != null && !currentNode.getChoices().isEmpty()) {
                int buttonWidth = 180;
                int buttonHeight = 20;
                int buttonMargin = 5;
                int numChoices = currentNode.getChoices().size();

                int buttonX = this.width - buttonWidth - 20;
                int totalButtonBlockHeight = numChoices * buttonHeight + Math.max(0, numChoices - 1) * buttonMargin;
                int topYForAllButtons = this.height - TEXT_WINDOW_HEIGHT - buttonMargin - totalButtonBlockHeight;
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
        if (choice.nextShouldEnd()) {
            onClose();
        } else {
            loadNode(choice.getNext());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonType) {
        if (super.mouseClicked(mouseX, mouseY, buttonType)) {
            return true;
        }

        if (buttonType == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { // 鼠标右键哦~
            showTextWindow = !showTextWindow;
            return true;

        } else if (buttonType == GLFW.GLFW_MOUSE_BUTTON_LEFT) { // 鼠标左键哦~
            return quickProcessText();
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 空格或者回车键，还有Ctrl键都可以哦~
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            return quickProcessText();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean quickProcessText(){
        if (currentBigPicturePath != null && activeBigPictureTexture != null) {
            // 如果正在显示大图图，点一下左键就会到下一个节点啦。
            // 文字慢慢出来和选项处理都会跳过哦。
            proceedToNextNode();
            return true;
        }

        // 下面是普通节点（不是大图图的时候）的逻辑~
        if (currentNode == null || currentNode.getText() == null) {
            Gal.LOGGER.warn("咦？鼠标点点的时候，节点或者文字好像不太对劲耶。窝试试加载 '0' 号起始节点看看好不好...");
            loadNode("0");
            return true;
        }

        if (textDisplayProgress < currentNode.getText().length()) {
            textDisplayProgress = currentNode.getText().length();
            lastUpdateTime = System.currentTimeMillis();
            updateButtons();
        } else {
            if (currentNode.getChoices() == null || currentNode.getChoices().isEmpty()) {
                proceedToNextNode();
            }
        }
        return true;
    }

    private void proceedToNextNode() {
        if (currentNode == null) {
            Gal.LOGGER.warn("想要继续到下一个节点，但是 currentNode 居然是空的！窝试试加载 '0' 号节点好啦...");
            loadNode("0");
            return;
        }
        if (currentNode.nextShouldEnd()) {
            onClose();
        } else {
            loadNode(currentNode.getNext());
        }
    }

    @Override
    public void onClose() {
        if (this.parser != null && this.parser.getSoundPlayer() != null) {
            this.parser.getSoundPlayer().stopAll();
        }

        if (minecraft != null) {
            cachedTextures.values().forEach(texture -> {
                if (texture != null) minecraft.getTextureManager().release(texture);
            });
        }
        cachedTextures.clear();
        this.activeBigPictureTexture = null; // 这个也要记得清掉哦~
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
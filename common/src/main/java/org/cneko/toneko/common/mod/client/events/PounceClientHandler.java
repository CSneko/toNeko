package org.cneko.toneko.common.mod.client.events;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.abilities.PounceClientState;
import org.cneko.toneko.common.mod.abilities.PounceHandler;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.packets.PouncePayload;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

/**
 * 扑跃的客户端部分：按键蓄力、蓄力条、本地预测。
 *
 * <p>按键规则（与需求一致）：
 * <ul>
 *   <li>潜行键 + 疾跑键同时按下 → 开始蓄力；</li>
 *   <li>蓄力期间松开疾跑键不影响，继续累计，直到松开潜行键才释放；</li>
 *   <li>蓄力上限 2 秒（{@link PounceHandler#MAX_CHARGE_TICKS}），不足 0.25 秒视为误触；</li>
 *   <li>离地、受伤、上载具、开界面、入水/滑翔时取消；</li>
 *   <li>猫娘限制由服务端最终裁定，客户端不因此拦蓄力（避免同步延迟导致按了没反应）。</li>
 * </ul>
 *
 * <p>蓄力条直接复用原版马跳跃条的贴图与位置（盖在经验条上）。
 */
@Environment(EnvType.CLIENT)
public final class PounceClientHandler {
    private static final Identifier JUMP_BAR_BACKGROUND = Identifier.withDefaultNamespace("hud/jump_bar_background");
    private static final Identifier JUMP_BAR_PROGRESS = Identifier.withDefaultNamespace("hud/jump_bar_progress");
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    private static boolean charging = false;
    private static int chargeTicks = 0;
    private static int cooldown = 0;
    private static boolean launched = false;
    private static int airborneTicks = 0;
    /** 两键同时按下的瞬间打印一次门槛状态，排查用 */
    private static boolean keysDownLogged = false;

    static {
        LOGGER.info("[pounce] client handler loaded");
    }

    private PounceClientHandler() {
    }

    public static void tick(Minecraft client) {
        Player player = client.player;
        if (player == null) {
            reset();
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        // 死亡/重生：立刻清掉姿势预测，避免复活后还趴着
        if (player.isDeadOrDying()) {
            launched = false;
            airborneTicks = 0;
            cancelCharge();
            PounceClientState.stop();
            return;
        }
        // 落地后结束姿势预测
        if (launched) {
            airborneTicks++;
            if (airborneTicks > 2 && (player.onGround() || player.isInWater())) {
                launched = false;
                airborneTicks = 0;
                PounceClientState.stop();
            }
        }

        // 打开界面时不能蓄力（避免在背包里误触）
        if (client.screen != null) {
            cancelCharge();
            return;
        }

        boolean sneakDown = client.options.keyShift.isDown();
        boolean sprintDown = client.options.keySprint.isDown();

        if (sneakDown && sprintDown && !keysDownLogged) {
            keysDownLogged = true;
            boolean neko = isNeko(player);
            LOGGER.info("[pounce] keys down: onGround={} water={} flying={} passenger={} hurt={} neko={} cooldown={} launched={} charging={}",
                    player.onGround(), player.isInWater(), player.getAbilities().flying, player.isPassenger(),
                    player.hurtTime, neko, cooldown, launched, charging);
            if (!neko) {
                player.sendOverlayMessage(Component.translatable("messages.toneko.pounce.not_neko"));
            }
        } else if ((!sneakDown || !sprintDown) && keysDownLogged) {
            keysDownLogged = false;
        }

        if (charging) {
            // 松开潜行 → 释放（或作废）
            if (!sneakDown) {
                int ticks = chargeTicks;
                cancelCharge();
                if (ticks >= PounceHandler.MIN_CHARGE_TICKS && canLaunch(player)) {
                    LOGGER.info("[pounce] release ticks={} neko={}", ticks, isNeko(player));
                    fire(player, ticks);
                } else {
                    LOGGER.info("[pounce] release discarded ticks={} canLaunch={}", ticks, canLaunch(player));
                }
                return;
            }
            // 蓄力期间必须持续站在地面且状态正常（失去猫娘身份也取消）
            if (!canLaunch(player) || !isNeko(player)) {
                LOGGER.info("[pounce] charge cancelled: onGround={} water={} hurt={} passenger={} flying={}",
                        player.onGround(), player.isInWater(), player.hurtTime,
                        player.isPassenger(), player.getAbilities().flying);
                cancelCharge();
                return;
            }
            if (chargeTicks < PounceHandler.MAX_CHARGE_TICKS) {
                chargeTicks++;
            }
            return;
        }

        // 两个键同时按下才开始蓄力（猫娘限定）
        if (sneakDown && sprintDown && cooldown <= 0 && !launched
                && canLaunch(player) && isNeko(player)) {
            charging = true;
            chargeTicks = 1;
            LOGGER.info("[pounce] charge start (neko={})", isNeko(player));
        }
    }

    private static void fire(Player player, int ticks) {
        float t = ticks / (float) PounceHandler.MAX_CHARGE_TICKS;
        ClientPlayNetworking.send(new PouncePayload(ticks));
        // 本地预测：与服务端同一套冲量公式；先摘掉 onGround 避免地面摩擦吃掉水平速度
        player.setOnGround(false);
        player.setDeltaMovement(PounceHandler.computeImpulse(player, t));
        player.hurtMarked = true;
        player.resetFallDistance();
        PounceClientState.start(player.getUUID());
        launched = true;
        airborneTicks = 0;
        cooldown = PounceHandler.COOLDOWN_TICKS;
    }

    /** 蓄力条：完全复用原版马跳跃条的贴图与位置（经验条位置） */
    public static void renderBar(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!charging || client.options.hideGui || client.player == null) {
            return;
        }
        int x = (graphics.guiWidth() - BAR_WIDTH) / 2;
        int y = graphics.guiHeight() - 29;

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, JUMP_BAR_BACKGROUND, x, y, BAR_WIDTH, BAR_HEIGHT);

        float progress = chargeTicks / (float) PounceHandler.MAX_CHARGE_TICKS;
        int width = Mth.lerpDiscrete(progress, 0, BAR_WIDTH);
        if (width > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, JUMP_BAR_PROGRESS,
                    BAR_WIDTH, BAR_HEIGHT, 0, 0, x, y, width, BAR_HEIGHT);
        }
    }

    public static boolean isCharging() {
        return charging;
    }

    public static boolean isLaunching() {
        return launched;
    }

    private static boolean isNeko(Player player) {
        return player instanceof INeko neko && neko.isNeko();
    }

    private static boolean canLaunch(Player player) {
        return player.isAlive()
                && !player.isDeadOrDying()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.isInWater()
                && !player.isFallFlying()
                && !player.getAbilities().flying
                && player.onGround()
                && player.hurtTime <= 0;
    }

    private static void cancelCharge() {
        charging = false;
        chargeTicks = 0;
    }

    private static void reset() {
        cancelCharge();
        launched = false;
        airborneTicks = 0;
        cooldown = 0;
        keysDownLogged = false;
        PounceClientState.stop();
    }
}

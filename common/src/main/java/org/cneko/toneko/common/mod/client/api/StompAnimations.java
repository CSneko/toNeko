package org.cneko.toneko.common.mod.client.api;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IActualAnimation;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * 客户端「玩味的踩」动画管理：负责为每个玩家注册动画层，并在收到 S2C 包后播放踩踏动画。
 * <p>
 * 动画数据位于 assets/toneko/player_animations/stomp.animation.json。
 */
@Environment(EnvType.CLIENT)
public class StompAnimations {
    /** 动画层在玩家关联数据中的键（不要与其它 mod 冲突）。 */
    public static final ResourceLocation LAYER_ID = toNekoLoc("stomp_layer");
    /** 踩踏循环动画资源键（namespace + 动画 name 字段）。 */
    public static final ResourceLocation ANIM_ID = toNekoLoc("stomp");
    /** 踩踏收回动画资源键（松开按键时播放）。 */
    public static final ResourceLocation RELEASE_ANIM_ID = toNekoLoc("stomp_release");

    private static boolean initialized = false;

    /** 客户端初始化时调用一次：为所有玩家注册踩踏动画层。 */
    public static void init() {
        if (initialized) return;
        initialized = true;
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                LAYER_ID,
                42,
                StompAnimations::createLayer
        );
    }

    private static IAnimation createLayer(AbstractClientPlayer player) {
        return new ModifierLayer<IAnimation>();
    }

    /**
     * 对指定玩家开始播放循环踩踏动画（按住期间持续循环碾的动作）。
     *
     * @param player 踩踏者（执行踩的玩家）
     * @return 是否成功开始播放
     */
    public static boolean play(AbstractClientPlayer player) {
        if (player == null) return false;
        var layer = getLayer(player);
        if (layer == null) return false;
        var anim = PlayerAnimationRegistry.getAnimation(ANIM_ID);
        if (anim == null) return false;
        layer.setAnimation(firstPerson(anim.playAnimation()));
        return true;
    }

    /**
     * 对指定玩家播放收回动画（松开按键时，从踩住姿态回到站立）。
     *
     * @param player 踩踏者
     * @return 是否成功开始播放
     */
    public static boolean playRelease(AbstractClientPlayer player) {
        if (player == null) return false;
        var layer = getLayer(player);
        if (layer == null) return false;
        var anim = PlayerAnimationRegistry.getAnimation(RELEASE_ANIM_ID);
        if (anim == null) return false;
        layer.setAnimation(firstPerson(anim.playAnimation()));
        return true;
    }

    /**
     * 让动画在第一人称下也可见：用第三人称模型渲染第一人称视角（能看到自己的腿踩下去）。
     */
    private static IAnimation firstPerson(IActualAnimation<?> anim) {
        return anim.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
    }

    /** 停止并清除指定玩家当前的踩踏动画。 */
    public static void stop(AbstractClientPlayer player) {
        if (player == null) return;
        var layer = getLayer(player);
        if (layer != null) {
            layer.setAnimation(null);
        }
    }

    /**
     * 判断指定玩家当前是否正在踩踏（踩踏动画处于 active 状态）。
     * 供第一人称渲染 mixin 使用，用于在踩踏期间显示腿与腿部物品。
     */
    public static boolean isStomping(AbstractClientPlayer player) {
        if (player == null) return false;
        var layer = getLayer(player);
        return layer != null && layer.getAnimation() != null && layer.getAnimation().isActive();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> getLayer(AbstractClientPlayer player) {
        var data = PlayerAnimationAccess.getPlayerAssociatedData(player);
        var anim = data.get(LAYER_ID);
        if (anim instanceof ModifierLayer<?> layer) {
            return (ModifierLayer<IAnimation>) layer;
        }
        return null;
    }
}

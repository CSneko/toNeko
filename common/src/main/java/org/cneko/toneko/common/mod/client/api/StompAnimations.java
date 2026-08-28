package org.cneko.toneko.common.mod.client.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * 客户端「玩味的踩」动画管理：负责为每个玩家注册动画层，并在收到 S2C 包后播放踩踏动画。
 * <p>
 * 动画数据位于 assets/toneko/player_animations/stomp.animation.json。
 *
 * <h2>26.x 迁移说明</h2>
 * PlayerAnimator（dev.kosmx.player-anim）目前没有 Minecraft 26.x（去混淆版本）的发布，
 * 因此本类暂时退化为空实现：
 * <ul>
 *     <li>不再注册 PlayerAnimationFactory 动画层；</li>
 *     <li>{@link #play} / {@link #playRelease} 直接返回 false，{@link #isStomping} 恒为 false；</li>
 *     <li>对应的 {@code StompFirstPersonPassMixin} 已移除，
 *         {@code StompFirstPersonRendererMixin} 依赖 {@link #isStomping} 判定，会自动跳过。</li>
 * </ul>
 * 待上游发布 26.x 版本后，从 git 历史（tag V1.9.6 前后）恢复原实现即可。
 */
@Environment(EnvType.CLIENT)
public class StompAnimations {
    /** 动画层在玩家关联数据中的键（不要与其它 mod 冲突）。 */
    public static final Identifier LAYER_ID = toNekoLoc("stomp_layer");
    /** 踩踏循环动画资源键（namespace + 动画 name 字段）。 */
    public static final Identifier ANIM_ID = toNekoLoc("stomp");
    /** 踩踏收回动画资源键（松开按键时播放）。 */
    public static final Identifier RELEASE_ANIM_ID = toNekoLoc("stomp_release");

    /** 客户端初始化时调用一次：为所有玩家注册踩踏动画层。 */
    public static void init() {
        // PlayerAnimator 尚无 26.x 版本，暂不注册动画层。
    }

    /**
     * 对指定玩家开始播放循环踩踏动画（按住期间持续循环碾的动作）。
     *
     * @param player 踩踏者（执行踩的玩家）
     * @return 是否成功开始播放
     */
    public static boolean play(AbstractClientPlayer player) {
        return false;
    }

    /**
     * 对指定玩家播放收回动画（松开按键时，从踩住姿态回到站立）。
     *
     * @param player 踩踏者
     * @return 是否成功开始播放
     */
    public static boolean playRelease(AbstractClientPlayer player) {
        return false;
    }

    /** 停止并清除指定玩家当前的踩踏动画。 */
    public static void stop(AbstractClientPlayer player) {
        // no-op
    }

    /**
     * 判断指定玩家当前是否正在踩踏（踩踏动画处于 active 状态）。
     * 供第一人称渲染 mixin 使用，用于在踩踏期间显示腿与腿部物品。
     */
    public static boolean isStomping(AbstractClientPlayer player) {
        return false;
    }
}

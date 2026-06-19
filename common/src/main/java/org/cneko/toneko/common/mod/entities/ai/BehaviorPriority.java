package org.cneko.toneko.common.mod.entities.ai;

/**
 * 行为优先级分层。
 * 数字越小优先级越高，用于 NekoBrain 的导航仲裁。
 */
public enum BehaviorPriority {
    /** 逃跑、yandere防御 —— 生存本能，不可打断 */
    CRITICAL(0),
    /** 攻击、仇恨追击 —— 战斗状态 */
    COMBAT(1),
    /** 跟随主人、繁殖 —— 高优先级社交 */
    HIGH(2),
    /** 治疗、睡觉、采集、拾取、活泼、自保、陪伴 —— 日常行为 */
    NORMAL(3),
    /** 漫游、中二、晒太阳、夜行 —— 低优先级自主行为 */
    LOW(4),
    /** 围观玩家、幽灵飞行 —— 最低优先级，随时可被打断 */
    IDLE(5);

    private final int rank;

    BehaviorPriority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    /** 返回 true 表示 requested 的优先级高于 current（数字更小） */
    public static boolean isHigherThan(BehaviorPriority requested, BehaviorPriority current) {
        if (requested == null) return false;
        if (current == null) return true;
        return requested.rank < current.rank;
    }
}

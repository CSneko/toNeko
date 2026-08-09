package org.cneko.toneko.common.mod.ai.actions;

/**
 * AI 输出的一条动作指令。
 * @param type   动作类型：move_to_player / give_item / follow / write_diary / mate 等
 * @param item   物品 id（give_item 使用，如 "minecraft:apple"）
 * @param count  物品数量（give_item 使用，默认 1）
 * @param target 目标（玩家名或实体名，可选；缺省为正在说话的人）
 * @param text   附加文本（write_diary 使用：日记正文）
 */
public record NekoAction(String type, String item, int count, String target, String text) {
}

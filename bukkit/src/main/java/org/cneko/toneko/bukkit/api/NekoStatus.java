package org.cneko.toneko.bukkit.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.util.LanguageUtil;

import java.util.ArrayList;
import java.util.List;

public class NekoStatus {
    public static void setToNeko(Player player){
        NekoQuery.setNeko(player.getUniqueId(), true);
        addPrefix(player);
    }

    public static void setToNotNeko(Player player) {
        NekoQuery.setNeko(player.getUniqueId(), false);
        removePrefix(player);
    }

    public static void addPrefix(Player player){
        /*
         * 这个地方呢，是为了兼容Folia而做的，目前Folia的Scoreboard API是损坏的，所以不能用，只能返回默认的东西
         * 好吧，期待Folia早日修复这个API喵
         */
        try {
            // 获取Scoreboard管理器
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            Scoreboard scoreboard = manager.getMainScoreboard(); // 使用主Scoreboard
            String teamName = "neko";

            // 检查队伍是否存在，不存在则创建
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName); // 创建新队伍
            }

            // 设置队伍前缀（头衔）
            team.setPrefix(LanguageUtil.prefix);

            // 将玩家添加到队伍
            team.addEntry(player.getName());

            // 为玩家设置Scoreboard
            player.setScoreboard(scoreboard);
        } catch (Exception ignore){
        }
    }

    public static void removePrefix(Player player){
        try {

            // 获取Scoreboard管理器
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            Scoreboard scoreboard = manager.getMainScoreboard();
            String teamName = "neko";

            // 获取队伍并从队伍中移除玩家
            Team team = scoreboard.getTeam(teamName);
            if (team != null && team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }catch (Exception ignore){}
    }

    public static List<String> getPlayerPrefixes(Player player) {
        try {

            List<String> prefixes = new ArrayList<>();

            // 获取Scoreboard管理器
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            Scoreboard scoreboard = manager.getMainScoreboard();

            // 遍历所有队伍，检查玩家是否在队伍中
            for (Team team : scoreboard.getTeams()) {
                if (team.hasEntry(player.getName())) {
                    // 获取前缀并添加到列表中
                    prefixes.add(team.getPrefix());
                }
            }

            return prefixes;
        } catch (Exception ignore) {
            if (NekoQuery.isNeko(player.getUniqueId())){
                return List.of(LanguageUtil.prefix);
            }
        }
        return List.of();
    }
}

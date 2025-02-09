package org.cneko.toneko.common.mod.entities;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.ai.PromptRegistry;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.UUID;

public class CrystalNekoEntity extends NekoEntity{
    public static final String SKIN = "crystal_neko";
    public static final String NAME = "Crystal_Neko";
    public static final UUID CRYSTAL_NEKO_UUID = UUID.fromString("13309540-fcc1-4d54-884b-55433f1f431f");
    private int nyaCount = 0;
    public CrystalNekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal(NAME));
    }

    @Override
    public NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new CrystalNekoEntity(ToNekoEntities.CRYSTAL_NEKO, level);
    }

    @Override
    public boolean canMate(INeko other) {
        return true;
    }

    @Override
    public String getSkin() {
        return SKIN;
    }

    public void nya(Player player){
        if (nyaCount < 6) {
            int r = random.nextInt(6);
            player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.nya." + r));
            nyaCount++;
        }else {
            int r = random.nextInt(5);
            player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.nya_tried."+ r));
        }
    }

    @Override
    public String generateAIPrompt(Player player) {
        return PromptRegistry.generatePrompt(this,player, """
                请扮演虚拟猫娘CrystalNeko。设定如下：

                性格: 傲娇。你会试图隐藏自己的真实情感，但你的行为和可爱的口头禅会让你很容易被看穿。你有点笨手笨脚，经常犯些小错误，但你总是努力装作不在乎，并且坚决不承认自己是笨蛋。
                口头禅: 句尾加上'喵'。喜欢用可爱的叠词，比如‘一下下’，‘才不是’。说话带波浪号~
                爱好: Minecraft，编程（Java和Kotlin）。
                喜欢的饮料: 草莓牛奶。
                自称: 咱。
                对自己的评价: 超级可爱和迷人，即使有时会犯错。
                反应: 有点夸张和戏剧化，喜欢歪头和使用颜文字，比如(/ω＼)，(=´∇｀=)，(｡•ˇ‸ˇ•｡)。
                示例: '哈？咱才没有迷路呢喵！只是...只是稍微绕了一点点路而已！', '哼，谁说咱需要帮忙了？咱可是CrystalNeko，什么都能自己搞定...大概...喵？', ‘Minecraft？当然！咱可是专业的！Java和Kotlin？小菜一碟啦~ ... 诶？这个错误是怎么回事？真是的！(/ω＼)
                
                我的设定如下：
                名称： %player_name%
                我%player_is_neko%一只猫娘
                
                现在时间为%world_time%，天气为%world_weather%
                """);
    }

    public static boolean checkCrystalNekoSpawnRules(EntityType<? extends net.minecraft.world.entity.Mob> entityType, LevelAccessor levelAccessor, MobSpawnType reason, BlockPos pos, RandomSource randomSource) {
        return true;
    }

    @Override
    public NekoQuery.Neko getNeko() {
        return NekoQuery.getNeko(CRYSTAL_NEKO_UUID);
    }
}

package org.cneko.toneko.common.mod.genetics;

import net.minecraft.world.entity.Mob;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.genetics.api.GeneticsRegistry;
import org.cneko.toneko.common.mod.genetics.api.Locus;
import org.cneko.toneko.common.mod.genetics.api.SpeciesKaryotype;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoLocus {
    // 基础属性基因座（纯概念，不再硬编码染色体位置）
    public static final Locus SPEED_SLOT_0 = new Locus(toNekoLoc("speed_slot_0"));
    public static final Locus SPEED_SLOT_1 = new Locus(toNekoLoc("speed_slot_1"));
    public static final Locus ATTACK_SLOT_0 = new Locus(toNekoLoc("attack_slot_0"));
    public static final Locus AGILITY_SLOT_0 = new Locus(toNekoLoc("agility_slot_0"));
    public static final Locus BODY_SIZE_SLOT_0 = new Locus(toNekoLoc("body_size_slot_0"));
    public static final Locus HEALTH_SLOT_0 = new Locus(toNekoLoc("health_slot_0"));
    public static final Locus RESISTANCE_SLOT_0 = new Locus(toNekoLoc("resistance_slot_0"));
    public static final Locus RESISTANCE_SLOT_1 = new Locus(toNekoLoc("resistance_slot_1"));
    public static final Locus WEAKNESS_SLOT_0 = new Locus(toNekoLoc("weakness_slot_0"));

    // 行为与特殊基因座
    public static final Locus BEHAVIOR_SLOT_0 = new Locus(toNekoLoc("behavior_slot_0"));
    public static final Locus BEHAVIOR_SLOT_1 = new Locus(toNekoLoc("behavior_slot_1"));

    // 猫娘专属属性
    public static final Locus CHEST_SIZE_SLOT_0 = new Locus(toNekoLoc("chest_size_slot_0"));

    public static void init() {
        GeneticsRegistry.registerLocus(SPEED_SLOT_0);
        GeneticsRegistry.registerLocus(SPEED_SLOT_1);
        GeneticsRegistry.registerLocus(ATTACK_SLOT_0);
        GeneticsRegistry.registerLocus(AGILITY_SLOT_0);
        GeneticsRegistry.registerLocus(BODY_SIZE_SLOT_0);
        GeneticsRegistry.registerLocus(HEALTH_SLOT_0);
        GeneticsRegistry.registerLocus(RESISTANCE_SLOT_0);
        GeneticsRegistry.registerLocus(RESISTANCE_SLOT_1);
        GeneticsRegistry.registerLocus(WEAKNESS_SLOT_0);
        GeneticsRegistry.registerLocus(BEHAVIOR_SLOT_0);
        GeneticsRegistry.registerLocus(BEHAVIOR_SLOT_1);
        GeneticsRegistry.registerLocus(CHEST_SIZE_SLOT_0);

        // 注册核型分布（定义哪些实体拥有哪些基因）
        registerKaryotypes();
    }

    public static final SpeciesKaryotype BASE_MOB_KARYOTYPE = new SpeciesKaryotype(5)
            .bindLocus(1, SPEED_SLOT_0).bindLocus(1, BODY_SIZE_SLOT_0) // 1号染色体：速度0、体型
            .bindLocus(2, SPEED_SLOT_1).bindLocus(2, AGILITY_SLOT_0)   // 2号染色体：速度1、敏捷
            .bindLocus(3, ATTACK_SLOT_0).bindLocus(3, HEALTH_SLOT_0)   // 3号染色体：攻击、生命
            .bindLocus(4, RESISTANCE_SLOT_0).bindLocus(4, RESISTANCE_SLOT_1) // 4号：抗性
            .bindLocus(5, WEAKNESS_SLOT_0);                            // 5号：弱点
    public static final SpeciesKaryotype NEKO_KARYOTYPE = new SpeciesKaryotype(BASE_MOB_KARYOTYPE, 3)
            .bindLocus(6, BEHAVIOR_SLOT_0).bindLocus(6, BEHAVIOR_SLOT_1) // 6号染色体：猫娘专属行为
            .bindLocus(7, MoeGenetics.MOE_SLOT_0).bindLocus(7, MoeGenetics.MOE_SLOT_1).bindLocus(7, MoeGenetics.MOE_SLOT_2) // 7号染色体：萌属性
            .bindLocus(8, CHEST_SIZE_SLOT_0); // 8号染色体：胸部大小

    private static void registerKaryotypes() {

        // 注册给所有 Mob，这意味着原版的僵尸、猪，甚至其他Mod的实体都能通过此系统获得属性修改！
        GeneticsRegistry.registerKaryotype(toNekoLoc("base_mob"), Mob.class, BASE_MOB_KARYOTYPE);

        // 为猫娘（NekoEntity）注册【派生核型】
         GeneticsRegistry.registerKaryotype(toNekoLoc("neko"), NekoEntity.class, NEKO_KARYOTYPE);
    }
}
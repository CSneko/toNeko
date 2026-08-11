package org.cneko.toneko.common.mod.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.ai.goal.GhostFollowOwnerGoal;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoFlyingAroundGoal;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class GhostNekoEntity extends NekoEntity{
    public static final List<String> nekoSkins = List.of("ninjia");
    /** 生前猫娘类型的翻译键（如 entity.toneko.adventurer_neko），NBT 持久化；
     *  名字/主人/好感度等数据幽灵仍然保留，只有类型变为幽灵，故需记录生前类型 */
    private String pastTypeName;
    private static final String PAST_TYPE_NAME_TAG = "PastTypeName";
    public GhostNekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.moveControl = new FlyingMoveControl(this,  20, true);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (pastTypeName != null) {
            compound.putString(PAST_TYPE_NAME_TAG, pastTypeName);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(PAST_TYPE_NAME_TAG)) {
            this.pastTypeName = compound.getString(PAST_TYPE_NAME_TAG);
        }
    }

    /** 生前猫娘类型的翻译键（null = 未记录，如野生/旧存档幽灵） */
    public @Nullable String getPastTypeName() {
        return pastTypeName;
    }

    public void setPastTypeName(String pastTypeName) {
        this.pastTypeName = pastTypeName;
    }

    @Override
    public void randomize() {
        super.randomize();
        EntityUtil.randomizeAttributeValue(this, Attributes.FLYING_SPEED,0.4,0.15,0.3); // 实体的飞行速度为0.15~0.3间
    }

    @Override
    public @Nullable GhostNekoEntity getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new GhostNekoEntity(ToNekoEntities.GHOST_NEKO,this.level());
    }

    @Override
    public void registerGoals() {
        super.registerGoals();
        // 父类跟随 goal 退休：它的 owner 会被互动菜单"跟随"按钮设置并提交移动意图，
        // 与幽灵专属跟随（GhostFollowOwnerGoal）冲突；移除后菜单"跟随"变无害 no-op
        this.goalSelector.removeGoal(this.nekoFollowOwnerGoal);
        // 专属跟随优先级最高（0）；乱飞降级为无主人时的"原地飘动等待"
        this.goalSelector.addGoal(0, new GhostFollowOwnerGoal(this));
        this.goalSelector.addGoal(1, new NekoFlyingAroundGoal(this));
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.getMainHandItem().is(Items.LEAD)) {
            return super.hurt(source, amount);
        }
        // 除非命令或魔法，否则不造成伤害
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.MAGIC)){
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public float getFlyingSpeed() {
        return (float) this.getAttributeValue(Attributes.FLYING_SPEED);
    }

    @Override
    public String getDefaultSkin() {
        return "ninjia";
    }

    public static AttributeSupplier.Builder createGhostNekoAttributes(){
        return NekoEntity.createNekoAttributes().add(Attributes.FLYING_SPEED);
    }

    /**
     * 把一只濒死的猫娘化作幽灵：NBT 快照复制全部灵魂数据
     * （主人/好感度/AI 聊天记忆/名字/萌属性/基因/能量/等级因子/日记等），
     * 原实体照常走完死亡掉落流程，幽灵从尸体上方 y+0.5 处飘出。
     * 背包/装备因掉落已清空，不会复制。仅在服务端有效，失败返回 null。
     */
    public static @Nullable GhostNekoEntity createGhostFrom(NekoEntity dying) {
        if (dying.level() instanceof ServerLevel serverLevel) {
            CompoundTag tag = new CompoundTag();
            // saveWithoutId 而非 addAdditionalSaveData：CustomName/UUID/Pos 等基础数据
            // 写在 saveWithoutId 里，addAdditionalSaveData 只有附加数据（会漏掉名字）
            dying.saveWithoutId(tag);
            // 调试：定位名字继承问题
            LOGGER.info("[GHOST] dying name={} customName={} | tag has CustomName={} value={}",
                    dying.getName().getString(), dying.getCustomName(),
                    tag.contains("CustomName"),
                    tag.contains("CustomName") ? tag.getString("CustomName") : "<none>");
            // 新实体不能继承旧 UUID
            tag.remove("UUID");
            // 防御性清理：实体类型 id 不参与加载，移除避免意外
            tag.remove("id");
            // 防御性清理：死亡瞬间乘客/坐骑/栓绳/速度可能仍在快照里
            tag.remove("Passengers");
            tag.remove("Vehicle");
            tag.remove("Leash");
            tag.remove("Motion");

            GhostNekoEntity ghost = ToNekoEntities.GHOST_NEKO.create(serverLevel);
            ghost.readAdditionalSaveData(tag);   // 末尾自动 expressTraits()，基因与生前一致
            // 兜底：显式复制名字（防御 NBT 读取链的意外，保证名字一定继承）
            if (dying.getCustomName() != null) {
                ghost.setCustomName(dying.getCustomName());
            }
            LOGGER.info("[GHOST] ghost customName={} name={}", ghost.getCustomName(), ghost.getName().getString());
            ghost.setPastTypeName(dying.getType().getDescriptionId()); // 记录生前类型（幽灵读 type 只会得到"幽灵猫娘"）
            ghost.setHealth(ghost.getMaxHealth()); // 原 Health ≤ 0，恢复满血（属性修饰符已继承）
            ghost.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); // 清掉死亡时残留的速度
            ghost.setPersistenceRequired();        // 即使无 AI 也不被自然刷掉
            ghost.moveTo(dying.getX(), dying.getY() + 0.5, dying.getZ(),
                    dying.getYRot(), dying.getXRot());
            return ghost;
        }
        return null;
    }
}

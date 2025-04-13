package org.cneko.toneko.common.mod.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.AmmunitionEntity;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BazookaItem extends Item {
    public static final String ID = "bazooka";
    private static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("empty");
    public BazookaItem(Properties properties) {
        super(properties);
    }

    public boolean isAmmunitionLegal(Item item){
        return item instanceof BazookaItem.Ammunition;
    }

    @Nullable
    public Ammunition getAmmunition(ItemStack stack){
        // 从注册表获取弹药
        ResourceLocation res = stack.getOrDefault(ToNekoComponents.ITEM_ID_COMPONENT, EMPTY);
        Item item = BuiltInRegistries.ITEM.get(res);
        if(item instanceof BazookaItem.Ammunition am){
            return am;
        }else {
            return null;
        }
    }

    public ItemStack foundAmmunitionInventory(Player shooter,Ammunition am){
        for(ItemStack stack : shooter.getInventory().items){
            if(stack.getItem() == am){
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private @Nullable Ammunition findFirstAmmoType(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof Ammunition ammo) {
                // 自动装填找到的弹药类型
                setAmmunitionType(stack.getItem(), player.getUseItem());
                return ammo;
            }
        }
        return null;
    }

    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack bazookaStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // 获取弹药类型
            Ammunition ammoType = getAmmunition(bazookaStack);
            if (ammoType == null) {
                // 尝试自动装填弹药
                ammoType = findFirstAmmoType(player);
            }

            if (ammoType != null) {
                // 获取弹药堆栈
                ItemStack ammoStack = foundAmmunitionInventory(player, ammoType);
                if (!ammoStack.isEmpty()) {
                    fire(player, bazookaStack, ammoStack);
                    // 设置冷却
                    player.getCooldowns().addCooldown(this, ammoType.getCooldownTicks(bazookaStack, ammoStack));
                }
            }
        }
        return InteractionResultHolder.success(bazookaStack);
    }

    // 设置当前装填的弹药类型
    public void setAmmunitionType(Item ammoItem, ItemStack bazookaStack) {
        if (ammoItem instanceof Ammunition) {
            ResourceLocation ammoId = BuiltInRegistries.ITEM.getKey(ammoItem);
            bazookaStack.set(ToNekoComponents.ITEM_ID_COMPONENT, ammoId);
        }
    }

    public InteractionResultHolder<ItemStack> reload(Player player, ItemStack bazooka, ItemStack ammoStack) {
        if (ammoStack.getItem() instanceof Ammunition) {
            // 设置弹药类型
            setAmmunitionType(ammoStack.getItem(), bazooka);
            // 消耗弹药（非创造模式）
            if (!player.isCreative()) {
                ammoStack.shrink(1);
            }
            return InteractionResultHolder.success(bazooka);
        }
        return InteractionResultHolder.pass(bazooka);
    }
    public void fire(LivingEntity shooter, ItemStack bazookaStack, ItemStack ammunition) {
        if (ammunition.isEmpty()) return;

        Item ammoItem = ammunition.getItem();
        if (ammoItem instanceof Ammunition ammo) {
            // 只在服务端执行
            if (!shooter.level().isClientSide()) {
                // 消耗弹药
                if (shooter instanceof Player player && !player.isCreative()) {
                    ammunition.shrink(1);
                }

                // 创建弹药物体
                AmmunitionEntity projectile = new AmmunitionEntity(ToNekoEntities.AMMUNITION_ENTITY, shooter.level());
                projectile.setBazookaStack(bazookaStack);
                projectile.setAmmunitionStack(ammunition);
                projectile.setOwner(shooter);

                // 设置位置和方向
                Vec3 spawnPos = shooter.position()
                        .add(shooter.getLookAngle().scale(0.5))
                        .add(0, shooter.getEyeHeight(), 0);
                projectile.setPos(spawnPos);
                float inaccuracy = 1.0f - shooter.getXRot() / 90.0f; // 根据俯仰角计算散布

                Vec3 lookAngle = shooter.getLookAngle().normalize();
                float speed = ammo.getSpeed(bazookaStack, ammunition) * 1.5f;

                // 直接传递方向向量和速度标量
                projectile.shootWithInitialPos(
                        lookAngle.x,
                        lookAngle.y,
                        lookAngle.z,
                        speed,      // 速度标量
                        inaccuracy  // 散布
                );
                // 添加到世界
                shooter.level().addFreshEntity(projectile);
            }
        }
    }

    public interface Ammunition{
        void hitOnEntity(LivingEntity shooter, LivingEntity target, ItemStack bazooka, ItemStack ammunition);
        void hitOnBlock(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition);
        void hitOnAir(LivingEntity shooter, BlockPos pos,ItemStack bazooka, ItemStack ammunition);
        float getSpeed(ItemStack bazooka, ItemStack ammunition);
        float getMaxDistance(ItemStack bazooka, ItemStack ammunition);
        int getCooldownTicks(ItemStack bazooka, ItemStack ammunition);
    }
}

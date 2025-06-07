package org.cneko.toneko.common.mod.items;

import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.AmmunitionEntity;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.cneko.toneko.common.mod.misc.ToNekoSoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BazookaItem extends Item {
    public static final String ID = "bazooka";
    private static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("empty");

    public BazookaItem(Properties properties) {
        super(properties);
    }



    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltips, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltips, tooltipFlag);
        Ammunition ammo = getAmmunition(stack);
        if (ammo instanceof Item ammoItem) {
            // 弹药类型显示
            tooltips.add(Component.translatable("item.toneko.bazooka.tip.ammo_type")
                    .append(" ")
                    .append(ammoItem.getDescription())
            );
        } else {
            tooltips.add(Component.translatable("item.toneko.bazooka.tip.no_ammo"));
        }
        tooltips.add(Component.translatable("item.toneko.bazooka.tip.reload"));
    }

    public boolean isAmmunitionLegal(Item item) {
        return item instanceof Ammunition;
    }

    @Nullable
    public Ammunition getAmmunition(ItemStack stack) {
        ResourceLocation res = stack.getOrDefault(ToNekoComponents.ITEM_ID_COMPONENT, EMPTY);
        Item item = BuiltInRegistries.ITEM.get(res);
        return item instanceof Ammunition am ? am : null;
    }

    public ItemStack foundAmmunitionInventory(Player shooter, Ammunition am) {
        for (ItemStack stack : shooter.getInventory().items) {
            if (stack.getItem() == am) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private @Nullable Ammunition findNextAmmoType(Player player, ItemStack bazookaStack) {
        List<Ammunition> availableAmmo = new ArrayList<>();
        // 收集所有可用的弹药类型
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof Ammunition ammo && !availableAmmo.contains(ammo)) {
                availableAmmo.add(ammo);
            }
        }
        if (availableAmmo.isEmpty()) return null;

        // 获取当前弹药索引
        Ammunition current = getAmmunition(bazookaStack);
        int currentIndex = current != null ? availableAmmo.indexOf(current) : -1;
        int nextIndex = (currentIndex + 1) % availableAmmo.size();
        return availableAmmo.get(nextIndex);
    }

    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack bazookaStack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // 换弹操作
            if (!level.isClientSide) {
                Ammunition newAmmo = findNextAmmoType(player, bazookaStack);
                if (newAmmo != null) {
                    setAmmunitionType((Item) newAmmo, bazookaStack);
                    player.displayClientMessage(Component.translatable("item.toneko.bazooka.reloaded")
                            .append(" ")
                            .append(((Item) newAmmo).getDescription()), true);
                }
            }
        } else {
            // 发射逻辑
            if (!level.isClientSide) {
                Ammunition ammoType = getAmmunition(bazookaStack);
                if (ammoType == null) {
                    player.displayClientMessage(Component.translatable("item.toneko.bazooka.no_ammo_selected"), true);
                    return InteractionResultHolder.fail(bazookaStack);
                }

                ItemStack ammoStack = foundAmmunitionInventory(player, ammoType);
                if (ammoStack.isEmpty()) {
                    player.displayClientMessage(Component.translatable("item.toneko.bazooka.out_of_ammo"), true);
                    return InteractionResultHolder.fail(bazookaStack);
                }

                fire(player, bazookaStack, ammoStack);
                player.getCooldowns().addCooldown(this, ammoType.getCooldownTicks(bazookaStack, ammoStack));

            }
            // 播放音效
            player.level().playSound(
                    player,
                    player.blockPosition(),
                    ToNekoSoundEvents.BAZOOKA_BIU,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f
            );
        }
        return InteractionResultHolder.success(bazookaStack);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 0;
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
                if ((shooter instanceof Player player && !player.isCreative()) ||  !(shooter instanceof Player)) {
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


    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, EnchantingContext context) {
        if (enchantment.is(Enchantments.LOYALTY)){
            return true;
        }
        return super.canBeEnchantedWith(stack, enchantment, context);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true; // 允许被附魔
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    public interface Ammunition {
        void hitOnEntity(LivingEntity shooter, LivingEntity target, ItemStack bazooka, ItemStack ammunition);
        void hitOnBlock(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition);
        void hitOnAir(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition);
        default float getSpeed(ItemStack bazooka, ItemStack ammunition){
            return 1.0f;
        }
        default float getMaxDistance(ItemStack bazooka, ItemStack ammunition){
            return 32;
        }
        default int getCooldownTicks(ItemStack bazooka, ItemStack ammunition){
             return 20;
        }
        default boolean isHarmful(ItemStack bazooka, ItemStack ammunition){
            return false;
        }
    }
}
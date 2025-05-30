package org.cneko.toneko.common.mod.entities;

import lombok.Getter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;
import org.cneko.toneko.common.mod.ai.PromptRegistry;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoPickupItemGoal;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;
import org.cneko.toneko.common.mod.packets.interactives.NekoEntityInteractivePayload;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoFollowOwnerGoal;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoMateGoal;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;
import static org.cneko.toneko.common.mod.util.TextUtil.randomTranslatabledComponent;
import static org.cneko.toneko.common.Bootstrap.LOGGER;

public abstract class NekoEntity extends AgeableMob implements GeoEntity, INeko {
    public static final TagKey<Item> NEKO_ARMOR = TagKey.create(Registries.ITEM,toNekoLoc("neko/armor"));
    public static double DEFAULT_FIND_RANGE = 16.0D;
    public static float DEFAULT_RIDE_RANGE = 3f;
    public static final List<String> MOE_TAGS = List.of(
            "tsundere", // 傲娇
            "baka", // 笨蛋
            "mesugaki", // 雌小鬼
            "yowaki", // 弱气
            "dojikko", // 冒失
            "yandere", // 病娇
            "tennen_boke", // 天然呆
            "haraguro", // 腹黑
            "gentleness", // 温柔
            "shoakuma", // 小恶魔
            "chunibyo", // 中二病
            "shizukana", // 文静
            "narenareshi", // 自来熟
            "paranoia", // 偏执
            "yuri" // 百合
    );

    public NekoFollowOwnerGoal nekoFollowOwnerGoal;
    public NekoMateGoal nekoMateGoal;
    private final AnimatableInstanceCache cache;
    @Getter
    private boolean isSitting = false;
    @Getter
    final NekoInventory inventory = new NekoInventory(this);
    private short slowTimer = 20;

    public static final EntityDataAccessor<String> SKIN_DATA_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> MOE_TAGS_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> GATHERING_POWER_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> NEKO_ENERGY_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.FLOAT);

    public NekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        if (!this.level().isClientSide()){
            NekoQuery.Neko neko = this.getNeko();
            neko.setNeko(true);
            randomize();
        }
        this.cache = GeckoLibUtil.createInstanceCache(this);
    }


    public void randomize(){
        NekoQuery.Neko neko = this.getNeko();
        // 设置名字（如果没有）
        if (!this.hasCustomName()) {
            this.setCustomName(Component.literal(NekoNameRegistry.getRandomName()));
        }

        EntityUtil.randomizeAttributeValue(this, Attributes.SCALE,1,0.65,1.05); // 实体的体型为0.65~1.05间
        EntityUtil.randomizeAttributeValue(this, Attributes.MOVEMENT_SPEED,0.7,0.5,0.6); // 实体速度为0.5~0.6间


        // 初始化萌属性喵
        if (!neko.hasAnyMoeTags()){
            // 随机设置2~3个萌属性喵
            List<String> tags = new ArrayList<>();
            for (int i = 0; i < Mth.nextInt(this.random, 2, 3); i++) {
                tags.add(MOE_TAGS.get(Mth.nextInt(this.random, 0, MOE_TAGS.size() - 1)));
            }
            this.setMoeTags(tags);
        }else {
            this.setMoeTags(getMoeTags());
        }

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_DATA_ID, this.getDefaultSkin());
        builder.define(MOE_TAGS_ID, "");
        builder.define(GATHERING_POWER_ID, 0);
        builder.define(NEKO_ENERGY_ID, 0f);
    }



    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("Skin", this.getSkin());
        compound.put("Inventory", this.inventory.save(new ListTag()));
        compound.putInt("SelectedItemSlot", this.inventory.selected);
        compound.putInt("GatheringPower", this.getGatheringPower());
        this.saveNekoNBTData(compound);
    }

    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setSkin(compound.getString("Skin"));
        ListTag listTag = compound.getList("Inventory", 10);
        this.inventory.load(listTag);
        this.inventory.selected = compound.getInt("SelectedItemSlot");
        this.setGatheringPower(compound.getInt("GatheringPower"));
        this.loadNekoNBTData(compound);
    }

    @Override
    public void registerGoals() {
        super.registerGoals();
        // 猫娘会观察玩家
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // 猫娘会近战攻击
        this.goalSelector.addGoal(10, new MeleeAttackGoal(this, 1.0D, false));
        // 猫娘需要呼吸才能活呀
        this.goalSelector.addGoal(5, new BreathAirGoal(this));
        // 猫娘会闲逛
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.3, 1));
        // 猫娘会跟主人
        nekoFollowOwnerGoal = new NekoFollowOwnerGoal(this,null,30,Math.min(0.1,this.followLeashSpeed() / 1.5));
        this.goalSelector.addGoal(4,nekoFollowOwnerGoal);
        // 猫娘有繁殖欲望
        nekoMateGoal = new NekoMateGoal(this,null,30,this.followLeashSpeed() / 2);
        this.goalSelector.addGoal(3,nekoMateGoal);
        // 会尝试捡起附近的物品
        this.goalSelector.addGoal(5, new NekoPickupItemGoal(this));
        // 会被拿着喜欢物品的玩家吸引
        this.goalSelector.addGoal(5, new TemptGoal(this, 0.5D, this::isFavoriteItem,false));
    }

    public NekoQuery.Neko getNeko() {
        if (this.isAlive()) {
            return NekoQuery.getNeko(this.getUUID());
        }else {
            // 返回默认值
            return NekoQuery.NekoData.getNeko(NekoQuery.NekoData.EMPTY_UUID);
        }
    }

    public void followOwner(Player followingOwner,double maxDistance, double followSpeed) {
        if (nekoFollowOwnerGoal !=null) {
            nekoFollowOwnerGoal.setTarget(followingOwner);
            nekoFollowOwnerGoal.setMaxDistance(maxDistance);
            nekoFollowOwnerGoal.setFollowSpeed(followSpeed);
            nekoFollowOwnerGoal.start();
        }

    }
    public void followOwner(Player followingOwner) {
        followOwner(followingOwner, this.getAttributeValue(Attributes.FOLLOW_RANGE),this.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }
    public @Nullable NekoFollowOwnerGoal getFollowingOwner() {
        return this.nekoFollowOwnerGoal;
    }

    public GoalSelector getGoalSelector() {
        return this.goalSelector;
    }

    public String getSkin(){
        if (this.entityData.get(SKIN_DATA_ID).isEmpty()){
            this.setSkin(getRandomSkin());
        }
        return this.entityData.get(SKIN_DATA_ID);
    }
    public void setSkin(String skin) {
        //noinspection ConstantValue
        if (this.entityData != null) {
            this.entityData.set(SKIN_DATA_ID, skin);
        }
    }
    public String getRandomSkin(){
        return NekoSkinRegistry.getRandomSkin(this.getType());
    }
    public String getDefaultSkin(){
        return "aquarter";
    }

    public List<String> getMoeTags(){
        // 服务器可以直接从猫娘数据文件中读取，客户端必须从服务器获取
        if (this.level().isClientSide){
            String moeTagsString = this.entityData.get(MOE_TAGS_ID);
            return moeTagsString.isEmpty() ? List.of() : List.of(moeTagsString.split(":"));
        } else {
            return this.getNeko().getMoeTags();
        }
    }

    // 翻译后的String的萌属性
    public String getMoeTagsString(){
        List<Component> tags = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        this.getMoeTags().forEach(moeTag -> tags.add(Component.translatable("moe.toneko."+moeTag)));
        for (Component tag : tags) {
            result.append(tag.getString()).append(",");
        }
        if (!result.isEmpty()){
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }
    public void setMoeTags(List<String> moeTags){
        // 服务器需要更新猫娘数据，客户端不需要
        if (!this.level().isClientSide){
            // 同时更新猫娘数据
            this.getNeko().setMoeTags(moeTags);
        }
        // 更新数据
        this.entityData.set(MOE_TAGS_ID, String.join(":", moeTags));
    }

    public float getNekoEnergy() {
        return this.entityData.get(NEKO_ENERGY_ID);
    }

    @Override
    public void setNekoEnergy(float energy) {
        this.entityData.set(NEKO_ENERGY_ID, Mth.clamp(energy, 0,this.getMaxNekoEnergy()));
    }

    // 获取采集动力
    public int getGatheringPower() {
        return this.entityData.get(GATHERING_POWER_ID);
    }

    // 设置采集动力
    public void setGatheringPower(int power) {
        this.entityData.set(GATHERING_POWER_ID, Mth.clamp(power, 0, Integer.MAX_VALUE));
    }

    // 增大
    public void addGatheringPower(int power) {
        this.setGatheringPower(this.getGatheringPower() + power);
    }

    public void consumeGatheringPower(int amount) {
        this.setGatheringPower(Math.max(0, this.getGatheringPower() - amount));
    }


    // 最喜欢的物品
    public boolean isFavoriteItem(ItemStack stack){
        return stack.is(ToNekoItems.CATNIP_TAG);
    }
    // 是否喜欢这个物品
    public boolean isLikedItem(ItemStack stack){
        return isFavoriteItem(stack) || stack.has(DataComponents.FOOD) || stack.is(NEKO_ARMOR);
    }
    // 是否需要这个物品
    public boolean isNeededItem(ItemStack stack){
        return isLikedItem(stack);
    }

    public boolean giftItem(Player player, int slot){
        return giftItem(player, player.getInventory().getItem(slot));
    }
    // 赠送物品
    public boolean giftItem(Player player, ItemStack stack){
        // 设置持久性
        this.setPersistenceRequired();
        // 如果是喜欢的物品
        if (this.isLikedItem(stack) && player instanceof ServerPlayer sp){
            // 增长动力
            this.addGatheringPower(20);
            if (this.equipArmors(stack)){
                // 装备
                return true;
            }
            // 达成进度
            ToNekoCriteria.GIFT_NEKO.trigger(sp);
            // 如果是食物，则吃掉并回血并获取对应的效果
            this.eat(this.level(), stack);
            if (this.getInventory().canAdd()) {
                ItemStack s = stack.copy();
                s.setCount(1);
                this.addItem(s);
            }else {
                player.sendSystemMessage(randomTranslatabledComponent("message.toneko.neko.gift_full",2, Objects.requireNonNull(this.getCustomName()).getString()));
                return false;
            }
            // 播放爱心粒子
            this.level().addParticle(ParticleTypes.HEART,this.getX()+1.8, this.getY(), this.getZ(),1,1,1);
            // 发送给客户端
            ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.HEART, true, this.getX() + 1.8, this.getY(), this.getZ(), 2, 2, 2, 1, 1);
            sp.connection.send(packet);
            // 随机发送感谢消息
            player.sendSystemMessage(randomTranslatabledComponent("message.toneko.neko.gift_success",3, Objects.requireNonNull(this.getCustomName()).getString()));

            // 设置玩家为主人
            if (!this.getNeko().hasOwner(player.getUUID())){
                this.getNeko().addOwner(player.getUUID());
            }else {
                // 如果是主人，则添加好感
                this.getNeko().addXp(player.getUUID(), 100);
                // 1%的几率掉落唱片
                if (player.getRandom().nextInt(100) == 0) {
                    player.drop(new ItemStack(ToNekoItems.MUSIC_DISC_KAWAII),false);
                }
            }
            return true;
        }else {
            if (player instanceof ServerPlayer) {
                player.sendSystemMessage(randomTranslatabledComponent("message.toneko.neko.gift_fail", 3, Objects.requireNonNull(this.getCustomName()).getString()));
            }
            return false;
        }
    }

    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.inventory.getSelected();
        } else if (slot == EquipmentSlot.OFFHAND) {
            return this.inventory.offhand.getFirst();
        } else {
            return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? this.inventory.armor.get(slot.getIndex()) : ItemStack.EMPTY;
        }
    }

    public void setItemSlot(@NotNull EquipmentSlot slot, @NotNull ItemStack stack) {
        this.verifyEquippedItem(stack);
        if (slot == EquipmentSlot.MAINHAND) {
            this.onEquipItem(slot, this.inventory.items.set(this.inventory.selected, stack), stack);
        } else if (slot == EquipmentSlot.OFFHAND) {
            this.onEquipItem(slot, this.inventory.offhand.set(0, stack), stack);
        } else if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            this.onEquipItem(slot, this.inventory.armor.set(slot.getIndex(), stack), stack);
        }

    }

    public @NotNull ItemStack getItemInHand() {
        return this.inventory.getSelected();
    }

    public void selectItem(ItemStack stack) {
        this.inventory.selected = this.inventory.findSlotMatchingItem(stack);
    }

    public boolean addItem(ItemStack stack) {
        // 先尝试装备防具
        if (this.equipArmors(stack)) {
            return true;
        }
        // 否则放入背包
        return this.inventory.add(stack);
    }
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return this.inventory.armor;
    }

    public boolean equipArmors(ItemStack stack) {
        // 检查是否为防具
        if (!stack.is(NEKO_ARMOR)) {
            return false;
        }

        // 获取防具类型对应的槽位
        EquipmentSlot slot = null;
        if (stack.is(Items.LEATHER_HELMET) || stack.is(Items.CHAINMAIL_HELMET) ||
                stack.is(Items.IRON_HELMET) || stack.is(Items.GOLDEN_HELMET) ||
                stack.is(Items.DIAMOND_HELMET) || stack.is(Items.NETHERITE_HELMET)) {
            slot = EquipmentSlot.HEAD;
        } else if (stack.is(Items.LEATHER_CHESTPLATE) || stack.is(Items.CHAINMAIL_CHESTPLATE) ||
                stack.is(Items.IRON_CHESTPLATE) || stack.is(Items.GOLDEN_CHESTPLATE) ||
                stack.is(Items.DIAMOND_CHESTPLATE) || stack.is(Items.NETHERITE_CHESTPLATE)) {
            slot = EquipmentSlot.CHEST;
        } else if (stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.CHAINMAIL_LEGGINGS) ||
                stack.is(Items.IRON_LEGGINGS) || stack.is(Items.GOLDEN_LEGGINGS) ||
                stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.NETHERITE_LEGGINGS)) {
            slot = EquipmentSlot.LEGS;
        } else if (stack.is(Items.LEATHER_BOOTS) || stack.is(Items.CHAINMAIL_BOOTS) ||
                stack.is(Items.IRON_BOOTS) || stack.is(Items.GOLDEN_BOOTS) ||
                stack.is(Items.DIAMOND_BOOTS) || stack.is(Items.NETHERITE_BOOTS)) {
            slot = EquipmentSlot.FEET;
        }

        if (slot == null) return false;

        // 获取当前装备
        ItemStack currentArmor = this.getItemBySlot(slot);

        // 计算防御值比较
        int newDefense = calculateDefenseValue(stack);
        int currentDefense = calculateDefenseValue(currentArmor);

        // 如果新防具更好则替换
        if (newDefense > currentDefense) {
            // 替换装备
            this.setItemSlot(slot, stack.copy().split(1)); // 只装备一个

            // 丢弃旧装备（如果存在）
            if (!currentArmor.isEmpty()) {
                this.spawnAtLocation(currentArmor);
            }
            return true;
        }
        return false;
    }

    // 计算防具的防御值
    private int calculateDefenseValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        // 基础防御值
        int defense = 0;
        if (stack.getItem() instanceof ArmorItem armorItem) {
            defense = armorItem.getDefense();
        }
        return defense;
    }

    /**
     * 获取当前装备的所有防具
     * @return 包含四个ItemStack的列表，顺序为：头盔、胸甲、护腿、靴子
     */
    public List<ItemStack> getCurrentArmors() {
        List<ItemStack> armors = new ArrayList<>(4);
        armors.add(this.getItemBySlot(EquipmentSlot.HEAD));
        armors.add(this.getItemBySlot(EquipmentSlot.CHEST));
        armors.add(this.getItemBySlot(EquipmentSlot.LEGS));
        armors.add(this.getItemBySlot(EquipmentSlot.FEET));
        return armors;
    }

    /**
     * 获取指定槽位的防具
     * @param slot 装备槽位
     * @return 对应槽位的防具ItemStack
     */
    public ItemStack getArmorInSlot(EquipmentSlot slot) {
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
            return ItemStack.EMPTY;
        }
        return this.getItemBySlot(slot);
    }

    /**
     * 获取当前装备的总防御值
     * @return 所有防具的防御值总和
     */
    public int getTotalArmorValue() {
        int total = 0;
        for (ItemStack armor : getCurrentArmors()) {
            total += calculateDefenseValue(armor);
        }
        return total;
    }

    /**
     * 获取当前装备的防御值百分比（0.0-1.0）
     * @return 伤害减免百分比（0.0表示无减免，1.0表示完全免疫）
     */
    public float getArmorProtectionPercentage() {
        int totalDefense = getTotalArmorValue();
        float reduction = totalDefense * 0.04f;
        return Math.min(reduction, 0.8f); // 最高80%减免
    }

    /**
     * 检查是否有穿戴任何防具
     * @return 如果至少穿戴一件防具则返回true
     */
    public boolean isWearingAnyArmor() {
        for (ItemStack armor : getCurrentArmors()) {
            if (!armor.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        super.die(damageSource);
        Level world = this.level();
        if (world instanceof ServerLevel serverLevel) {
            this.dropAllDeathLoot(serverLevel, damageSource);
            this.getInventory().dropAll();
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);

        // 获取猫猫对象
        NekoQuery.Neko neko = this.getNeko();
        if (neko == null) {
            LOGGER.warn("Neko instance is null for UUID: {}", this.getUUID());
            return;
        }

        if (reason.shouldDestroy()) {
            // 需要销毁数据
            NekoQuery.NekoData.deleteNeko(this.getUUID());
        } else if (reason.shouldSave()) {
            // 需要保存数据
            NekoQuery.NekoData.saveAndRemoveNeko(this.getUUID());
        } else {
            // 既不需要销毁也不需要保存
            NekoQuery.NekoData.deleteNeko(this.getUUID());
        }
    }


    @Override
    public void baseTick() {
        super.baseTick();
        // 啊不要学我
        slowTimer++;
        if (slowTimer >= 20){
            slowTimer = 0;
            slowTick();
        }
    }
    public void slowTick(){
        if (!this.level().isClientSide()){
            this.setMoeTags(this.getMoeTags());
            this.setSkin(this.getSkin());
            this.serverNekoSlowTick();
        }
    }

    @Nullable
    public ItemEntity drop(ItemStack itemStack, boolean includeThrowerName) {
        return this.drop(itemStack, false, includeThrowerName);
    }

    @Nullable
    public ItemEntity drop(ItemStack droppedItem, boolean dropAround, boolean includeThrowerName) {
        if (droppedItem.isEmpty()) {
            return null;
        } else {
            if (this.level().isClientSide) {
                this.swing(InteractionHand.MAIN_HAND);
            }

            double d = this.getEyeY() - 0.30000001192092896;
            ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), d, this.getZ(), droppedItem);
            itemEntity.setPickUpDelay(40);
            if (includeThrowerName) {
                itemEntity.setThrower(this);
            }

            float f;
            float g;
            if (dropAround) {
                f = this.random.nextFloat() * 0.5F;
                g = this.random.nextFloat() * 6.2831855F;
                itemEntity.setDeltaMovement(-Mth.sin(g) * f, 0.20000000298023224, Mth.cos(g) * f);
            } else {
                g = Mth.sin(this.getXRot() * 0.017453292F);
                float h = Mth.cos(this.getXRot() * 0.017453292F);
                float i = Mth.sin(this.getYRot() * 0.017453292F);
                float j = Mth.cos(this.getYRot() * 0.017453292F);
                float k = this.random.nextFloat() * 6.2831855F;
                float l = 0.02F * this.random.nextFloat();
                itemEntity.setDeltaMovement((double)(-i * h * 0.3F) + Math.cos(k) * (double)l, -g * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F, (double)(j * h * 0.3F) + Math.sin(k) * (double)l);
            }

            return itemEntity;
        }
    }

    public void tryMating(ServerLevel level, INeko mate) {
        if (this.canMate(mate)) {
            this.nekoMateGoal.setTarget(mate);
            mate.getEntity().sendSystemMessage(Component.translatable("message.toneko.neko.mate.start",this.getName(), mate.getEntity().getName()).withStyle(ChatFormatting.GREEN));
        }else {
            mate.getEntity().sendSystemMessage(Component.translatable("message.toneko.neko.mate.fail",this.getName(), mate.getEntity().getName()).withStyle(ChatFormatting.RED));
        }
    }
    public void afterMate() {
        this.nekoMateGoal.mating = 0;
    }
    public boolean canMate(INeko other){
        return (other.getNeko().isNeko() || other.allowMateIfNotNeko()) && !this.hasEffect(MobEffects.WEAKNESS) && !other.getEntity().hasEffect(MobEffects.WEAKNESS);
    }

    public void breed(ServerLevel level, INeko mate) {
        // 冒爱心
        level.addParticle(ParticleTypes.HEART, this.getX(), this.getY(), this.getZ(), 1, 1, 1);
        Packet<?> packet = new ClientboundLevelParticlesPacket(ParticleTypes.HEART,true, this.getX(), this.getY(), this.getZ(),3,3,3,0.2f,25);
        if (mate instanceof ServerPlayer sp){
            sp.connection.send(packet);
        }
        // 增加等级
        this.getNeko().addLevel(0.1);
        mate.getNeko().addLevel(0.1);
        NekoEntity baby = this.spawnChildFromBreeding(level, mate);
        // 分别给予虚弱效果
        this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3000, 0));
        mate.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3000, 0));
    }
    public NekoEntity spawnChildFromBreeding(ServerLevel level, INeko mate) {
        NekoEntity child = this.getBreedOffspring(level, mate);
        if (child != null) {
            child.setBaby(true);
            child.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            this.finalizeSpawnChildFromBreeding(level, mate, child);
            level.addFreshEntityWithPassengers(child);
        }
        return child;
    }
    public void finalizeSpawnChildFromBreeding(ServerLevel level, INeko mate, NekoEntity child) {
        level.broadcastEntityEvent(this, (byte)18);
        child.setAge(-72000);
        if (level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            level.addFreshEntity(new ExperienceOrb(level, this.getX(), this.getY(), this.getZ(), this.getRandom().nextInt(7) + 1));
        }
    }
    @Nullable
    public abstract NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent);
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        if (otherParent instanceof INeko neko){
            return this.getBreedOffspring(level, neko);
        }
        return null;
    }

    // 当玩家右键
    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        // shift+右键打开互动菜单
        if (hand.equals(InteractionHand.MAIN_HAND) && player.isShiftKeyDown() && player instanceof ServerPlayer sp){
            openInteractiveMenu(sp);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    public void openInteractiveMenu(ServerPlayer player) {
        ServerPlayNetworking.send(player, new NekoEntityInteractivePayload(this.getUUID().toString()));
    }

    @Override
    public boolean startRiding(@NotNull Entity vehicle, boolean force) {
        this.isSitting = super.startRiding(vehicle, force);
        return this.isSitting;
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        this.isSitting = false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 20, state -> {
            // 地上趴着
            if (this.getPose() == Pose.SWIMMING && !this.isInLiquid()){
                return state.setAndContinue(DefaultAnimations.CRAWL);
            }
            // 在水里
            if (this.isInLiquid() && this.isEyeInFluid(FluidTags.WATER)){
                if (state.isMoving()) {
                    return state.setAndContinue(DefaultAnimations.SWIM);
                }else {
                    return state.setAndContinue(DefaultAnimations.CRAWL);
                }
            }
            // 没有移动
            if (!state.isMoving()){
                // 是否为sit
                if (this.isSitting()) return state.setAndContinue(RawAnimation.begin().thenLoop("misc.sit"));
                return state.setAndContinue(DefaultAnimations.IDLE);
            }else {
                // 如果速度较快
                if (this.getDeltaMovement().length() > 0.2){
                    return state.setAndContinue(DefaultAnimations.RUN);
                }
                return state.setAndContinue(DefaultAnimations.WALK);
            }

        }));
    }

    @Override
    public void tick() {
        super.tick();
        this.inventory.tick();
    }

    @Override
    public void move(@NotNull MoverType type, @NotNull Vec3 pos) {
        if (this.canMove()) {
            super.move(type, pos);
        }
    }

    @Override
    public void moveTo(double x, double y, double z, float yRot, float xRot) {
        if (this.canMove()){
            super.moveTo(x, y, z, yRot, xRot);
        }
    }

    public boolean canMove() {
        return this.getPose() != Pose.SWIMMING && !this.isSitting() || this.isInLiquid();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // 计算防具提供的伤害减免
        float damageAfterArmor = calculateDamageAfterArmor(source, amount);

        if (source.getEntity() instanceof Player player){
            // 栓住玩家
            if (player.getMainHandItem().is(Items.LEAD)){
                if (player.isLeashed()){
                    player.dropLeash(true, true);
                }else {
                    player.setLeashedTo(this, true);
                    // 减少栓绳
                    player.getMainHandItem().setCount(player.getMainHandItem().getCount() - 1);
                }
                return false;
            }
        }

        // 使用经过防具减免后的伤害值
        boolean result = super.hurt(source, damageAfterArmor);

        if (!result) return false;
        if (source.getEntity() instanceof Player player){
            hurtByPlayer(player);
        }
        return true;
    }

    // 计算经过防具减免后的伤害值
    private float calculateDamageAfterArmor(DamageSource source, float amount) {
        // 基础伤害值
        float damage = amount;

        // 计算所有防具的总防御值
        int totalDefense = 0;
        for (ItemStack armor : this.inventory.armor) {
            if (!armor.isEmpty()) {
                totalDefense += calculateDefenseValue(armor);
            }
        }

        // 应用防御值减免（每点防御值减少4%伤害）
        if (totalDefense > 0) {
            float reduction = totalDefense * 0.04f;
            damage *= (1.0f - Math.min(reduction, 0.8f)); // 最多减免80%伤害
        }

        return Math.max(0, damage);
    }

    // 防具耐久损耗
    protected void damageArmor(DamageSource source, float damageAmount) {
        if (damageAmount <= 0.0F) return;

        damageAmount /= 4.0F; // 伤害平均分配到四件防具上
        if (damageAmount < 1.0F) damageAmount = 1.0F;

        for (int i = 0; i < this.inventory.armor.size(); i++) {
            ItemStack armor = this.inventory.armor.get(i);
            if (!armor.isEmpty() && armor.getItem() instanceof ArmorItem  armorItem) {
                armor.hurtAndBreak((int)damageAmount, this,armorItem.getEquipmentSlot());
            }
        }
    }

    public void hurtByPlayer(Player player){
        sendHurtMessageToPlayer(player);
    }

    public void sendHurtMessageToPlayer(Player player){
        if (player instanceof ServerPlayer) {
            var moe = this.getMoeTags();
            var name = this.getName();
            if (moe.contains("yandere")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.yandere",5,name));
            }else if (moe.contains("chunibyo")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.chunibyo",5,name));
            }else if (moe.contains("mesugaki")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.mesugaki",5,name));
            }else if (moe.contains("tsundere")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.tsundere",5,name));
            } else if (moe.contains("tennen_boke")) {
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.tennen_boke",5,name));
            }else {
                int r = random.nextInt(6);
                player.sendSystemMessage(Component.translatable("message.toneko.neko.on_hurt." + r, this.getName()));
            }
        }
    }

    public String generateAIPrompt(Player player) {
        return PromptRegistry.generatePrompt(this,player,ConfigUtil.getAIPrompt());
    }

    @Override
    public @NotNull Component getTypeName() {return super.getTypeName();}

    public String getDescription() {
        return Component.translatable(getType().getDescriptionId()+".des").getString();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public LivingEntity getEntity() {
        return this;
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public boolean isNeko() {
        return true;
    }

    @Override
    public boolean checkSpawnRules(@NotNull LevelAccessor level, @NotNull MobSpawnType reason) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull EntityType<? extends NekoEntity> getType() {
        return (EntityType<? extends NekoEntity>) super.getType();
    }

    public static AttributeSupplier.Builder createNekoAttributes(){
        return createMobAttributes().add(Attributes.ATTACK_DAMAGE).add(Attributes.ATTACK_SPEED).add(ToNekoAttributes.NEKO_DEGREE).add(ToNekoAttributes.MAX_NEKO_ENERGY);
    }

}

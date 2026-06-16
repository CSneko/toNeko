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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;
import org.cneko.toneko.common.mod.ai.PromptRegistry;
import org.cneko.toneko.common.mod.api.NekoLevelRegistry;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.ai.goal.*;
import org.cneko.toneko.common.mod.genetics.ToNekoLocus;
import org.cneko.toneko.common.mod.genetics.api.*;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;
import org.cneko.toneko.common.mod.misc.ToNekoSoundEvents;
import org.cneko.toneko.common.mod.packets.interactives.NekoEntityInteractivePayload;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.cneko.toneko.common.mod.util.EntityUtil;
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

import java.util.*;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;
import static org.cneko.toneko.common.mod.util.TextUtil.randomTranslatabledComponent;

public abstract class NekoEntity extends AgeableMob implements GeoEntity, INeko, IGeneticEntity {
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

    // 萌属性缓存
    private String lastMoeTagsStringCache = null;
    private List<String> cachedMoeTagsList = null;

    // 被动回血
    protected static final int PASSIVE_HEAL_INTERVAL = 600; // 30秒回血一次
    private int passiveHealTimer = 0;

    // 受伤求救冷却时间，避免被连击时疯狂扫描附近实体
    private long lastHelpCallTime = 0;
    private long lastLoliAlarmTime = 0;
    // ====== 通用仇恨系统（绕过GoalSystem，直接在tick中处理）=======
    protected static final ResourceLocation HATRED_ATTACK_BOOST_ID = toNekoLoc("hatred_attack_boost");
    protected static final double HATRED_ATTACK_BOOST = 0.2; // 攻击力倍率（×1.2）
    protected static final double HATRED_ATTACK_RANGE = 4.0; // 攻击距离（平方根后为2格）
    protected static final int HATRED_ATTACK_COOLDOWN = 20; // 攻击间隔（tick）
    protected static final int HATRED_DEFAULT_DURATION = 600; // 默认追击持续时间
    @Nullable
    protected LivingEntity hatredTarget = null;
    protected int hatredCooldown = 0;
    protected int hatredAttackCooldown = 0;
    protected int hatredMessageCooldown = 0;

    // 用于动画渲染的客户端状态缓存，避免每帧都去查方块状态
    private boolean clientIsInLiquid = false;
    private boolean clientIsEyeInWater = false;
    // 服务端液体状态缓存，避免 canMove() 被高频率调用时反复查方块状态
    private boolean serverIsInLiquid = false;

    // 遗传
    private Genome genome = new Genome();
    private final CompoundTag geneticData = new CompoundTag();
    private final List<ExpressedTrait> activeTraits = new ArrayList<>();
    private final List<Goal> activeGeneticGoals = new ArrayList<>();

    public NekoFollowOwnerGoal nekoFollowOwnerGoal;
    public NekoMateGoal nekoMateGoal;
    private final AnimatableInstanceCache cache;
    @Getter
    private boolean isSitting = false;
    @Getter
    final NekoInventory inventory = new NekoInventory(this);
    private short slowTimer = 20;
    private CompoundTag nekoLevelFactorData = new CompoundTag();

    public static final EntityDataAccessor<String> SKIN_DATA_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> MOE_TAGS_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> GATHERING_POWER_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> NEKO_ENERGY_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> CHEST_SCALE_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> AGE_SCALE_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.FLOAT);

    public NekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        this.cache = GeckoLibUtil.createInstanceCache(this);
    }


    public void randomize(){
        // 设置名字（如果没有）
        if (!this.hasCustomName()) {
            this.setCustomName(Component.literal(NekoNameRegistry.getRandomName()));
        }

        EntityUtil.randomizeAttributeValue(this, Attributes.SCALE,1,0.65,1.05); // 实体的体型为0.65~1.05间
        EntityUtil.randomizeAttributeValue(this, Attributes.MOVEMENT_SPEED,0.7,0.5,0.6); // 实体速度为0.5~0.6间

        // 随机皮肤（仅在未被基因系统等修改过时生效）
        if (this.getSkin().equals(this.getDefaultSkin())) {
            this.setSkin(NekoSkinRegistry.getRandomSkin(getType()));
        }

        if (!this.hasAnyMoeTags()) {
            this.generateRandomMoeTags();
        }
    }

    /**
     * 辅助方法：随机生成 2~3 个不重复的萌属性
     */
    public void generateRandomMoeTags() {
        // 1. 创建标签列表的副本（因为我们要打乱它，不能直接改静态的常量列表）
        List<String> availableTags = new ArrayList<>(MOE_TAGS);

        // 2. 打乱列表顺序 (使用实体的随机种子)
        Collections.shuffle(availableTags, new java.util.Random(this.random.nextLong()));

        // 3. 随机决定数量：1~3
        int count = Mth.nextInt(this.random, 1, 3);

        // 4. 截取前 count 个元素作为结果
        List<String> selectedTags = availableTags.subList(0, count);

        // 5. 设置属性
        this.setMoeTags(selectedTags);
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_DATA_ID, this.getDefaultSkin());
        builder.define(MOE_TAGS_ID, "");
        builder.define(GATHERING_POWER_ID, 0);
        builder.define(NEKO_ENERGY_ID, 0f);
        builder.define(NEKO_LEVEL_ID, 0f);
        builder.define(NICKNAME_ID, "");
        builder.define(CHEST_SCALE_ID, 1.0f);
        builder.define(AGE_SCALE_ID, 1.0f);
    }



    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("Skin", this.getSkin());
        compound.put("Inventory", this.inventory.save(new ListTag()));
        compound.putInt("SelectedItemSlot", this.inventory.selected);
        compound.putInt("GatheringPower", this.getGatheringPower());
        compound.putString("MoeTags", String.join(":", this.getMoeTags()));
        this.saveNekoNBTData(compound);
        compound.put("Genome", this.genome.save());
        compound.put("GeneticData", this.geneticData);
    }

    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Skin")) {
            this.setSkin(compound.getString("Skin"));
        }
        if (compound.contains("Inventory")) {
            ListTag listTag = compound.getList("Inventory", 10);
            this.inventory.load(listTag);
            if (compound.contains("SelectedItemSlot")) {
                this.inventory.selected = compound.getInt("SelectedItemSlot");
            }
        }
        if (compound.contains("GatheringPower")){
            this.setGatheringPower(compound.getInt("GatheringPower"));
        }
        if (compound.contains("MoeTags")) {
            this.entityData.set(MOE_TAGS_ID, compound.getString("MoeTags"));
        }else {
            // 随机设置2~3个萌属性喵
            List<String> tags = new ArrayList<>();
            for (int i = 0; i < Mth.nextInt(this.random, 2, 3); i++) {
                tags.add(MOE_TAGS.get(Mth.nextInt(this.random, 0, MOE_TAGS.size() - 1)));
            }
            this.setMoeTags(tags);

        }
        this.loadNekoNBTData(compound);
        if (compound.contains("Genome")) {
            this.genome.load(compound.getCompound("Genome"));
        }
        if (compound.contains("GeneticData")) {
            this.geneticData.merge(compound.getCompound("GeneticData"));
        }

        // 载入完成后必须表达基因
        this.expressTraits();
    }

    @Override
    public void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new org.cneko.toneko.common.mod.entities.ai.goal.NekoChunibyoGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.3, 1));
        this.goalSelector.addGoal(6, new org.cneko.toneko.common.mod.entities.ai.goal.NekoSelfPreservationGoal(this));
        this.goalSelector.addGoal(6, new org.cneko.toneko.common.mod.entities.ai.goal.NekoLivelyGoal(this));
        this.goalSelector.addGoal(5, new BreathAirGoal(this));
        this.goalSelector.addGoal(5, new NekoPickupItemGoal(this));
        this.goalSelector.addGoal(5, new TemptGoal(this, 0.5D,
                stack -> this.getMoeTags().contains("narenareshi") || this.isFavoriteItem(stack), false));
        nekoFollowOwnerGoal = new NekoFollowOwnerGoal(this, null, 30, Math.min(0.1, this.followLeashSpeed() / 1.5));
        this.goalSelector.addGoal(4, nekoFollowOwnerGoal);
        this.goalSelector.addGoal(4, new org.cneko.toneko.common.mod.entities.ai.goal.NekoHealGoal(this));
        nekoMateGoal = new NekoMateGoal(this, null, 30, this.followLeashSpeed() / 2);
        this.goalSelector.addGoal(3, nekoMateGoal);
        this.goalSelector.addGoal(2, new NekoSleepInBedGoal(this));
        this.goalSelector.addGoal(2, new org.cneko.toneko.common.mod.entities.ai.goal.NekoParanoiaGoal(this));
        this.goalSelector.addGoal(1, new NekoEscapeDangerGoal(this));
        this.goalSelector.addGoal(1, new org.cneko.toneko.common.mod.entities.ai.goal.NekoYandereDefenseGoal(this));
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
        if (this.entityData.get(SKIN_DATA_ID)== null || this.entityData.get(SKIN_DATA_ID).isEmpty()) {
            this.setSkin(getRandomSkin());
        }
        return this.entityData.get(SKIN_DATA_ID);
    }
    public void setSkin(String skin) {
        //noinspection ConstantValue
        if (this.entityData != null && skin != null && !skin.isEmpty()) {
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
        String moeTagsString = this.entityData.get(MOE_TAGS_ID);
        // 如果同步数据没有变，直接返回缓存的 List
        if (cachedMoeTagsList == null || !moeTagsString.equals(lastMoeTagsStringCache)) {
            lastMoeTagsStringCache = moeTagsString;
            cachedMoeTagsList = moeTagsString.isEmpty() ? List.of() : List.of(moeTagsString.split(":"));
        }
        return cachedMoeTagsList;
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

    public boolean hasAnyMoeTags(){
        return !this.getMoeTags().isEmpty();
    }
    public void setMoeTags(List<String> moeTags){
        String tags = moeTags != null ? String.join(":", moeTags) : "";
        this.entityData.set(MOE_TAGS_ID, tags);
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
            if (this.getLastHurtByMob()==player){
                // 消除仇恨
                this.setLastHurtByMob(null);
            }
            // 增长动力
            this.addGatheringPower(10);
            if (this.equipArmors(stack)){
                // 装备
                return true;
            }
            // 达成进度
            ToNekoCriteria.GIFT_NEKO.trigger(sp);
            if (this.getInventory().canAdd()) {
                ItemStack s = stack.copy();
                s.setCount(1);
                this.addItem(s);

                if (!player.isCreative()) {
                    stack.shrink(1);
                }
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
            sendGiftMessageToPlayer(player);

            if (this.hasOwner(player.getUUID())){
                // 如果是主人，则添加好感
                this.setXpWithOwner(player.getUUID(), this.getXpWithOwner(player.getUUID()) + 20);
                // 1%的几率掉落唱片
                if (player.getRandom().nextInt(100) == 0) {
                    player.drop(new ItemStack(ToNekoItems.MUSIC_DISC_KAWAII),false);
                }
            }
            // 增加互动等级因子
            NekoLevelRegistry.interaction().addRaw(this, 15.0);
            if (player.isNeko()) {
                NekoLevelRegistry.interaction().addRaw(player, 15.0);
            }
            return true;
        }else {
            if (player instanceof ServerPlayer) {
                player.sendSystemMessage(randomTranslatabledComponent("message.toneko.neko.gift_fail", 3, Objects.requireNonNull(this.getCustomName()).getString()));
            }
            return false;
        }
    }
    public void sendGiftMessageToPlayer(Player player){
        player.sendSystemMessage(randomTranslatabledComponent("message.toneko.neko.gift_success",3, Objects.requireNonNull(this.getCustomName()).getString()));
    }

    @Override
    public void setLastHurtByPlayer(@Nullable Player player) {
        if (player == null || !this.hasOwner(player.getUUID())) {
            super.setLastHurtByPlayer(player);
        }
        super.setLastHurtByPlayer(null);
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
        // 如果是食物，则吃掉回血并获取对应的效果
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        if (food!=null && (this.getHealth() < this.getMaxHealth() || !food.effects().isEmpty())){
            // 回血
            this.heal(food.nutrition());
            this.eat(this.level(), stack);
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
    public void travel(@NotNull Vec3 travelVector) {
        if (this.isInWater()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.02, 0)); // 增加上浮力
        }
        super.travel(travelVector);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (!this.level().isClientSide() && this.getMoeTags().contains("baka")
                && this.random.nextFloat() < 0.002f) {
            // baka: occasionally get distracted and stop navigating
            this.getNavigation().stop();
            this.getLookControl().setLookAt(
                    this.getX() + this.random.nextGaussian() * 3,
                    this.getY() + this.random.nextFloat() * 2,
                    this.getZ() + this.random.nextGaussian() * 3
            );
        }
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
            // 先同步年龄缩放再更新 modifier，保证 getNekoAgeScale() 读到最新值
            this.entityData.set(AGE_SCALE_ID, (float) computeAgeScale());
            this.updateNekoLevelModifiers();
            this.entityData.set(NEKO_LEVEL_ID, this.getNekoLevel());
            this.updateMoeTagAwareGoals();
            // dojikko: 0.5% chance per second to drop a random item
            if (this.getMoeTags().contains("dojikko") && this.random.nextFloat() < 0.005f) {
                ItemStack dropped = this.getRandomInventoryItem();
                if (!dropped.isEmpty()) {
                    this.spawnAtLocation(dropped.split(1));
                }
            }
            // 被动回血：缓慢自然恢复
            passiveHealTimer++;
            if (passiveHealTimer >= PASSIVE_HEAL_INTERVAL) {
                passiveHealTimer = 0;
                float amount = getPassiveHealAmount();
                if (amount > 0 && this.getHealth() < this.getMaxHealth()) {
                    this.heal(amount);
                }
            }
        }
    }

    /** 每次被动回血的量，子类可重写以改变回血速度 */
    protected float getPassiveHealAmount() {
        return 1.0f;
    }

    private void updateMoeTagAwareGoals() {
        if (nekoFollowOwnerGoal == null) return;
        List<String> tags = this.getMoeTags();
        double dist = 30;
        double speed = Math.min(0.1, this.followLeashSpeed() / 1.5);
        if (tags.contains("yandere")) {
            dist = 40;
            speed *= 1.5;
        } else if (tags.contains("yowaki")) {
            dist = 15;
            speed *= 0.9;
        } else if (tags.contains("shizukana")) {
            speed *= 0.7;
        }
        nekoFollowOwnerGoal.setMaxDistance(dist);
        nekoFollowOwnerGoal.setFollowSpeed(speed);
    }

    public ItemStack getRandomInventoryItem() {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) return stack.copy();
        }
        return ItemStack.EMPTY;
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

            this.level().addFreshEntity(itemEntity);
            return itemEntity;
        }
    }

    public void tryMating(ServerLevel level, INeko mate) {
        if (this.isNekoBaby() || mate.isNekoBaby()) {
            mate.getEntity().sendSystemMessage(Component.translatable("message.toneko.neko.mate.fail",this.getName(), mate.getEntity().getName()).withStyle(ChatFormatting.RED));
            return;
        }
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
        if (this.isNekoBaby() || other.isNekoBaby()) return false;
        return (other.isNeko() || other.allowMateIfNotNeko()) && !this.hasEffect(MobEffects.WEAKNESS) && !other.getEntity().hasEffect(MobEffects.WEAKNESS);
    }

    public void breed(ServerLevel level, INeko mate) {
        // 冒爱心
        level.addParticle(ParticleTypes.HEART, this.getX(), this.getY(), this.getZ(), 1, 1, 1);
        Packet<?> packet = new ClientboundLevelParticlesPacket(ParticleTypes.HEART,true, this.getX(), this.getY(), this.getZ(),3,3,3,0.2f,25);
        if (mate instanceof ServerPlayer sp){
            sp.connection.send(packet);
        }

        // 生成配子
        Gamete paternalGamete = this.getGenome().createGamete(this.getRandom());
        Gamete maternalGamete;

        // 如果另一半也实现了遗传接口
        if (mate.getEntity() instanceof IGeneticEntity geneticMate) {
            maternalGamete = geneticMate.getGenome().createGamete(mate.getEntity().getRandom());
        } else {
            // 对另一半进行随机降级处理
            maternalGamete = Genome.generateFallbackGamete(mate.getEntity().getRandom(), ToNekoLocus.NEKO_KARYOTYPE);
        }

        // 基因组合并
        Genome childGenome = Genome.combine(paternalGamete, maternalGamete,ToNekoLocus.NEKO_KARYOTYPE);

        // 生成子代实体
        NekoEntity child = this.spawnChildFromBreeding(level, mate);
        if (child != null) {
            child.setGenome(childGenome);
            child.expressTraits(); // 让基因立刻生效，改变属性和外观
        }

        // 分别给予虚弱效果
        this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3000, 0));
        mate.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3000, 0));
    }
    public NekoEntity spawnChildFromBreeding(ServerLevel level, INeko mate) {
        NekoEntity child = this.getBreedOffspring(level, mate);
        if (child != null) {
            child.setNekoBaby(true);
            child.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            this.finalizeSpawnChildFromBreeding(level, mate, child);
            level.addFreshEntityWithPassengers(child);
        }
        return child;
    }
    public void finalizeSpawnChildFromBreeding(ServerLevel level, INeko mate, NekoEntity child) {
        level.broadcastEntityEvent(this, (byte)18);
        child.setNekoBaby(true); // 使用 INeko 统一的 MaxAge 机制
        child.randomize(); // 随机化名字、皮肤、体型、速度、萌属性
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
            NekoEntity child = this.getBreedOffspring(level, neko);
            if (child != null) {
                // 分配随机基因（兼容刷怪蛋、繁殖等不走finalizeSpawn的路径）
                Gamete gamete1 = Genome.generateFallbackGamete(child.random, ToNekoLocus.NEKO_KARYOTYPE);
                Gamete gamete2 = Genome.generateFallbackGamete(child.random, ToNekoLocus.NEKO_KARYOTYPE);
                child.setGenome(Genome.combine(gamete1, gamete2, ToNekoLocus.NEKO_KARYOTYPE));
                child.expressTraits();
                // 强制随机化名字、皮肤、萌属性
                child.setCustomName(Component.literal(NekoNameRegistry.getRandomName()));
                child.setSkin(NekoSkinRegistry.getRandomSkin(child.getType()));
                child.generateRandomMoeTags();
                EntityUtil.randomizeAttributeValue(child, Attributes.SCALE,1,0.65,1.05);
                EntityUtil.randomizeAttributeValue(child, Attributes.MOVEMENT_SPEED,0.7,0.5,0.6);
            }
            return child;
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
            if (this.getPose() == Pose.SWIMMING && !this.clientIsInLiquid){
                return state.setAndContinue(DefaultAnimations.CRAWL);
            }
            // 在水里
            if (this.clientIsInLiquid && this.clientIsEyeInWater){
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
        // 在 tick() 中缓存流体状态，避免 AI/动画每帧都查方块状态
        if (this.level().isClientSide()) {
            this.clientIsInLiquid = this.isInLiquid();
            this.clientIsEyeInWater = this.isEyeInFluid(FluidTags.WATER);
        } else {
            this.serverIsInLiquid = this.isInLiquid();
            // ====== 通用仇恨系统（子类可重写此方法来自定义攻击行为）=======
            tickHatred();

            // 环境粒子效果（每5秒一次）
            if (this.tickCount % 100 == 0) {
                spawnAmbientParticles();
            }

            // 萝莉猫娘防狼：检测玩家侵犯意图（持武器接近）
            if (this.isNekoBaby() && this.tickCount % 40 == 0) {
                long currentTime = this.level().getGameTime();
                if (currentTime - this.lastLoliAlarmTime > 1200) { // 60秒冷却，防止误触
                    for (Player player : this.level().getEntitiesOfClass(Player.class,
                            this.getBoundingBox().inflate(5),
                            p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
                        if (this.hasLineOfSight(player) && isWeapon(player.getMainHandItem())) {
                            this.lastLoliAlarmTime = currentTime;
                            triggerLoliAlarm(player);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 环境粒子效果，每5秒调用一次（仅服务端）。
     * 子类可重写以实现自定义粒子。
     * 默认：在主人附近时偶尔冒出爱心，体现猫娘的依恋。
     */
    protected void spawnAmbientParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // 当前面无主人时跳过
        boolean hasOwnerNearby = false;
        for (var entry : this.getOwners().entrySet()) {
            Player owner = this.level().getPlayerByUUID(entry.getKey());
            if (owner != null && owner.isAlive() && this.distanceToSqr(owner) < 100.0) { // 10格内
                hasOwnerNearby = true;
                break;
            }
        }
        if (!hasOwnerNearby) return;

        // 15% 概率冒出爱心
        if (this.random.nextFloat() < 0.15f) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
                    1, 0.3, 0.2, 0.3, 0.02);
        }
    }

    @Override
    public void move(@NotNull MoverType type, @NotNull Vec3 pos) {
        if (this.canMove()) {
            super.move(type, pos);
        }
    }

    @Override
    public void moveTo(double x, double y, double z, float yRot, float xRot) {
        // moveTo 是直接设置位置的 teleport/生成 API，不是 AI 移动，不应用 canMove 限制
        super.moveTo(x, y, z, yRot, xRot);
    }

    public boolean canMove() {
        return !this.isSitting() || this.serverIsInLiquid;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
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

        // 萝莉猫娘防狼警报：如果被玩家攻击且是幼体（萝莉）
        if (source.getEntity() instanceof Player player && this.isNekoBaby()) {
            triggerLoliAlarm(player);
        }

        boolean result = super.hurt(source, amount);

        if (!result) return false;
        if (source.getEntity() instanceof Player player){
            hurtByPlayer(player);
        }
        // 被攻击时对攻击者产生仇恨（不攻击主人）
        if (source.getEntity() instanceof LivingEntity attacker
                && !this.hasOwner(attacker.getUUID())) {
            this.setHatredTarget(attacker, HATRED_DEFAULT_DURATION);
        }
        // 寻找回血食物
        for (ItemStack stack : this.getInventory().items){
            // 如果是食物，则吃掉回血并获取对应的效果
            FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
            if (food!=null && (this.getHealth() < this.getMaxHealth() || !food.effects().isEmpty())){
                // 回血
                this.heal(food.nutrition());
                this.eat(this.level(), stack);
                stack.shrink(1);
            }
        }
        // 召集附近猫娘共同作战（40tick冷却）
        // 只在攻击者是生物实体且不是主人的情况下召集
        if (source.getEntity() instanceof LivingEntity attacker
                && !this.hasOwner(attacker.getUUID())) {
            long currentTime = this.level().getGameTime();
            if (currentTime - this.lastHelpCallTime > 40) {
                this.lastHelpCallTime = currentTime;
                // 寻找附近所有猫娘（不限于FightingNeko）
                List<NekoEntity> nearbyNekos = this.level().getEntitiesOfClass(NekoEntity.class,
                        this.getBoundingBox().inflate(DEFAULT_FIND_RANGE), LivingEntity::isAlive
                );
                // 设置仇恨（如果攻击者是某只猫娘的主人，则该猫娘不参与反击）
                for (NekoEntity neko : nearbyNekos) {
                    if (neko != this && !neko.hasOwner(attacker.getUUID())) {
                        neko.setLastHurtByMob(attacker);
                        neko.setHatredTarget(attacker, HATRED_DEFAULT_DURATION);
                    }
                }
            }
        }
        return true;
    }

    // ====== 通用仇恨系统 API ========

    /**
     * 设置仇恨目标（通用接口）
     * @param target 要攻击的目标
     * @param duration 持续tick数
     */
    protected void setHatredTarget(LivingEntity target, int duration) {
        this.hatredTarget = target;
        this.hatredCooldown = duration;
        this.hatredAttackCooldown = 0;
        this.setTarget(target);
        this.setAggressive(true);
        // 伤害加成
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, false));
        AttributeInstance attr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null && attr.getModifier(HATRED_ATTACK_BOOST_ID) == null) {
            attr.addTransientModifier(new AttributeModifier(
                    HATRED_ATTACK_BOOST_ID,
                    HATRED_ATTACK_BOOST,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    /**
     * 尝试对当前仇恨目标发动攻击
     * @return 是否成功造成伤害
     */
    protected boolean tryHatredAttack() {
        if (this.hatredTarget == null || !this.hatredTarget.isAlive()) return false;
        if (this.distanceToSqr(this.hatredTarget) > HATRED_ATTACK_RANGE) return false;
        if (this.hatredAttackCooldown > 0) {
            this.hatredAttackCooldown--;
            return false;
        }

        equipBestMeleeWeapon();
        // 标准mob攻击
        boolean damaged = this.doHurtTarget(this.hatredTarget);
        if (!damaged) {
            // 备用方案：使用通用伤害类型（绕过可能的mobAttack免疫）
            damaged = this.hatredTarget.hurt(this.damageSources().generic(), 4.0f);
        }
        if (damaged) {
            this.hatredAttackCooldown = HATRED_ATTACK_COOLDOWN;
            // 攻击粒子效果（愤怒村民粒子）
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        this.getX(), this.getY() + this.getBbHeight() * 0.8, this.getZ(),
                        1, 0.2, 0.2, 0.2, 0);
            }
        }
        return damaged;
    }

    /**
     * 仇恨系统tick处理（protected以便子类重写自定义攻击行为）
     * 在super.tick()之后运行，默认提供导航追击+近战攻击，
     * 子类（如FightingNeko）可重写此方法使用自己的攻击AI
     */
    protected void tickHatred() {
        if (this.hatredTarget == null) return;
        if (!this.hatredTarget.isAlive() || this.hatredCooldown <= 0) {
            clearHatred();
            return;
        }
        this.hatredCooldown--;
        // 仅在当前没有活跃导航路径时设置追击路径，避免每tick覆盖GoalSystem的导航指令
        if (this.getNavigation().isDone()) {
            this.getNavigation().moveTo(this.hatredTarget, this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.2);
        }
        tryHatredAttack();
        trySendHatredMessage();
    }

    /**
     * 战斗中向仇恨目标发送消息，群殴时自动降低频率避免刷屏
     */
    protected void trySendHatredMessage() {
        if (this.level().isClientSide) return;
        if (!(this.hatredTarget instanceof Player player)) return;
        if (this.getCustomName() == null || this.getName().getString().equals("null")) {
            this.setCustomName(Component.literal(NekoNameRegistry.getRandomName()));
        }
        if (hatredMessageCooldown > 0) {
            hatredMessageCooldown--;
            return;
        }
        // 统计附近同样在攻击该玩家的猫娘数量，越多则每只发消息间隔越长
        long attackingNekos = this.level().getEntitiesOfClass(NekoEntity.class,
                this.getBoundingBox().inflate(20),
                n -> n != this && player.equals(n.hatredTarget) && n.isAlive()
        ).size();
        hatredMessageCooldown = 60 + (int)(attackingNekos * 30);
        int i = this.random.nextInt(15);
        player.sendSystemMessage(Component.translatable("message.toneko.neko.hatred_attack." + i, this.getName()));
    }

    /**
     * 清除仇恨状态
     */
    protected void clearHatred() {
        this.hatredTarget = null;
        this.hatredAttackCooldown = 0;
        this.setTarget(null);
        this.setAggressive(false);
        // 移除攻击力加成
        AttributeInstance attr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            attr.removeModifier(HATRED_ATTACK_BOOST_ID);
        }
        this.removeEffect(MobEffects.DAMAGE_BOOST);
    }

    /**
     * 是否有活跃的仇恨目标
     */
    private boolean hasHatredTarget() {
        return this.hatredTarget != null && this.hatredTarget.isAlive() && this.hatredCooldown > 0;
    }

    /**
     * 触发萝莉防狼警报：播放警报声音，并使周围所有Neko对攻击者产生攻击欲望
     */
    public void triggerLoliAlarm(Player attacker) {
        if (!this.level().isClientSide) {
            // 播放警报声音
            this.level().playSound(
                    null,
                    this.blockPosition(),
                    ToNekoSoundEvents.NEKO_ALARM,
                    SoundSource.HOSTILE,
                    1.0F,
                    1.0F
            );

            // 使周围所有Neko对攻击者产生攻击欲望（20格范围）
            List<NekoEntity> nearbyNekos = this.level().getEntitiesOfClass(NekoEntity.class,
                    this.getBoundingBox().inflate(20), LivingEntity::isAlive);
            boolean hasFightingNeko = false;
            for (NekoEntity neko : nearbyNekos) {
                if (neko.hasOwner(attacker.getUUID())) continue; // 不攻击自己的主人
                if (neko instanceof FightingNekoEntity) hasFightingNeko = true;
                // 使用通用仇恨系统设置追击
                neko.setLastHurtByMob(attacker);
                neko.setHatredTarget(attacker, HATRED_DEFAULT_DURATION);
                // 立即发动一次攻击，确保即时反馈
                neko.equipBestMeleeWeapon();
                neko.doHurtTarget(attacker);
            }
            // 附近有FightingNeko时，召唤一道纯视觉闪电劈向玩家，伤害仅对玩家生效
            if (hasFightingNeko) {
                LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, this.level());
                lightning.setPos(attacker.getX(), attacker.getY(), attacker.getZ());
                lightning.setVisualOnly(true);
                this.level().addFreshEntity(lightning);
                attacker.hurt(this.damageSources().lightningBolt(), 5.0f);
            }
        }
    }

    /**
     * 判断物品是否为武器（剑、斧、远程武器等）
     */
    private boolean isWeapon(ItemStack stack) {
        return stack.is(net.minecraft.tags.ItemTags.SWORDS)
                || stack.is(net.minecraft.tags.ItemTags.AXES)
                || stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT);
    }

    /**
     * 在背包中寻找攻击伤害最高的近战武器并切换到该武器
     */
    protected void equipBestMeleeWeapon() {
        int bestSlot = -1;
        double bestDamage = 0.0;

        for (int i = 0; i < this.inventory.items.size(); i++) {
            ItemStack stack = this.inventory.items.get(i);
            if (!stack.isEmpty()) {
                double damage = getMeleeWeaponDamage(stack);
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot >= 0) {
            this.inventory.selected = bestSlot;
            this.setItemSlot(EquipmentSlot.MAINHAND, this.inventory.items.get(bestSlot));
        }
    }

    /**
     * 计算物品作为近战武器的攻击伤害，非近战武器返回0
     */
    protected double getMeleeWeaponDamage(ItemStack stack) {
        if (!stack.is(net.minecraft.tags.ItemTags.SWORDS)
                && !stack.is(net.minecraft.tags.ItemTags.AXES)
                && !stack.is(Items.TRIDENT)
                && !stack.is(Items.MACE)) {
            return 0.0;
        }
        double[] damage = {0.0};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
                damage[0] += modifier.amount();
            }
        });
        return damage[0];
    }

    public boolean eatOrStoreFood(ItemStack stack){
        if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) {
            // 不处理
            return false;
        }
        FoodProperties food = stack.getItem().components().get(DataComponents.FOOD);
        if (food != null){
            if(this.getHealth() < this.getMaxHealth() || !food.effects().isEmpty()){
                this.heal(food.nutrition());
                this.eat(this.level(), stack);
            }else {
                if (this.getInventory().isFull()) return false;
                this.getInventory().add(stack.copy());
            }
            return true;
        }
        return false;
    }



    public void hurtByPlayer(Player player){
        if (this.isAlive()) {
            sendHurtMessageToPlayer(player);
        }
    }

    public void sendHurtMessageToPlayer(Player player){
        if (player instanceof ServerPlayer) {
            var moe = this.getMoeTags();
            var name = this.getName();
            if (moe.contains("yandere")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.yandere",15,name));
            }else if (moe.contains("chunibyo")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.chunibyo",5,name));
            }else if (moe.contains("mesugaki")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.mesugaki",14,name));
            }else if (moe.contains("tsundere")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.tsundere",5,name));
            } else if (moe.contains("tennen_boke")) {
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.tennen_boke",5,name));
            }else if (moe.contains("shoakuma")) {
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.shoakuma",14,name));
            }else if (moe.contains("haraguro")) {
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.haraguro",13,name));
            }else if (moe.contains("narenareshi")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.narenareshi",15,name));
            }else if (moe.contains("shizukana")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.shizukana",15,name));
            }else if (moe.contains("gentleness")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.gentleness",15,name));
            }else if (moe.contains("baka")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.baka",13,name));
            }else if (moe.contains("dojikko")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.dojikko",12,name));
            }else if (moe.contains("yowaki")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.yowaki",14,name));
            }else if (moe.contains("paranoia")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.paranoia",15,name));
            }else if (moe.contains("yuri")){
                player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.yuri",12,name));
            }
            else {
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

    /**
     * 是否因萌属性（yowaki/paranoia）而逃离陌生人。
     * 子类可重写以定制行为（如诺艾尔是陪伴型，不应逃离）。
     */
    public boolean shouldFleeFromStrangers() {
        return this.getMoeTags().contains("yowaki") || this.getMoeTags().contains("paranoia");
    }

    @Override
    public int getNekoAge() {
        return this.getAge();
    }

    @Override
    public void setNekoAge(int age) {
        this.setAge(age);
    }

    public static final EntityDataAccessor<Float> NEKO_LEVEL_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.FLOAT);
    @Override
    public float getNekoLevel() {
        if (this.level() != null && this.level().isClientSide()) {
            return entityData.get(NEKO_LEVEL_ID);
        }
        return (float) NekoLevelRegistry.computeTotal(this);
    }

    @Override
    @Deprecated
    public void setNekoLevel(float nekoLevel) {
        // no-op — retained for binary compatibility only
    }

    @Override
    public CompoundTag getNekoLevelFactorData() {
        return nekoLevelFactorData;
    }

    @Override
    public void setNekoLevelFactorData(CompoundTag data) {
        this.nekoLevelFactorData = data;
    }

    private Map<UUID,Owner> owners = new HashMap<>();
    @Override
    public Map<UUID, Owner> getOwners() {
        return owners;
    }

    private List<BlockedWord> blockedWords = new ArrayList<>();
    @Override
    public List<BlockedWord> getBlockedWords() {
        return blockedWords;
    }

    public static final EntityDataAccessor<String> NICKNAME_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.STRING);
    @Override
    public @NotNull String getNickName() {
        return entityData.get(NICKNAME_ID);
    }
    @Override
    public void setNickName(String name) {
        this.entityData.set(NICKNAME_ID, name != null ? name : "");
    }

    private List<Quirk> quirks = new ArrayList<>();
    @Override
    public List<Quirk> getQuirks() {
        return quirks;
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

    // ------------------------------- 遗传系统相关 -------------------------------

    @Override
    public Genome getGenome() { return this.genome; }

    @Override
    public void setGenome(Genome genome) { this.genome = genome; }

    @Override
    public CompoundTag getGeneticData() { return this.geneticData; }

    @Override
    public List<ExpressedTrait> getActiveTraits() { return this.activeTraits; }

    @Override
    public List<Goal> getActiveGeneticGoals() { return this.activeGeneticGoals; }

    @Override
    public void expressTraits() {
        if (!this.level().isClientSide) {
            this.genome.express(this);

            // 同步胸部大小缩放值到客户端
            float chestScale = this.geneticData.contains("chest_scale", CompoundTag.TAG_FLOAT)
                    ? this.geneticData.getFloat("chest_scale")
                    : 1.0f;
            this.entityData.set(CHEST_SCALE_ID, chestScale);

            // 同步给客户端
            this.setSkin(this.getSkin());
            this.setMoeTags(this.getMoeTags());
        }
    }

    public float getChestScale() {
        return this.entityData.get(CHEST_SCALE_ID);
    }

    @Override
    public double getNekoAgeScale() {
        return this.entityData.get(AGE_SCALE_ID);
    }

    private double computeAgeScale() {
        int age = this.getNekoAge();
        int maxAge = this.getMaxAge();
        if (age >= 0) return 1.0;
        return 0.3 + 0.7 * (1.0 + (double) age / maxAge);
    }

    // 初始生成时的随机基因分配
    @Override
    public @NotNull SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        // 对所有非繁殖的生成方式分配基因（覆盖刷怪蛋、SPAWNER、TRIGGERED、指令等）
        // 繁殖走的 mate()→spawnChildFromBreeding() 流程已单独处理基因
        if (reason != MobSpawnType.BREEDING) {
            // 生成两套随机配子并结合，模拟”野生猫娘基因库”
            Gamete gamete1 = Genome.generateFallbackGamete(this.random, ToNekoLocus.NEKO_KARYOTYPE);
            Gamete gamete2 = Genome.generateFallbackGamete(this.random, ToNekoLocus.NEKO_KARYOTYPE);
            this.setGenome(Genome.combine(gamete1, gamete2, ToNekoLocus.NEKO_KARYOTYPE));

            // 初始化名字等基础信息
            if (!this.hasCustomName()) {
                this.setCustomName(Component.literal(NekoNameRegistry.getRandomName()));
            }
            this.expressTraits();
        }
        // 随机化皮肤、体型、速度、萌属性等（对所有生成方式生效）
        this.randomize();
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }



    public static AttributeSupplier.Builder createNekoAttributes(){
        return createMobAttributes().add(Attributes.ATTACK_DAMAGE).add(Attributes.ATTACK_SPEED).add(ToNekoAttributes.NEKO_DEGREE).add(ToNekoAttributes.MAX_NEKO_ENERGY);
    }

}

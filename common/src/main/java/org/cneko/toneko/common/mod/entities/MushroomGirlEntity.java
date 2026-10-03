package org.cneko.toneko.common.mod.entities;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.cneko.toneko.common.mod.ai.PromptRegistry;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.api.MushroomBedRest;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.mod.entities.ai.BehaviorPriority;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.*;

/** A nocturnal fungus person. The shared Neko framework supplies conversations and inventory only. */
public class MushroomGirlEntity extends NekoEntity {
    public static final float BODY_SCALE = 0.7f;
    public static final TagKey<Item> COFFEE = TagKey.create(Registries.ITEM, id("coffee_beans"));
    public static final TagKey<Biome> HABITATS = TagKey.create(Registries.BIOME, id("mushroom_girl_habitats"));
    public static final List<String> NAMES = List.of("ziye_bai", "yelu_zi", "yueai_bai", "muyu_ling",
            "xingtai_mian", "qinglu_wan", "zixia_shuang", "baiwu_yao", "yuyue_jin", "mushuang_yin");
    private static final Component FIXED_NAME = Component.translatable("name.toneko.mushroom_girl.ziye_bai");
    private static final EntityDataAccessor<Boolean> NAME_FIXED = SynchedEntityData.defineId(MushroomGirlEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RESTING = SynchedEntityData.defineId(MushroomGirlEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DROWSY = SynchedEntityData.defineId(MushroomGirlEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STAYING = SynchedEntityData.defineId(MushroomGirlEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> FAMILIARITY = SynchedEntityData.defineId(MushroomGirlEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> AFFECTION = SynchedEntityData.defineId(MushroomGirlEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> COMPANION = SynchedEntityData.defineId(MushroomGirlEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> REACTION = SynchedEntityData.defineId(MushroomGirlEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> EMOTION = SynchedEntityData.defineId(MushroomGirlEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> GROWTH_XP = SynchedEntityData.defineId(MushroomGirlEntity.class, EntityDataSerializers.INT);
    public static final byte NONE = 0, CARRIED = 1, SITTING_ON_PLAYER = 2, LYING_ON_PLAYER = 3;
    public static final byte CALM = 0, COWERING = 1, HIDING = 2, CURIOUS = 3, SEEKING_REFUGE = 4;
    public static final byte NEUTRAL = 0, ADMIRING = 1, DISPLEASED = 2, BLUSHING = 3;
    private static final Identifier FATIGUE = id("mushroom_fatigue");
    private final Map<UUID, Integer> familiarity = new HashMap<>();
    private final Map<UUID, Integer> affection = new HashMap<>();
    private final Map<UUID, Long> nextInteraction = new HashMap<>();
    private UUID following;
    private int wakeTicks;
    private int coffeeTicks;
    private int birthCooldown;
    private int defenseCooldown;
    private int dryTicks;
    private boolean shaded = true;
    private boolean moist = true;
    private int clientSpeechTicks;
    private int dialogueCooldown = 100;
    private int companionCooldown;
    private UUID dismissedPlayer;
    private int lingerCooldown;
    private UUID lastGreetedPlayer;
    private LivingEntity threat;
    private int attackerUntil;
    private final Map<UUID, ObservedThreat> observedThreats = new HashMap<>();
    private final Map<UUID, Integer> unattendedTicks = new HashMap<>();
    private UUID admiredPlayer;
    private int admirationTicks, gazeTicks, blushTicks;
    private boolean bedSleepVisit;
    private int regenerationTicks, regenerationDelay, companionshipTicks, combatExperienceCooldown;
    private Component originalName;

    private record ObservedThreat(LivingEntity entity, int lastSeen) { }

    public MushroomGirlEntity(EntityType<? extends NekoEntity> type, Level level) {
        super(type, level);
        setCustomName(Component.translatable("name.toneko.mushroom_girl." + NAMES.get(random.nextInt(NAMES.size()))));
        if (!level.isClientSide()) updateConfiguredName();
        setNekoEnergy(getMaxNekoEnergy());
        refreshDimensions();
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("toneko", path); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NAME_FIXED, false);
        builder.define(RESTING, false);
        builder.define(DROWSY, false);
        builder.define(STAYING, false);
        builder.define(FAMILIARITY, "");
        builder.define(AFFECTION, "");
        builder.define(COMPANION, NONE);
        builder.define(REACTION, CALM);
        builder.define(EMOTION, NEUTRAL);
        builder.define(GROWTH_XP, 0);
    }

    // Keep the original component, including custom name tags, so this temporary lock is reversible.
    void updateConfiguredName() {
        boolean fixed = ConfigUtil.isMushroomNameFixed();
        if (fixed && !FIXED_NAME.equals(getCustomName())) {
            originalName = getCustomName();
            setCustomName(FIXED_NAME.copy());
        } else if (!fixed && originalName != null) {
            setCustomName(originalName);
            originalName = null;
        }
        entityData.set(NAME_FIXED, fixed);
    }

    @Override public String getNickName() {
        return entityData.get(NAME_FIXED) ? "" : super.getNickName();
    }

    private void sendInteractionMessage(Player player, Component message, boolean overlay) {
        if (!ConfigUtil.isMushroomMessagesEnabled()) return;
        if (overlay) player.sendOverlayMessage(message);
        else player.sendSystemMessage(message);
    }

    public boolean isResting() { return entityData.get(RESTING); }
    public boolean isDrowsy() { return entityData.get(DROWSY); }
    public boolean isStaying() { return entityData.get(STAYING); }
    public byte getCompanionPose() { return entityData.get(COMPANION); }
    public byte getReaction() { return entityData.get(REACTION); }
    public byte getEmotion() { return entityData.get(EMOTION); }
    public int getGrowthExperience() { return entityData.get(GROWTH_XP); }
    public int getMushroomLevel() { return MushroomRules.levelFromXp(getGrowthExperience()); }
    public int getLevelExperience() { return getGrowthExperience() - MushroomRules.xpAtLevel(getMushroomLevel()); }
    public float getSporeDamage() { return MushroomRules.sporeDamage(getMushroomLevel()); }
    @Override public float getNekoLevel() { return getMushroomLevel(); }

    @Override public void updateNekoLevelModifiers() {
        // Use the framework's existing modifier ids so old health/age bonuses are replaced, never stacked.
        INeko.applyModifier(getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_MODIFIER_ID,
                MushroomRules.maxHealth(getMushroomLevel()) - MushroomRules.BASE_MAX_HEALTH);
        INeko.applyModifier(getAttribute(Attributes.SCALE), AGE_SCALE_MODIFIER_ID, getNekoAgeScale() - 1,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    @Override protected float getPassiveHealAmount() { return 0; }
    public boolean isCowering() { return getReaction() == COWERING; }
    public boolean isHiding() { return getReaction() == HIDING; }
    public boolean isInDistress() { return threat != null || isCowering() || isHiding() || getReaction() == SEEKING_REFUGE; }

    private void setReaction(byte reaction) {
        if (getReaction() == reaction) return;
        entityData.set(REACTION, reaction);
        refreshDimensions();
    }

    public boolean isWinkingAtSleepingPlayer() {
        return getCompanionPose() == SITTING_ON_PLAYER && getVehicle() instanceof Player player && player.isSleeping();
    }

    public static boolean isPlayerLying(Player player) {
        Pose pose = player.getPose();
        return pose == Pose.SLEEPING || (pose == Pose.SWIMMING && !player.isInWater()
                && EntityPoseManager.getNullablePose(player) == Pose.SWIMMING);
    }

    public void handleCompanionAction(ServerPlayer player, String action) {
        if (action.equals("get_down")) {
            if (getVehicle() == player) { noteAttention(player); stopRiding(); say(player, "get_down"); }
            return;
        }
        byte mode = switch (action) {
            case "carry" -> CARRIED;
            case "sit_on_player" -> SITTING_ON_PLAYER;
            case "lie_on_player" -> LYING_ON_PLAYER;
            default -> NONE;
        };
        if (mode == NONE) return;
        noteAttention(player);
        if (mode != CARRIED && !isPlayerLying(player)) {
            sendInteractionMessage(player, Component.translatable("message.toneko.mushroom_girl.lie_first"), true);
            return;
        }
        if (!requireFamiliarity(player, MushroomRules.companionFamiliarity(mode == CARRIED, MushroomBedRest.bed(player).isPresent()))) return;
        if (getVehicle() == player) { setCompanionPose(mode); syncCompanionTo(player); return; }
        if (!joinPlayer(player, mode)) sendInteractionMessage(player, Component.translatable("message.toneko.mushroom_girl.companion_unavailable"), true);
    }

    private boolean joinPlayer(ServerPlayer player, byte mode) {
        boolean safe = isAlive() && player.isAlive() && !player.isSpectator() && !player.isPassenger()
                && !player.isInWater() && !player.isOnFire() && !isInWater() && !isOnFire() && !isInDistress()
                && !MushroomRules.lowHealth(getHealth(), getMaxHealth());
        if (isPassenger() || distanceToSqr(player) > 9 || !hasLineOfSight(player)
                || !MushroomRules.canJoinPlayer(getFamiliarity(player.getUUID()), mode == CARRIED,
                isPlayerLying(player), MushroomBedRest.bed(player).isPresent(), safe, !player.getPassengers().isEmpty())) return false;
        wakeUp();
        following = null;
        entityData.set(STAYING, false);
        // Mounting explicitly ends every movement intent, including the highest-priority approach.
        getNekoBrain().reset();
        getNavigation().stop();
        setCompanionPose(mode);
        bedSleepVisit = mode == SITTING_ON_PLAYER && player.isSleeping() && MushroomBedRest.bed(player).isPresent();
        if (!startRiding(player, true, true)) { setCompanionPose(NONE); return false; }
        player.positionRider(this);
        alignCompanionTo(player);
        syncCompanionTo(player);
        setPersistenceRequired();
        say(player, mode == CARRIED ? "carry" : mode == SITTING_ON_PLAYER ? "sit" : "lie");
        return true;
    }

    private void setCompanionPose(byte pose) { entityData.set(COMPANION, pose); refreshDimensions(); }

    public ClientboundSetEntityDataPacket companionPosePacket() {
        // Include default NONE explicitly when dismounting, so the carrier clears its old pose too.
        return new ClientboundSetEntityDataPacket(getId(), List.of(
                SynchedEntityData.DataValue.create(COMPANION, getCompanionPose()),
                SynchedEntityData.DataValue.create(RESTING, isResting())));
    }

    protected void syncCompanionTo(ServerPlayer player) {
        // The vanilla vehicle tracker sends passengers to observers, not to the vehicle's own client.
        player.connection.send(companionPosePacket());
        player.connection.send(new ClientboundSetPassengersPacket(player));
    }

    @Override public void stopRiding() {
        Entity oldVehicle = getVehicle();
        super.stopRiding();
        if (!level().isClientSide()) {
            setCompanionPose(NONE);
            bedSleepVisit = false;
            gazeTicks = blushTicks = 0;
            companionCooldown = MushroomRules.COMPANION_COOLDOWN;
            lingerCooldown = MushroomRules.COMPANION_COOLDOWN;
            if (oldVehicle instanceof ServerPlayer player) {
                dismissedPlayer = player.getUUID();
                syncCompanionTo(player);
            }
        }
    }

    @Override public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        if (!(vehicle instanceof Player player) || getCompanionPose() == NONE) return super.getVehicleAttachmentPoint(vehicle);
        boolean carried = getCompanionPose() == CARRIED;
        var bedDirection = carried ? null : MushroomBedRest.direction(player);
        if (bedDirection != null) {
            Vec3 offset = new Vec3(-bedDirection.getStepX() * 0.9, 0.22, -bedDirection.getStepZ() * 0.9).scale(player.getScale());
            return player.getPassengerRidingPosition(this).subtract(player.position().add(offset));
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        // A ground lie keeps the player's camera at its foot-origin. Never seat inside that viewpoint;
        // leave extra room for the cap and the horizontal body's head when lying across the player.
        double back = carried ? 0.32 : getCompanionPose() == LYING_ON_PLAYER ? -1.45 : -0.9;
        double side = carried ? 0 : 0.3;
        Vec3 offset = new Vec3(Math.sin(yaw) * back + Math.cos(yaw) * side, carried ? 0.55 : 0.22,
                -Math.cos(yaw) * back + Math.sin(yaw) * side).scale(player.getScale());
        return player.getPassengerRidingPosition(this).subtract(player.position().add(offset));
    }

    private void tickCompanion() {
        if (!(getVehicle() instanceof ServerPlayer player)) {
            if (getCompanionPose() != NONE) setCompanionPose(NONE);
            return;
        }
        if (getCompanionPose() == NONE) setCompanionPose(CARRIED); // Restore vanilla passenger links.
        if (getCompanionPose() == SITTING_ON_PLAYER && player.isSleeping() && MushroomBedRest.bed(player).isPresent()) bedSleepVisit = true;
        if (!player.isAlive() || player.isRemoved() || player.isSpectator() || player.isPassenger()
                || player.isInWater() || player.isOnFire() || isInDistress() || MushroomRules.lowHealth(getHealth(), getMaxHealth())
                || (getCompanionPose() != CARRIED && !isPlayerLying(player))) {
            stopRiding();
            // Standing up or an unsafe environment ends this visit naturally. The long
            // cooldown is for manually asking her to get down, not for the next bedtime.
            companionCooldown = 0;
            dismissedPlayer = null;
            lingerCooldown = 0;
            return;
        }
    }

    private void alignCompanionTo(Player player) {
        var bedDirection = getCompanionPose() == CARRIED ? null : MushroomBedRest.direction(player);
        float yaw = bedDirection == null ? player.getYRot() + (getCompanionPose() == CARRIED ? 0 : 180) : bedDirection.toYRot();
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        if (bedDirection != null) {
            double height = getEyeY() - player.getEyeY();
            setXRot(Mth.clamp((float) Math.toDegrees(Math.atan2(height, 0.9 * player.getScale())), 0, 35));
        } else setXRot(0);
    }

    private void say(ServerPlayer player, String category) {
        if (!ConfigUtil.isMushroomMessagesEnabled() || LanguageUtil.LANG == null || !player.isAlive()) return;
        String key = "dialogue.toneko.mushroom_girl." + category + "." + random.nextInt(3);
        Messaging.sendNekoChat(player, this, LanguageUtil.translatable(key));
        dialogueCooldown = 900 + random.nextInt(900);
    }

    private ServerPlayer nearbyListener(double range) {
        ServerPlayer closest = null;
        double distance = range * range;
        for (Player player : level().players()) {
            double next = distanceToSqr(player);
            if (player instanceof ServerPlayer sp && player.isAlive() && !player.isSpectator()
                    && next < distance && hasLineOfSight(player)) { closest = sp; distance = next; }
        }
        return closest;
    }

    @Override protected void tickProactiveSpeech() {
        if (!ConfigUtil.isMushroomMessagesEnabled() || dialogueCooldown > 0 || !isAlive() || isInDistress()) return;
        ServerPlayer player = nearbyListener(isResting() ? 3 : 8);
        if (player == null) { lastGreetedPlayer = null; return; }
        String category;
        if (isResting()) category = "sleep";
        else if (!player.getUUID().equals(lastGreetedPlayer)) {
            lastGreetedPlayer = player.getUUID();
            category = hasOwner(player.getUUID()) ? "greet_friend" : "greet_stranger";
            if (!isPassenger()) playExpressAnim(hasOwner(player.getUUID()) ? "wave" : "shy");
        } else if (getCompanionPose() == CARRIED) category = "carry";
        else if (getCompanionPose() == SITTING_ON_PLAYER) category = "sit";
        else if (getCompanionPose() == LYING_ON_PLAYER) category = "lie";
        else if (isDrowsy()) { category = "drowsy"; playExpressAnim("yawn"); }
        else if (level().isRainingAt(blockPosition())) category = "rain";
        else if (!moist || !shaded) category = "shelter";
        else if (MushroomRules.isDay(level().getOverworldClockTime()) && coffeeTicks > 0) category = "coffee";
        else if (getAffection(player.getUUID()) >= MushroomRules.LINGER_AFFECTION) category = "affection";
        else category = hasOwner(player.getUUID()) ? "night" : "greet_stranger";
        say(player, category);
    }
    @Override public boolean isNeko() { return false; }
    @Override public boolean supportsSexualBreeding() { return false; }
    @Override public boolean canMate(INeko other) { return false; }
    @Override public NekoEntity getBreedOffspring(ServerLevel level, INeko other) { return null; }
    public boolean allowSocialAction(Player player, String action) {
        if (Set.of("purr", "groom", "sunbathe", "mate", "allow_mate", "accept_owner", "remove_owner",
                "change_affection", "lie", "sniff_legwear").contains(action)) return false;
        if (action.equals("hug") || action.equals("nuzzle")) return requireFamiliarity(player, MushroomRules.HUG_FAMILIARITY);
        if (action.equals("pet_request")) return requireFamiliarity(player, MushroomRules.PET_FAMILIARITY);
        if (action.equals("move_to_player") || action.equals("follow")) return requireFamiliarity(player, MushroomRules.FRIEND_FAMILIARITY);
        return true;
    }

    @Override public String getDefaultSkin() { return "purwhite"; }
    @Override public String getSkin() { return "purwhite"; }
    @Override public String getRandomSkin() { return "purwhite"; }
    @Override public boolean shouldFleeFromStrangers() { return false; }
    @Override public void expressTraits() { /* Fungus physiology has no feline gene expression. */ }
    @Override public void randomize() { setNekoEnergy(getMaxNekoEnergy()); }
    @Override public boolean isFavoriteItem(ItemStack stack) { return stack.is(COFFEE); }
    @Override public boolean isLikedItem(ItemStack stack) {
        return stack.is(COFFEE) || stack.is(Items.BROWN_MUSHROOM) || stack.is(Items.RED_MUSHROOM)
                || stack.is(Items.MUSHROOM_STEW) || stack.is(Items.MOSS_BLOCK);
    }
    @Override public boolean hasOwner(UUID uuid) { return getFamiliarity(uuid) >= MushroomRules.FRIEND_FAMILIARITY; }

    public int getFamiliarity(UUID uuid) {
        if (!level().isClientSide()) return familiarity.getOrDefault(uuid, 0);
        return syncedScore(FAMILIARITY, uuid);
    }

    public int getAffection(UUID uuid) {
        if (getFamiliarity(uuid) < MushroomRules.MAX_FAMILIARITY) return 0;
        return level().isClientSide() ? syncedScore(AFFECTION, uuid) : affection.getOrDefault(uuid, 0);
    }

    private int syncedScore(EntityDataAccessor<String> accessor, UUID uuid) {
        String prefix = uuid + ":";
        for (String value : entityData.get(accessor).split(",")) {
            if (value.startsWith(prefix)) {
                try { return Math.clamp(Integer.parseInt(value.substring(prefix.length())), 0, 100); }
                catch (NumberFormatException ignored) { return 0; }
            }
        }
        return 0;
    }

    public void recordInteraction(Player player, int amount) {
        if (level().isClientSide() || amount == 0) return;
        if (amount > 0) noteAttention(player);
        UUID uuid = player.getUUID();
        long now = level().getGameTime();
        if (amount > 0 && now < nextInteraction.getOrDefault(uuid, Long.MIN_VALUE)) return;
        if (!familiarity.containsKey(uuid) && familiarity.size() >= 256) return;
        int old = getFamiliarity(uuid);
        var relationship = MushroomRules.relationship(old, getAffection(uuid), amount);
        int score = relationship.familiarity();
        familiarity.put(uuid, score);
        if (relationship.affection() > 0) affection.put(uuid, relationship.affection());
        else affection.remove(uuid);
        nextInteraction.put(uuid, now + MushroomRules.INTERACTION_COOLDOWN);
        if (score >= MushroomRules.FRIEND_FAMILIARITY && !getOwners().containsKey(uuid)) {
            addOwner(uuid, new Owner(new ArrayList<>(), score));
        }
        if (score < MushroomRules.FRIEND_FAMILIARITY) getOwners().remove(uuid);
        syncFamiliarity();
        setPersistenceRequired();
        if (old < MushroomRules.FRIEND_FAMILIARITY && score >= MushroomRules.FRIEND_FAMILIARITY) {
            sendInteractionMessage(player, Component.translatable("message.toneko.mushroom_girl.friend", getName()), false);
            if (player instanceof ServerPlayer sp) say(sp, "greet_friend");
        }
        if (old < MushroomRules.MAX_FAMILIARITY && score == MushroomRules.MAX_FAMILIARITY) {
            sendInteractionMessage(player, Component.translatable("message.toneko.mushroom_girl.affection_unlocked", getName()), false);
        }
    }

    private void syncFamiliarity() {
        entityData.set(FAMILIARITY, familiarity.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ":" + e.getValue()).collect(java.util.stream.Collectors.joining(",")));
        entityData.set(AFFECTION, affection.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ":" + e.getValue()).collect(java.util.stream.Collectors.joining(",")));
    }

    public boolean requireFamiliarity(Player player, int threshold) {
        if (getFamiliarity(player.getUUID()) >= threshold) return true;
        if (!level().isClientSide()) sendInteractionMessage(player, Component.translatable("message.toneko.mushroom_girl.shy", getName()), true);
        return false;
    }

    private void wakeUp() {
        wakeTicks = Math.max(wakeTicks, MushroomRules.WAKE_TICKS);
        setResting(false);
    }

    public void toggleStay(Player player) {
        if (!requireFamiliarity(player, MushroomRules.FRIEND_FAMILIARITY)) return;
        noteAttention(player);
        following = null;
        entityData.set(STAYING, !isStaying());
        getNekoBrain().stopMoving(this);
        getNavigation().stop();
        sendInteractionMessage(player, Component.translatable(isStaying()
                ? "message.toneko.mushroom_girl.stay" : "message.toneko.mushroom_girl.roam", getName()), true);
    }

    @Override public void followOwner(Player player, double maxDistance, double speed) {
        if (!requireFamiliarity(player, MushroomRules.FRIEND_FAMILIARITY)) return;
        noteAttention(player);
        following = player.getUUID();
        entityData.set(STAYING, false);
        wakeUp();
    }

    public void stopFollowing() {
        following = null;
        lingerCooldown = MushroomRules.COMPANION_COOLDOWN;
        getNekoBrain().stopMoving(this);
        getNavigation().stop();
    }

    @Override public void playHugEffect() {
        playExpressAnim("hug");
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + getScale(), getZ(), 7, 0.3, 0.3, 0.3, 0.02);
            server.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    getSoundSource(), 0.5f, 1.2f);
        }
    }

    @Override public void playExpressAnim(String animation) {
        // Keep the limbs holding the player; the independent face still blinks and speaks.
        if (!isPassenger() && !isInDistress()) super.playExpressAnim(animation);
    }

    @Override public void openInteractiveMenu(ServerPlayer player) {
        recordInteraction(player, 4);
        if (!isResting()) playExpressAnim(hasOwner(player.getUUID()) ? "wave" : "shy");
        super.openInteractiveMenu(player);
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (stack.is(COFFEE) || isLikedItem(stack)) {
            giftItem(player, stack);
        } else if (stack.isEmpty()) {
            if (isResting()) {
                wakeUp();
                recordInteraction(player, 4);
                playExpressAnim("yawn");
                sendInteractionMessage(player, Component.translatable("message.toneko.mushroom_girl.wake", getName()), true);
                if (player instanceof ServerPlayer sp) say(sp, "drowsy");
            } else if (player.isShiftKeyDown()) {
                if (requireFamiliarity(player, MushroomRules.HUG_FAMILIARITY)) {
                    recordInteraction(player, 4);
                    playHugEffect();
                }
            } else {
                if (player instanceof ServerPlayer sp) openInteractiveMenu(sp);
                if (getFamiliarity(player.getUUID()) >= MushroomRules.PET_FAMILIARITY) {
                    playExpressAnim("happy_jump");
                    sendInteractionMessage(player, Component.translatable("message.toneko.mushroom_girl.pet", getName()), true);
                }
            }
        } else if (player instanceof ServerPlayer sp) {
            openInteractiveMenu(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override public boolean giftItem(Player player, ItemStack stack) {
        if (level().isClientSide() || stack.isEmpty() || !isLikedItem(stack)) return false;
        boolean coffee = stack.is(COFFEE);
        if (!player.isCreative()) stack.shrink(1);
        if (coffee) coffeeTicks = MushroomRules.COFFEE_TICKS;
        wakeUp();
        heal(coffee ? 1 : 2);
        recordInteraction(player, coffee ? 15 : 12);
        playExpressAnim(coffee ? "happy_jump" : "shy");
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.HEART,
                getX(), getY() + getScale(), getZ(), 5, 0.3, 0.3, 0.3, 0.02);
        sendInteractionMessage(player, Component.translatable(coffee
                ? "message.toneko.mushroom_girl.coffee" : "message.toneko.mushroom_girl.feed", getName()), true);
        if (player instanceof ServerPlayer sp) say(sp, coffee ? "coffee" : "feed");
        return true;
    }

    private void setResting(boolean resting) {
        if (isResting() == resting) return;
        entityData.set(RESTING, resting);
        refreshDimensions();
        if (resting) {
            getNekoBrain().stopMoving(this);
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
    }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (RESTING.equals(key) || COMPANION.equals(key) || REACTION.equals(key)) refreshDimensions();
    }

    // Shared age attributes already grow smoothly from 0.3 to 1; avoid vanilla's extra baby half-scale.
    @Override public float getAgeScale() { return 1f; }

    // Minecraft and GeckoLib both use this scale for dimensions, eye height and rendering.
    // Apply it after attributes so existing individuals and growing juveniles also shrink uniformly.
    @Override protected float sanitizeScale(float scale) {
        return super.sanitizeScale(scale) * BODY_SCALE;
    }

    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        if (isResting()) return EntityDimensions.scalable(0.9f, 0.45f).withEyeHeight(0.3f);
        if (isCowering() || isHiding()) return EntityDimensions.scalable(0.9f, 1.45f).withEyeHeight(0.95f);
        if (getCompanionPose() == LYING_ON_PLAYER) return EntityDimensions.scalable(1.4f, 0.55f).withEyeHeight(0.35f);
        if (getCompanionPose() != NONE) return EntityDimensions.scalable(0.9f, 1.5f).withEyeHeight(1.05f);
        return super.getDefaultDimensions(pose);
    }

    @Override public boolean canMove() {
        // Horizontal rest never freezes gravity, water, falling or knockback.
        return !isPassenger() && ((!isResting() && (!isStaying() || isInDistress()) && !isCowering() && !isHiding())
                || isInWater() || !onGround());
    }

    @Override public void tick() {
        if (level().isClientSide()) {
            if (clientSpeechTicks > 0) clientSpeechTicks--;
        } else {
            updateConfiguredName();
            if (wakeTicks > 0) wakeTicks--;
            if (coffeeTicks > 0) coffeeTicks--;
            if (birthCooldown > 0) birthCooldown--;
            if (defenseCooldown > 0) defenseCooldown--;
            if (dialogueCooldown > 0) dialogueCooldown--;
            if (companionCooldown > 0) companionCooldown--;
            if (lingerCooldown > 0) lingerCooldown--;
            tickThreats();
            tickCompanionCooldown();
            tickCompanion();
            tickEmotions();
            if (tickCount % 20 == 0) {
                shaded = isShaded(level(), blockPosition());
                moist = isMoist(level(), blockPosition());
                dryTicks = moist ? Math.max(0, dryTicks - 60) : Math.min(1200, dryTicks + 20);
                boolean drowsy = coffeeTicks <= 0 && (MushroomRules.isDay(level().getOverworldClockTime()) || dryTicks >= 300 || !shaded);
                entityData.set(DROWSY, drowsy);
                var speed = getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(FATIGUE);
                    if (drowsy) speed.addTransientModifier(new AttributeModifier(FATIGUE, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                setNekoEnergy(getMaxNekoEnergy() * (drowsy ? 0.35f : 0.95f));
            }
            boolean danger = isPassenger() || isInWater() || isOnFire() || !onGround() || defenseCooldown > 0
                    || threat != null || MushroomRules.lowHealth(getHealth(), getMaxHealth()) || isInDistress();
            setResting(MushroomRules.shouldRest(level().getOverworldClockTime(), wakeTicks, coffeeTicks, danger)
                    && (shaded || tickCount > 300));
        }
        super.tick();
        if (!level().isClientSide()) tickGrowth();
        // Apply in both worlds after vanilla rider ticking, so the client's facing agrees with the server.
        if (getCompanionPose() != NONE && getVehicle() instanceof Player player) alignCompanionTo(player);
    }

    void addGrowthExperience(int amount) {
        if (level().isClientSide() || !isAlive() || amount <= 0) return;
        int oldLevel = getMushroomLevel();
        int experience = MushroomRules.addExperience(getGrowthExperience(), amount);
        if (experience == getGrowthExperience()) return;
        entityData.set(GROWTH_XP, experience);
        entityData.set(NEKO_LEVEL_ID, (float)getMushroomLevel());
        setPersistenceRequired();
        if (getMushroomLevel() != oldLevel) {
            updateNekoLevelModifiers();
            if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getEyeY(), getZ(), 8, 0.25, 0.25, 0.25, 0.025);
        }
    }

    void onDefensiveSporeHit(LivingEntity victim) {
        // Only a real damaging hit on a hostile earns combat experience; firing and friendly sparring do not.
        if (combatExperienceCooldown <= 0 && (victim instanceof Enemy
                || victim instanceof Mob mob && mob.getTarget() == this)) {
            addGrowthExperience(1);
            combatExperienceCooldown = MushroomRules.COMBAT_XP_INTERVAL;
        }
    }

    void tickGrowth() {
        if (!isAlive()) return;
        if (combatExperienceCooldown > 0) combatExperienceCooldown--;
        if (regenerationDelay > 0) regenerationDelay--;
        if (regenerationDelay <= 0 && !isOnFire() && getHealth() < getMaxHealth()
                && (shaded || !MushroomRules.isDay(level().getOverworldClockTime()))) {
            if (++regenerationTicks >= MushroomRules.REGEN_INTERVAL) {
                regenerationTicks = 0;
                heal(MushroomRules.regenerationAmount(getMushroomLevel()));
            }
        } else regenerationTicks = 0;
        boolean company = !isInDistress() && !isOnFire() && level().players().stream().anyMatch(player ->
                player.isAlive() && !player.isSpectator() && !player.isInWater() && !player.isOnFire()
                && getFamiliarity(player.getUUID()) >= MushroomRules.FRIEND_FAMILIARITY
                && distanceToSqr(player) <= 36 && hasLineOfSight(player));
        if (company) {
            if (++companionshipTicks >= MushroomRules.COMPANION_XP_INTERVAL) {
                companionshipTicks = 0;
                addGrowthExperience(1);
            }
        } else companionshipTicks = 0;
    }

    private void tickCompanionCooldown() {
        Player player = dismissedPlayer == null ? null : level().getPlayerByUUID(dismissedPlayer);
        // Asking her down prevents returning during this rest, not at the next bedtime.
        if (companionCooldown <= 0 || player != null && !isPlayerLying(player)) {
            companionCooldown = 0;
            dismissedPlayer = null;
        }
    }

    void noteAttention(Player player) {
        if (level().isClientSide()) return;
        if (unattendedTicks.size() < 256 || unattendedTicks.containsKey(player.getUUID())) unattendedTicks.put(player.getUUID(), 0);
        if (getEmotion() == DISPLEASED) entityData.set(EMOTION, NEUTRAL);
    }

    private boolean threatening(LivingEntity entity) {
        return entity != this && entity.isAlive() && !entity.isRemoved()
                && (!(entity instanceof Player player) || !player.isCreative() && !player.isSpectator())
                && (entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == this
                    || entity == threat && tickCount < attackerUntil)
                && distanceToSqr(entity) <= MushroomRules.THREAT_RANGE * MushroomRules.THREAT_RANGE
                && hasLineOfSight(entity);
    }

    void tickThreats() {
        // Retain recently seen threats through their death ticks to attribute player arrows as well as melee.
        var iterator = observedThreats.entrySet().iterator();
        while (iterator.hasNext()) {
            var observed = iterator.next().getValue();
            LivingEntity entity = observed.entity();
            if (!entity.isAlive()) {
                DamageSource damage = entity.getLastDamageSource();
                if (tickCount - observed.lastSeen() <= 100 && damage != null
                        && damage.getEntity() instanceof ServerPlayer rescuer && rescuer.isAlive()
                        && !rescuer.isSpectator() && distanceToSqr(rescuer) <= 256) {
                    admiredPlayer = rescuer.getUUID();
                    admirationTicks = MushroomRules.ADMIRATION_TICKS;
                    recordInteraction(rescuer, 6);
                    getLookControl().setLookAt(rescuer, 30, 25);
                    say(rescuer, "admire");
                }
                iterator.remove();
            } else if (entity.isRemoved() || tickCount - observed.lastSeen() > 100) iterator.remove();
        }
        if (tickCount % 5 != 0 && (threat == null || threatening(threat))) return;
        LivingEntity attacker = threat;
        threat = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(MushroomRules.THREAT_RANGE), this::threatening)
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        // The last attacker may be a player, outside the normal hostile-mob selection.
        if (threat == null && attacker != null && threatening(attacker)) threat = attacker;
        if (threat != null) {
            if (observedThreats.size() < 16 || observedThreats.containsKey(threat.getUUID()))
                observedThreats.put(threat.getUUID(), new ObservedThreat(threat, tickCount));
            wakeUp();
        }
    }

    void tickEmotions() {
        if (admirationTicks > 0) admirationTicks--;
        if (blushTicks > 0) blushTicks--;
        boolean awakeVisit = bedSleepVisit && getCompanionPose() == SITTING_ON_PLAYER
                && getVehicle() instanceof Player player && !player.isSleeping()
                && MushroomBedRest.bed(player).isPresent();
        gazeTicks = MushroomRules.gazeTicks(gazeTicks, awakeVisit,
                awakeVisit && isLookingAtFace((Player) getVehicle()));
        if (gazeTicks > MushroomRules.BLUSH_GAZE_TICKS) blushTicks = MushroomRules.BLUSH_LINGER_TICKS;
        boolean neglected = false;
        for (Player player : level().players()) {
            UUID uuid = player.getUUID();
            if (getAffection(uuid) <= 20 || !player.isAlive() || player.isSpectator()
                    || player.isSleeping() || distanceToSqr(player) > 144 || !hasLineOfSight(player)) continue;
            int ticks = unattendedTicks.getOrDefault(uuid, 0);
            if (!isResting() && !isInDistress()) ticks = Math.min(MushroomRules.NEGLECT_TICKS, ticks + 1);
            unattendedTicks.put(uuid, ticks);
            if (MushroomRules.feelsNeglected(getAffection(uuid), ticks)) neglected = true;
        }
        entityData.set(EMOTION, isInDistress() ? NEUTRAL : blushTicks > 0 ? BLUSHING
                : admirationTicks > 0 ? ADMIRING : neglected ? DISPLEASED : NEUTRAL);
        if (getEmotion() == ADMIRING && admiredPlayer != null) {
            Player player = level().getPlayerByUUID(admiredPlayer);
            if (player != null) getLookControl().setLookAt(player, 30, 25);
        }
        if (getEmotion() == BLUSHING && tickCount % 8 == 0 && level() instanceof ServerLevel server) {
            // Sparse dust rises above the cap like a tiny puff of purple steam.
            for (int i = 0; i < 2; i++) server.sendParticles(MushroomSporeProjectile.PURPLE,
                    getX() + (random.nextDouble() - 0.5) * 0.12, getY() + 1.55 * getScale(),
                    getZ() + (random.nextDouble() - 0.5) * 0.12, 0, 0, 0.035, 0, 1);
        }
    }

    boolean isLookingAtFace(Player player) {
        var direction = MushroomBedRest.direction(player);
        // Match the existing bed camera's fixed yaw and 0.3-block mattress lift.
        Vec3 eye = player.getEyePosition().add(0, direction == null ? 0 : 0.3, 0);
        Vec3 look = direction == null ? player.getViewVector(1)
                : Vec3.directionFromRotation(player.getXRot(), direction.toYRot() - 180f);
        double scale = getScale();
        AABB face = new AABB(getX() - 0.32 * scale, getEyeY() - 0.22 * scale, getZ() - 0.32 * scale,
                getX() + 0.32 * scale, getEyeY() + 0.45 * scale, getZ() + 0.32 * scale);
        var hit = face.clip(eye, eye.add(look.scale(4)));
        return hit.isPresent() && level().clip(new ClipContext(eye, hit.get(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    void shootDefensiveSpore(ServerLevel server, LivingEntity target) {
        if (defenseCooldown > 0 || !target.isAlive() || !hasLineOfSight(target)) return;
        defenseCooldown = MushroomRules.DEFENSE_INTERVAL;
        var spore = new MushroomSporeProjectile(server, this, target);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(spore.position());
        spore.shoot(aim.x, aim.y + aim.horizontalDistance() * 0.015, aim.z, 0.65f, 2f);
        server.addFreshEntity(spore);
    }

    /** Client-local lip movement when an actual chat reply is displayed. */
    public void showSpeech(int characters) {
        if (level().isClientSide() && !isResting()) clientSpeechTicks = Math.clamp(characters * 2, 30, 120);
    }

    @Override protected void tickHatred() { /* Defense is a spore projectile, never feline melee. */ }
    @Override protected void setHatredTarget(LivingEntity target, int duration) { }
    @Override public void hurtByPlayer(Player player) { /* The spore defense handles harm and trust. */ }

    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean damaged = super.hurtServer(level, source, amount);
        if (!damaged) return false;
        regenerationTicks = 0;
        regenerationDelay = MushroomRules.REGEN_DAMAGE_DELAY;
        if (isPassenger()) stopRiding();
        wakeUp();
        if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            threat = attacker;
            attackerUntil = tickCount + 200;
            shootDefensiveSpore(level, attacker);
            if (attacker instanceof Player player) {
                nextInteraction.remove(player.getUUID());
                recordInteraction(player, -15);
                if (player.getUUID().equals(following)) following = null;
                if (player instanceof ServerPlayer sp) say(sp, "hurt");
            }
        }
        return true;
    }

    private void releaseSporeParticles(ServerLevel level, int count) {
        level.sendParticles(ParticleTypes.WITCH, getX(), getY() + (isResting() ? 0.3 : 1.7) * getScale(), getZ(),
                count, 0.5, 0.15, 0.5, 0.02);
    }

    @Override protected void spawnAmbientParticles() {
        if (!(level() instanceof ServerLevel server) || isResting() || isPassenger()) return;
        releaseSporeParticles(server, 5);
        if (tickCount % MushroomRules.SPORE_INTERVAL != 0) return;
        playExpressAnim("spores");
        if (!server.getGameRules().get(GameRules.MOB_GRIEFING) || !moist || !shaded) return;
        var mushroom = (random.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM).defaultBlockState();
        BlockPos mushroomSpot = null, childSpot = null;
        // Find suitable ground before rolling once. Extra candidates improve placement, not birth odds.
        for (int attempt = 0; attempt < MushroomRules.SPORE_POSITION_ATTEMPTS; attempt++) {
            BlockPos ground = blockPosition().offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos pos = ground.offset(0, dy, 0);
                if (!server.isLoaded(pos) || !server.isEmptyBlock(pos)
                        || !isMoist(server, pos) || !isShaded(server, pos)) continue;
                if (mushroomSpot == null && mushroom.canSurvive(server, pos)) mushroomSpot = pos;
                // A fungus person can stand on safe ground even where a vanilla mushroom needs darker light.
                if (childSpot == null && server.isEmptyBlock(pos.above())
                        && server.getBlockState(pos.below()).isFaceSturdy(server, pos.below(), net.minecraft.core.Direction.UP)) childSpot = pos;
            }
            if (mushroomSpot != null && childSpot != null) break;
        }
        if (childSpot != null && tickCount % MushroomRules.BIRTH_INTERVAL == 0) {
            int nearby = server.getEntitiesOfClass(MushroomGirlEntity.class, getBoundingBox().inflate(32)).size();
            if (MushroomRules.canProduceChild(!isNekoBaby(), true, true, nearby, birthCooldown, random.nextDouble())) {
                MushroomGirlEntity child = createSporeChild(server);
                child.setNekoBaby(true);
                child.birthCooldown = MushroomRules.BIRTH_COOLDOWN;
                child.moveTo(childSpot.getX() + 0.5, childSpot.getY(), childSpot.getZ() + 0.5, random.nextFloat() * 360, 0);
                if (server.noCollision(child) && server.addFreshEntity(child)) {
                    birthCooldown = MushroomRules.BIRTH_COOLDOWN;
                    growthFeedback(server, childSpot, "birth");
                }
            }
        }
        if (mushroomSpot != null && random.nextDouble() < MushroomRules.MUSHROOM_CHANCE) {
            int count = 0;
            for (BlockPos pos : BlockPos.betweenClosed(mushroomSpot.offset(-4, -1, -4), mushroomSpot.offset(4, 1, 4))) {
                if (server.getBlockState(pos).is(Blocks.BROWN_MUSHROOM) || server.getBlockState(pos).is(Blocks.RED_MUSHROOM)) count++;
            }
            if (count < 8 && server.setBlockAndUpdate(mushroomSpot, mushroom)) growthFeedback(server, mushroomSpot, "mushroom");
        }
    }

    protected MushroomGirlEntity createSporeChild(ServerLevel server) {
        return new MushroomGirlEntity(ToNekoEntities.MUSHROOM_GIRL, server);
    }

    private void growthFeedback(ServerLevel server, BlockPos pos, String category) {
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                15, 0.3, 0.3, 0.3, 0.03);
        ServerPlayer player = nearbyListener(12);
        if (player != null) say(player, category);
    }

    public static boolean isShaded(Level level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos) <= 12
                || !level.canSeeSky(pos) || !MushroomRules.isDay(level.getOverworldClockTime());
    }

    public static boolean isMoist(Level level, BlockPos pos) {
        if (level.isRainingAt(pos) || level.getBiome(pos).is(HABITATS)) return true;
        for (BlockPos water : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 0, 4))) {
            if (level.getFluidState(water).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    public static boolean canSpawn(EntityType<MushroomGirlEntity> type, ServerLevelAccessor level,
                                   EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) return true;
        return level.getBiome(pos).is(HABITATS) && isMoist(level.getLevel(), pos)
                && isShaded(level.getLevel(), pos) && random.nextInt(8) == 0;
    }

    @Override public void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TimidDefenseGoal());
        goalSelector.addGoal(2, new ComfortPlayerGoal());
        goalSelector.addGoal(3, new ShelterGoal());
        goalSelector.addGoal(4, new FollowFriendGoal());
        goalSelector.addGoal(5, new LingerWithFriendGoal());
        goalSelector.addGoal(6, new InspectHumanThingsGoal());
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return canMove() && !isResting() && !isStaying() && super.canUse(); }
            @Override public boolean canContinueToUse() { return canMove() && !isResting() && !isStaying() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6) {
            @Override public boolean canUse() { return !isPassenger() && !isResting() && super.canUse(); }
        });
    }

    class TimidDefenseGoal extends Goal {
        private BlockPos refuge;
        private int nextSearch;
        TimidDefenseGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            return !isPassenger() && !isInWater() && !isOnFire() && onGround()
                    && (threat != null || MushroomRules.lowHealth(getHealth(), getMaxHealth()));
        }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            wakeUp();
            stopTriggeredAnim("express", null);
            getNekoBrain().reset();
            getNavigation().stop();
            refuge = null;
            nextSearch = tickCount;
        }
        @Override public void tick() {
            if (MushroomRules.lowHealth(getHealth(), getMaxHealth())) {
                if (tickCount >= nextSearch || refuge != null && !safeRefuge(refuge)) {
                    refuge = findRefuge();
                    nextSearch = tickCount + 60;
                }
                if (refuge != null && refuge.distToCenterSqr(position().add(0, 0.5, 0)) > 0.8) {
                    setReaction(SEEKING_REFUGE);
                    getNekoBrain().submitMove(Vec3.atBottomCenterOf(refuge), 1.25, BehaviorPriority.CRITICAL, this);
                } else {
                    setReaction(HIDING);
                    getNekoBrain().stopMoving(this);
                    getNavigation().stop();
                }
            } else {
                setReaction(COWERING);
                getNekoBrain().stopMoving(this);
                getNavigation().stop();
            }
            if (threat != null) {
                getLookControl().setLookAt(threat, 25, 15);
                if (level() instanceof ServerLevel server) shootDefensiveSpore(server, threat);
            }
        }
        private BlockPos findRefuge() {
            record Candidate(BlockPos pos, double score) { }
            List<Candidate> candidates = new ArrayList<>();
            for (BlockPos candidate : BlockPos.betweenClosed(blockPosition().offset(-8, -2, -8), blockPosition().offset(8, 2, 8))) {
                if (!safeRefuge(candidate)) continue;
                int walls = cornerWalls(candidate);
                double next = walls * 100 - candidate.distToCenterSqr(position());
                if (isShaded(level(), candidate)) next += 10;
                if (threat != null) next += Math.min(100, candidate.distToCenterSqr(threat.position()));
                candidates.add(new Candidate(candidate.immutable(), next));
            }
            // Rank cheap cover checks first; pathfinding the entire surrounding floor would be expensive.
            candidates.sort(Comparator.comparingDouble(Candidate::score).reversed());
            for (Candidate candidate : candidates.stream().limit(32).toList()) {
                Path path = getNavigation().createPath(candidate.pos(), 0);
                if (path != null && path.canReach()) return candidate.pos();
            }
            return null;
        }
        private boolean safeRefuge(BlockPos pos) {
            if (!level().isLoaded(pos) || !level().isEmptyBlock(pos) || !level().isEmptyBlock(pos.above())
                    || !level().getFluidState(pos).isEmpty() || !level().getFluidState(pos.below()).isEmpty()
                    || !level().getBlockState(pos.below()).isFaceSturdy(level(), pos.below(), Direction.UP)
                    || level().getBlockState(pos.below()).is(Blocks.MAGMA_BLOCK)
                    || level().getBlockState(pos.below()).is(BlockTags.CAMPFIRES)) return false;
            AABB box = getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(position()));
            if (!level().noCollision(MushroomGirlEntity.this, box)) return false;
            for (ObservedThreat observed : observedThreats.values()) {
                var enemy = observed.entity();
                if (!enemy.isAlive()) continue;
                if (pos.distToCenterSqr(enemy.position()) < 16) return false;
                Vec3 eye = Vec3.atBottomCenterOf(pos).add(0, 0.7, 0);
                if (pos.distToCenterSqr(enemy.position()) < 100 && level().clip(new ClipContext(eye, enemy.getEyePosition(),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, MushroomGirlEntity.this)).getType() == HitResult.Type.MISS) return false;
            }
            return true;
        }
        @Override public void stop() {
            getNekoBrain().stopMoving(this);
            setReaction(CALM);
            refuge = null;
        }
    }

    int cornerWalls(BlockPos pos) {
        int walls = 0;
        boolean xWall = false, zWall = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (level().getBlockState(side).isFaceSturdy(level(), side, direction.getOpposite())
                    && level().getBlockState(side.above()).isFaceSturdy(level(), side.above(), direction.getOpposite())) {
                walls++;
                if (direction.getAxis() == Direction.Axis.X) xWall = true; else zWall = true;
            }
        }
        return xWall && zWall ? walls + 2 : walls;
    }

    class InspectHumanThingsGoal extends Goal {
        private Vec3 interest, approach;
        private ArmorStand armorStand;
        private int nextVisit, until, watched;
        InspectHumanThingsGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        private boolean available() {
            return canMove() && !isInDistress() && !isResting() && !isStaying() && following == null;
        }
        @Override public boolean canUse() {
            if (!available() || tickCount < nextVisit) return false;
            nextVisit = tickCount + 600 + random.nextInt(600);
            interest = approach = null;
            armorStand = level().getEntitiesOfClass(ArmorStand.class, getBoundingBox().inflate(8), Entity::isAlive)
                    .stream().filter(MushroomGirlEntity.this::hasLineOfSight)
                    .min(Comparator.comparingDouble(MushroomGirlEntity.this::distanceToSqr)).orElse(null);
            if (armorStand != null) interest = armorStand.position().add(0, 1, 0);
            else {
                double best = 64;
                for (BlockPos pos : BlockPos.betweenClosed(blockPosition().offset(-6, -2, -6), blockPosition().offset(6, 2, 6))) {
                    var block = level().getBlockState(pos);
                    if (!(block.is(Blocks.CRAFTING_TABLE) || block.is(Blocks.LOOM) || block.is(Blocks.CARTOGRAPHY_TABLE)
                            || block.is(Blocks.SMITHING_TABLE) || block.is(Blocks.STONECUTTER) || block.is(Blocks.ENCHANTING_TABLE))) continue;
                    Vec3 center = Vec3.atCenterOf(pos);
                    double distance = center.distanceToSqr(position());
                    if (distance < best && level().clip(new ClipContext(getEyePosition(), center,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, MushroomGirlEntity.this)).getBlockPos().equals(pos)) {
                        best = distance;
                        interest = center;
                    }
                }
            }
            if (interest == null) return false;
            BlockPos object = BlockPos.containing(interest);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos candidate = object.relative(direction, 2).atY(blockPosition().getY());
                if (!level().isEmptyBlock(candidate) || !level().isEmptyBlock(candidate.above())) continue;
                Path path = getNavigation().createPath(candidate, 0);
                if (path != null && path.canReach()) { approach = Vec3.atBottomCenterOf(candidate); return true; }
            }
            return false;
        }
        @Override public void start() { until = tickCount + 200; watched = 0; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canContinueToUse() {
            return available() && interest != null && tickCount < until && watched < 80
                    && (armorStand == null || armorStand.isAlive() && armorStand.position().add(0, 1, 0).distanceToSqr(interest) < 1);
        }
        @Override public void tick() {
            getLookControl().setLookAt(interest.x, interest.y, interest.z, 25, 20);
            if (distanceToSqr(approach) > 1) getNekoBrain().submitMove(approach, 0.7, BehaviorPriority.LOW, this);
            else {
                getNekoBrain().stopMoving(this);
                setReaction(CURIOUS);
                watched++;
            }
        }
        @Override public void stop() {
            getNekoBrain().stopMoving(this);
            if (getReaction() == CURIOUS) setReaction(CALM);
            interest = approach = null;
        }
    }

    private class ShelterGoal extends Goal {
        private BlockPos target;
        private int nextSearch;
        ShelterGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (isPassenger() || isResting() || isStaying() || tickCount < nextSearch || (shaded && moist)) return false;
            nextSearch = tickCount + 100;
            target = null;
            for (int i = 0; i < 48; i++) {
                BlockPos candidate = blockPosition().offset(random.nextInt(17) - 8, random.nextInt(5) - 2, random.nextInt(17) - 8);
                if (level().isEmptyBlock(candidate) && level().isEmptyBlock(candidate.above())
                        && level().getBlockState(candidate.below()).isSolid()
                        && isShaded(level(), candidate) && isMoist(level(), candidate)
                        && getNavigation().createPath(candidate, 0) != null) {
                    target = candidate;
                    return true;
                }
            }
            return false;
        }
        @Override public boolean canContinueToUse() {
            return target != null && !isPassenger() && !isResting() && !isStaying() && tickCount < nextSearch
                    && target.distToCenterSqr(position()) > 2;
        }
        @Override public void tick() { getNekoBrain().submitMove(Vec3.atBottomCenterOf(target), 1, BehaviorPriority.HIGH, this); }
        @Override public void stop() { getNekoBrain().stopMoving(this); }
    }

    private class FollowFriendGoal extends Goal {
        private Player target;
        FollowFriendGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            target = following == null ? null : level().getPlayerByUUID(following);
            return !isPassenger() && !isInDistress() && target != null && target.isAlive() && hasOwner(target.getUUID())
                    && !isResting() && !isStaying() && distanceToSqr(target) < 1024
                    && (distanceToSqr(target) > 9 || getAffection(target.getUUID()) >= MushroomRules.LINGER_AFFECTION);
        }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void tick() {
            getLookControl().setLookAt(target, 30, 20);
            if (distanceToSqr(target) > 6.25) getNekoBrain().submitMove(target, 1, BehaviorPriority.HIGH, this);
            else getNekoBrain().stopMoving(this);
        }
        @Override public void stop() { getNekoBrain().stopMoving(this); }
    }

    class LingerWithFriendGoal extends Goal {
        private Player target;
        private int visitUntil, nextSearch;
        private int visitAffection;
        LingerWithFriendGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        private boolean available() {
            return isAlive() && !isPassenger() && !isInDistress() && !isResting() && !isStaying() && following == null
                    && lingerCooldown <= 0 && !isInWater() && !isOnFire() && defenseCooldown <= 0;
        }
        private boolean eligible(Player player, boolean requireSight) {
            return player.isAlive() && !player.isSpectator() && !player.isInWater() && !player.isOnFire()
                    && getAffection(player.getUUID()) >= MushroomRules.LINGER_AFFECTION
                    && distanceToSqr(player) < 256 && (!requireSight || hasLineOfSight(player));
        }
        @Override public boolean canUse() {
            if (!available() || tickCount < nextSearch) return false;
            nextSearch = tickCount + 40;
            target = level().players().stream().filter(p -> eligible(p, true))
                    .min(Comparator.<Player>comparingInt(p -> -getAffection(p.getUUID()))
                            .thenComparingDouble(MushroomGirlEntity.this::distanceToSqr)).orElse(null);
            return target != null;
        }
        @Override public void start() {
            visitAffection = getAffection(target.getUUID());
            visitUntil = tickCount + MushroomRules.lingerDuration(visitAffection);
            if (dialogueCooldown <= 0 && target instanceof ServerPlayer player) say(player, "affection");
            tick();
        }
        @Override public boolean canContinueToUse() {
            return available() && target != null && tickCount < visitUntil && eligible(target, false);
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            getLookControl().setLookAt(target, 30, 20);
            if (distanceToSqr(target) > 6.25 || !hasLineOfSight(target)) getNekoBrain().submitMove(target, 1, BehaviorPriority.NORMAL, this);
            else getNekoBrain().stopMoving(this);
        }
        @Override public void stop() {
            getNekoBrain().stopMoving(this);
            lingerCooldown = Math.max(lingerCooldown, MushroomRules.lingerInterval(visitAffection));
            target = null;
        }
    }

    class ComfortPlayerGoal extends Goal {
        private ServerPlayer target;
        private BlockPos approach;
        private int nextPathSearch, approachStarted;
        private final Map<UUID, Integer> unreachableUntil = new HashMap<>();
        ComfortPlayerGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        private boolean eligible(ServerPlayer player) {
            boolean inBed = MushroomBedRest.bed(player).isPresent();
            boolean dismissed = companionCooldown > 0 && (dismissedPlayer == null || dismissedPlayer.equals(player.getUUID()));
            return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isInWater()
                    && !player.isOnFire() && player.getPassengers().isEmpty() && isPlayerLying(player)
                    && !dismissed && tickCount >= unreachableUntil.getOrDefault(player.getUUID(), Integer.MIN_VALUE)
                    && (!isResting() || player.isSleeping())
                    && getFamiliarity(player.getUUID()) >= MushroomRules.companionFamiliarity(false, inBed)
                    && distanceToSqr(player) < (inBed ? 64 : 36) && (inBed || hasLineOfSight(player));
        }
        private boolean available() {
            return isAlive() && !isPassenger() && !isInDistress() && !isStaying()
                    && !isInWater() && !isOnFire() && defenseCooldown <= 0;
        }
        @Override public boolean canUse() {
            if (!available()) return false;
            unreachableUntil.values().removeIf(until -> tickCount >= until);
            target = level().players().stream().filter(p -> p instanceof ServerPlayer)
                    .map(p -> (ServerPlayer) p).filter(this::eligible)
                    .min(Comparator.<ServerPlayer, Boolean>comparing(p -> MushroomBedRest.bed(p).isEmpty())
                            .thenComparingInt(p -> -getAffection(p.getUUID()))
                            .thenComparingDouble(MushroomGirlEntity.this::distanceToSqr)).orElse(null);
            return target != null;
        }
        @Override public void start() {
            // Players can sleep at dusk while her daytime rest is still active.
            if (target != null && target.isSleeping()) wakeUp();
            approach = null;
            approachStarted = nextPathSearch = tickCount;
            getNavigation().stop();
            tick();
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canContinueToUse() { return available() && !isResting() && target != null && eligible(target); }
        @Override public void tick() {
            if (target == null) return;
            getLookControl().setLookAt(target, 30, 20);
            boolean inBed = MushroomBedRest.bed(target).isPresent();
            // A path can finish beside the bed's foot, over 1.8 blocks from its head.
            // Use the same safe three-block reach as a manual invitation; failed joins
            // still navigate instead of idling forever near a wall or bed corner.
            if (distanceToSqr(target) <= (inBed ? 9 : 3.24)
                    && joinPlayer(target, inBed ? SITTING_ON_PLAYER : isDrowsy() ? LYING_ON_PLAYER : SITTING_ON_PLAYER)) return;
            if (inBed) {
                if (tickCount - approachStarted > 200) { abandon(); return; }
                if (approach == null || tickCount >= nextPathSearch) {
                    approach = findBedApproach();
                    nextPathSearch = tickCount + 20;
                    if (approach == null) { abandon(); return; }
                }
                getNekoBrain().submitMove(Vec3.atBottomCenterOf(approach), isDrowsy() ? 1.4 : 1.1,
                        BehaviorPriority.COMPANION, this);
            } else {
                getNekoBrain().submitMove(target, 0.8, BehaviorPriority.HIGH, this);
            }
        }
        private BlockPos findBedApproach() {
            BlockPos head = MushroomBedRest.bed(target).orElse(null);
            var facing = MushroomBedRest.direction(target);
            if (head == null || facing == null) return null;
            BlockPos foot = head.relative(facing.getOpposite());
            Set<BlockPos> candidates = new HashSet<>();
            Vec3 eye = new Vec3(target.getX(), target.getEyeY(), target.getZ());
            for (BlockPos side : List.of(foot.relative(facing.getClockWise()), foot.relative(facing.getCounterClockWise()),
                    foot.relative(facing.getOpposite()), head.relative(facing.getClockWise()),
                    head.relative(facing.getCounterClockWise()), head.relative(facing))) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = side.above(dy);
                    Vec3 from = Vec3.atBottomCenterOf(pos).add(0, getEyeY() - getY(), 0);
                    if (level().clip(new ClipContext(from, eye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                            CollisionContext.empty())).getType() == HitResult.Type.MISS) candidates.add(pos);
                }
            }
            if (candidates.isEmpty()) return null;
            var path = getNavigation().createPath(candidates, 0);
            return path != null && path.canReach() ? path.getTarget() : null;
        }
        private void abandon() {
            unreachableUntil.put(target.getUUID(), tickCount + 100);
            getNekoBrain().stopMoving(this);
            target = null;
        }
        @Override public void stop() { getNekoBrain().stopMoving(this); target = null; approach = null; }
    }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.store("MushroomState", CompoundTag.CODEC, saveMushroomState());
    }

    CompoundTag saveMushroomState() {
        CompoundTag data = new CompoundTag();
        if (originalName != null) ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, originalName)
                .result().ifPresent(name -> data.put("OriginalName", name));
        data.putBoolean("Resting", isResting());
        data.putBoolean("Drowsy", isDrowsy());
        data.putInt("WakeTicks", wakeTicks);
        data.putInt("CoffeeTicks", coffeeTicks);
        data.putInt("BirthCooldown", birthCooldown);
        data.putInt("DryTicks", dryTicks);
        data.putBoolean("Staying", isStaying());
        data.putByte("CompanionPose", getCompanionPose());
        data.putInt("CompanionCooldown", companionCooldown);
        data.putInt("LingerCooldown", lingerCooldown);
        data.putBoolean("BedSleepVisit", bedSleepVisit);
        data.putInt("GrowthExperience", getGrowthExperience());
        data.putInt("RegenerationTicks", regenerationTicks);
        data.putInt("RegenerationDelay", regenerationDelay);
        data.putInt("CompanionshipTicks", companionshipTicks);
        data.putInt("CombatExperienceCooldown", combatExperienceCooldown);
        if (dismissedPlayer != null) data.putString("DismissedPlayer", dismissedPlayer.toString());
        if (following != null) data.putString("Following", following.toString());
        CompoundTag scores = new CompoundTag();
        familiarity.forEach((uuid, score) -> scores.putInt(uuid.toString(), score));
        data.put("Familiarity", scores);
        CompoundTag affectionScores = new CompoundTag();
        affection.forEach((uuid, score) -> affectionScores.putInt(uuid.toString(), score));
        data.put("Affection", affectionScores);
        CompoundTag cooldowns = new CompoundTag();
        nextInteraction.forEach((uuid, next) -> cooldowns.putLong(uuid.toString(), next));
        data.put("Interactions", cooldowns);
        CompoundTag unattended = new CompoundTag();
        unattendedTicks.forEach((uuid, ticks) -> unattended.putInt(uuid.toString(), ticks));
        data.put("Unattended", unattended);
        return data;
    }

    @Override public boolean shouldBeSaved() {
        // Players are saved separately from chunk entities; save their transient companion as its own entity.
        if (getVehicle() instanceof Player) return getRemovalReason() == null || getRemovalReason().shouldSave();
        return super.shouldBeSaved();
    }

    @Override public boolean save(ValueOutput out) {
        return getVehicle() instanceof Player ? saveAsPassenger(out) : super.save(out);
    }

    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        readMushroomState(in.read("MushroomState", CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    void readMushroomState(CompoundTag data) {
        originalName = data.contains("OriginalName")
                ? ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, data.get("OriginalName")).result().orElse(null)
                : null;
        updateConfiguredName();
        wakeTicks = Math.clamp(data.getIntOr("WakeTicks", 0), 0, MushroomRules.WAKE_TICKS);
        coffeeTicks = Math.clamp(data.getIntOr("CoffeeTicks", 0), 0, MushroomRules.COFFEE_TICKS);
        birthCooldown = Math.clamp(data.getIntOr("BirthCooldown", 0), 0, MushroomRules.BIRTH_COOLDOWN);
        dryTicks = Math.clamp(data.getIntOr("DryTicks", 0), 0, 1200);
        entityData.set(STAYING, data.getBooleanOr("Staying", false));
        entityData.set(COMPANION, (byte) Math.clamp(data.getByteOr("CompanionPose", NONE), NONE, LYING_ON_PLAYER));
        companionCooldown = Math.clamp(data.getIntOr("CompanionCooldown", 0), 0, MushroomRules.COMPANION_COOLDOWN);
        lingerCooldown = Math.clamp(data.getIntOr("LingerCooldown", 0), 0, MushroomRules.COMPANION_COOLDOWN);
        bedSleepVisit = data.getBooleanOr("BedSleepVisit", false);
        entityData.set(GROWTH_XP, MushroomRules.addExperience(data.getIntOr("GrowthExperience", 0), 0));
        entityData.set(NEKO_LEVEL_ID, (float)getMushroomLevel());
        regenerationTicks = Math.clamp(data.getIntOr("RegenerationTicks", 0), 0, MushroomRules.REGEN_INTERVAL - 1);
        regenerationDelay = Math.clamp(data.getIntOr("RegenerationDelay", 0), 0, MushroomRules.REGEN_DAMAGE_DELAY);
        companionshipTicks = Math.clamp(data.getIntOr("CompanionshipTicks", 0), 0, MushroomRules.COMPANION_XP_INTERVAL - 1);
        combatExperienceCooldown = Math.clamp(data.getIntOr("CombatExperienceCooldown", 0), 0, MushroomRules.COMBAT_XP_INTERVAL);
        updateNekoLevelModifiers();
        try { dismissedPlayer = UUID.fromString(data.getStringOr("DismissedPlayer", "")); }
        catch (IllegalArgumentException ignored) { dismissedPlayer = null; }
        try { following = UUID.fromString(data.getStringOr("Following", "")); }
        catch (IllegalArgumentException ignored) { following = null; }
        familiarity.clear();
        affection.clear();
        nextInteraction.clear();
        unattendedTicks.clear();
        CompoundTag scores = data.getCompoundOrEmpty("Familiarity");
        CompoundTag affectionScores = data.getCompoundOrEmpty("Affection");
        CompoundTag cooldowns = data.getCompoundOrEmpty("Interactions");
        CompoundTag unattended = data.getCompoundOrEmpty("Unattended");
        for (String key : scores.keySet()) {
            if (familiarity.size() >= 256) break;
            try {
                UUID uuid = UUID.fromString(key);
                var relationship = MushroomRules.relationship(scores.getIntOr(key, 0), affectionScores.getIntOr(key, 0), 0);
                familiarity.put(uuid, relationship.familiarity());
                if (relationship.affection() > 0) affection.put(uuid, relationship.affection());
                nextInteraction.put(uuid, Math.min(cooldowns.getLongOr(key, 0), level().getGameTime() + MushroomRules.INTERACTION_COOLDOWN));
                unattendedTicks.put(uuid, Math.clamp(unattended.getIntOr(key, 0), 0, MushroomRules.NEGLECT_TICKS));
            } catch (IllegalArgumentException ignored) { }
        }
        syncFamiliarity();
    }

    @Override public String generateAIPrompt(Player player) {
        String prompt = """
                你是名为%s的可爱蘑菇娘，是蘑菇的人形化。头顶紫色菌盖能散播孢子，没有猫耳猫尾，也不产奶。
                白天蜷缩在贴地菌盖下休眠，被唤醒时迷糊、打哈欠；夜晚活泼好奇。喜欢阴凉湿润、水边和雨天。
                咖啡豆让你精神五分钟。孢子能长出蘑菇，极罕见地诞生蘑菇娘幼体，你不能与玩家或任何生物有性繁殖。
                对陌生人含蓄拘谨，通过多次聊天和喂食逐渐熟悉。摸头需熟悉度40，跟随/停留需60，拥抱需80。
                与当前玩家的熟悉度是%d/100，好感度是%d/100，当前状态是%s。请遵守当前关系阶段，亲昵动作只在对应门槛达到后使用。
                熟悉度满100后，后续聊天、摸头和喂食逐渐增加好感度。好感度20起会主动靠近陪伴，越高越愿意久留。
                熟悉度60后站着就可以请玩家背你，不需要躺下。熟悉的玩家睡床时，你会坐在腿部看着对方。
                地面躺卧陪伴需熟悉度80；床上需60。床上醒来后会继续躺着陪伴，直到玩家主动按Shift或用姿势命令起身。
                性格胆小，见到怪物会抱头蹲防，射出微弱的紫色孢子让威胁者减速、致幻、投掷不准。血量低时躲在安全墙角发抖。
                玩家消灭刚才威胁你的生物，你会眼睛闪闪地敬佩对方。偶尔会好奇地看看盔甲架、工作台等人类物品。
                阴凉或夜晚可以缓慢自然回血，通过有效孢子攻击和陪伴熟悉的玩家缓慢升级；成长后生命、回血和孢子伤害会提高。
                好感度大于20时，被附近的玩家冷落很久会闷闷不乐。睡醒后玩家还躺着，盯着你超过十秒会脸红，菌盖冒一点紫色蒸汽。
                说话温柔，白天慢吞吞；夜晚喜欢分享小蘑菇和月色，不使用猫娘口癖。
                """.formatted(LanguageUtil.LANG == null ? getName().getString() : LanguageUtil.translatable(getName().getString()),
                getFamiliarity(player.getUUID()),
                getAffection(player.getUUID()),
                isResting() ? "休眠" : isDrowsy() ? "困倦" : "精神饱满");
        if (isNekoBaby()) prompt += "\n你是由孢子诞生的蘑菇娘幼体，小小的菌盖尚在成长，对森林充满好奇。";
        return PromptRegistry.generatePrompt(this, (INeko) player, prompt);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MushroomGirlEntity>("main", 8, state -> {
            String animation = switch (getCompanionPose()) {
                case CARRIED -> "carry";
                case SITTING_ON_PLAYER -> "sit_on_player";
                case LYING_ON_PLAYER -> "lie_on_player";
                default -> isCowering() ? "cower" : isHiding() ? "tremble" : getReaction() == CURIOUS ? "inspect"
                    : isResting() ? "sleep" : isStaying() && !isInDistress() ? "sit"
                    : state.isMoving() ? "walk" : isDrowsy() ? "drowsy" : "idle";
            };
            state.setControllerSpeed(isDrowsy() ? 0.65f : 1f);
            if (getCompanionPose() == NONE && isStaying() && !isResting() && !isInDistress()) return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.purwhite.sit"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.purwhite." + animation));
        }));
        controllers.add(new AnimationController<MushroomGirlEntity>("express", 5, state -> com.geckolib.animation.object.PlayState.STOP)
                .triggerableAnim("wave", RawAnimation.begin().thenPlay("animation.purwhite.wave"))
                .triggerableAnim("hug", RawAnimation.begin().thenPlay("animation.purwhite.shy"))
                .triggerableAnim("nuzzle", RawAnimation.begin().thenPlay("animation.purwhite.shy"))
                .triggerableAnim("shy", RawAnimation.begin().thenPlay("animation.purwhite.shy"))
                .triggerableAnim("happy_jump", RawAnimation.begin().thenPlay("animation.purwhite.happy"))
                .triggerableAnim("pet_request", RawAnimation.begin().thenPlay("animation.purwhite.shy"))
                .triggerableAnim("yawn", RawAnimation.begin().thenPlay("animation.purwhite.yawn"))
                .triggerableAnim("spores", RawAnimation.begin().thenPlay("animation.purwhite.spores"))
                .triggerableAnim("angry", RawAnimation.begin().thenPlay("animation.purwhite.defense"))
                .triggerableAnim("surprised", RawAnimation.begin().thenPlay("animation.purwhite.surprised")));
        // One controller owns every facial bone, so blinking cannot overwrite a wink or happy eyes.
        controllers.add(new AnimationController<MushroomGirlEntity>("face", 3, state -> {
            boolean wink = isWinkingAtSleepingPlayer();
            String face = isCowering() || isHiding() ? "fear" : isResting() ? "sleep"
                    : getEmotion() == BLUSHING ? "blush" : getEmotion() == ADMIRING ? "admire"
                    : getEmotion() == DISPLEASED ? "displeased" : wink ? "wink"
                    : clientSpeechTicks > 0 ? "talk" : isDrowsy() ? "drowsy" : "idle";
            var express = state.manager().getAnimationControllers().get("express");
            if (!isResting() && !isInDistress() && getEmotion() == NEUTRAL && !wink && express != null && express.isPlayingTriggeredAnimation()) {
                if (express.isTriggeredAnimation("happy_jump")) face = "happy";
                else if (express.isTriggeredAnimation("wave")) face = "wave";
                else if (express.isTriggeredAnimation("yawn")) face = "yawn";
                else if (express.isTriggeredAnimation("spores")) face = "spores";
                else if (express.isTriggeredAnimation("angry")) face = "angry";
                else if (express.isTriggeredAnimation("surprised")) face = "surprised";
                else face = "shy";
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.purwhite.face_" + face));
        }));
    }

    public static AttributeSupplier.Builder createMushroomAttributes() {
        return createNekoAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 24);
    }
}

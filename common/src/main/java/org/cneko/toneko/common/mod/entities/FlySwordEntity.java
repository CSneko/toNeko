package org.cneko.toneko.common.mod.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class FlySwordEntity extends Entity {
    private static final EntityDataAccessor<String> BLOCK_ID =
            SynchedEntityData.defineId(FlySwordEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DISPLAY_ITEM =
            SynchedEntityData.defineId(FlySwordEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> PITCH =
            SynchedEntityData.defineId(FlySwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL =
            SynchedEntityData.defineId(FlySwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YAW_OFFSET =
            SynchedEntityData.defineId(FlySwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_SPEED =
            SynchedEntityData.defineId(FlySwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_MINECART =
            SynchedEntityData.defineId(FlySwordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> MINECART_TYPE =
            SynchedEntityData.defineId(FlySwordEntity.class, EntityDataSerializers.STRING);

    // Minecart item-to-entity type mapping
    private static final Map<Item, EntityType<? extends AbstractMinecart>> MINECART_ITEM_TO_ENTITY = Map.of(
            Items.MINECART,         EntityType.MINECART,
            Items.CHEST_MINECART,   EntityType.CHEST_MINECART,
            Items.FURNACE_MINECART, EntityType.FURNACE_MINECART,
            Items.HOPPER_MINECART,  EntityType.HOPPER_MINECART,
            Items.TNT_MINECART,     EntityType.TNT_MINECART
    );

    private static final Map<String, EntityType<? extends AbstractMinecart>> TYPE_NAME_TO_ENTITY = Map.of(
            "rideable", EntityType.MINECART,
            "chest",    EntityType.CHEST_MINECART,
            "furnace",  EntityType.FURNACE_MINECART,
            "hopper",   EntityType.HOPPER_MINECART,
            "tnt",      EntityType.TNT_MINECART
    );

    private float targetPitch, targetRoll;
    private float prevPitch, prevRoll, prevYawOffset;

    private ItemStack swordStack = ItemStack.EMPTY;
    private int hitCooldown = 0;

    // Upgrade levels
    private int ironLevel = 0;     // increases mass
    private int diamondLevel = 0;  // increases damage
    private int netheriteLevel = 0; // increases max speed & acceleration
    private int maxUpgradeLimit = 10; // base limit, increased by gold
    private static final float IRON_MASS_PER_LEVEL = 0.5f;
    private static final float DIAMOND_DAMAGE_PER_LEVEL = 1.5f;
    private static final float NETHERITE_SPEED_PER_LEVEL = 0.15f;

    /** Diminishing returns: sqrt(level) instead of linear */
    private static float dim(int level, float perLevel) {
        return (float) (Math.sqrt(level) * perLevel * 3.0f);
    }
    private int fuelTicks = 0;
    private int maxFuelTicks = 100;
    private float fuelPower = 1.0f;
    // Client-side smoothed speed
    private double clientSpeed = 0;
    private double[] speedSamples = new double[10];
    private int speedIdx = 0;
    private double _lx, _ly, _lz;
    private int tickCount = 0;
    private static final java.util.Map<Item, FuelData> FUEL_TABLE = new java.util.LinkedHashMap<>();
    static {
        FUEL_TABLE.put(Items.COAL,         new FuelData(80, 2.5f));
        FUEL_TABLE.put(Items.CHARCOAL,     new FuelData(80, 2.5f));
        FUEL_TABLE.put(Items.COAL_BLOCK,   new FuelData(800, 3.0f));
        FUEL_TABLE.put(Items.BLAZE_ROD,    new FuelData(120, 3.5f));
        FUEL_TABLE.put(Items.BLAZE_POWDER, new FuelData(60, 3.0f));
        FUEL_TABLE.put(Items.LAVA_BUCKET,  new FuelData(1000, 1.8f));
        FUEL_TABLE.put(Items.FIRE_CHARGE,  new FuelData(200, 4.0f));
        FUEL_TABLE.put(Items.DRIED_KELP_BLOCK, new FuelData(400, 2.0f));
    }

    private record FuelData(int ticks, float power) {}

    public FlySwordEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLOCK_ID, "minecraft:diamond_block");
        builder.define(DISPLAY_ITEM, "");
        builder.define(PITCH, 0f);
        builder.define(ROLL, 0f);
        builder.define(YAW_OFFSET, 0f);
        builder.define(MAX_SPEED, 1.5f);
        builder.define(IS_MINECART, false);
        builder.define(MINECART_TYPE, "");
    }

    // === Mass ===
    public float getMass() {
        float base = 10.0f;
        // Minecart: heavy vehicle mass (like a "大运" truck)
        if (isMinecartMode()) {
            base += 250.0f;
            // TNT minecart slightly lighter, chest/furnace heavier
            if (swordStack.is(Items.TNT_MINECART)) {
                base += 50.0f;
            } else if (swordStack.is(Items.CHEST_MINECART) || swordStack.is(Items.FURNACE_MINECART)) {
                base += 100.0f;
            }
        } else if (swordStack.getItem() instanceof BlockItem bi) {
            BlockState bs = bi.getBlock().defaultBlockState();
            // Approximate mass from hardness
            base += bs.getDestroySpeed(level(), BlockPos.ZERO) * 3.0f;
        }
        // Iron upgrades
        base += dim(ironLevel, IRON_MASS_PER_LEVEL);
        // Weight increases by 20% per iron level
        base *= (1.0f + ironLevel * 0.2f);
        return Math.max(5.0f, base * org.cneko.toneko.common.util.ConfigUtil.getFlySwordMassMultiplier());
    }

    // === Upgrades ===
    public int getIronLevel() { return ironLevel; }
    public int getDiamondLevel() { return diamondLevel; }
    public int getNetheriteLevel() { return netheriteLevel; }
    public int getMaxUpgradeLimit() { return maxUpgradeLimit; }
    public int getFuelTicks() { return fuelTicks; }
    public int getMaxFuelTicks() { return maxFuelTicks; }
    public float getFuelPower() { return fuelPower; }
    public double getClientSpeed() { return clientSpeed; }
    public float getSyncedMaxSpeed() { return entityData.get(MAX_SPEED); }

    // === Minecart mode ===
    public boolean isMinecartMode() {
        return !swordStack.isEmpty() && swordStack.getItem() instanceof MinecartItem;
    }
    public boolean isSyncedMinecartMode() {
        return entityData.get(IS_MINECART);
    }
    public EntityType<? extends AbstractMinecart> getMinecartEntityType() {
        return MINECART_ITEM_TO_ENTITY.getOrDefault(swordStack.getItem(), EntityType.MINECART);
    }
    public EntityType<? extends AbstractMinecart> getMinecartEntityTypeClient() {
        return TYPE_NAME_TO_ENTITY.getOrDefault(entityData.get(MINECART_TYPE), EntityType.MINECART);
    }

    // Client-side cached minecart entity for rendering
    @Nullable
    private transient AbstractMinecart cachedRenderMinecart;
    private transient String cachedRenderMinecartType;

    @Nullable
    public AbstractMinecart getOrCreateRenderMinecart() {
        if (!level().isClientSide) return null;

        String currentType = entityData.get(MINECART_TYPE);
        if (cachedRenderMinecart == null || !currentType.equals(cachedRenderMinecartType)) {
            EntityType<? extends AbstractMinecart> entityType = getMinecartEntityTypeClient();
            if (entityType != null) {
                cachedRenderMinecart = (AbstractMinecart) entityType.create(level());
                cachedRenderMinecartType = currentType;
            }
        }
        return cachedRenderMinecart;
    }

    public void setIronLevel(int v) { this.ironLevel = v; }
    public void setDiamondLevel(int v) { this.diamondLevel = v; }
    public void setNetheriteLevel(int v) { this.netheriteLevel = v; }
    public void setMaxUpgradeLimit(int v) { this.maxUpgradeLimit = Math.min(v, 100); }

    // === Stored ItemStack ===
    public ItemStack getSwordStack() { return swordStack; }

    public void setSwordStack(ItemStack stack) {
        this.swordStack = stack.copy();
        updateAppearanceFromStack();
    }

    /** Build an item stack representing this fly sword (with upgrades + stored item preserved) */
    /** Build a fly sword item stack with upgrades preserved (always returns the fly sword item) */
    public ItemStack buildFlySwordItem() {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("toneko", "fly_sword"));
        if (item == null) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(item);
        CompoundTag data = new CompoundTag();
        data.putInt("iron", ironLevel);
        data.putInt("diamond", diamondLevel);
        data.putInt("netherite", netheriteLevel);
        data.putInt("maxLimit", maxUpgradeLimit);
        result.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(data));
        return result;
    }

    private void updateAppearanceFromStack() {
        if (swordStack.isEmpty()) {
            entityData.set(DISPLAY_ITEM, "");
            entityData.set(BLOCK_ID, "minecraft:diamond_block");
            entityData.set(IS_MINECART, false);
            entityData.set(MINECART_TYPE, "");
            return;
        }
        if (swordStack.getItem() instanceof BlockItem bi) {
            entityData.set(BLOCK_ID, BuiltInRegistries.BLOCK.getKey(bi.getBlock()).toString());
            entityData.set(DISPLAY_ITEM, "");
            entityData.set(IS_MINECART, false);
            entityData.set(MINECART_TYPE, "");
        } else if (swordStack.getItem() instanceof MinecartItem) {
            // Minecart items: still set DISPLAY_ITEM for backward compat, flag minecart mode
            ResourceLocation rl = BuiltInRegistries.ITEM.getKey(swordStack.getItem());
            entityData.set(DISPLAY_ITEM, rl != null ? rl.toString() : "");
            entityData.set(BLOCK_ID, "");
            entityData.set(IS_MINECART, true);
            entityData.set(MINECART_TYPE, getMinecartTypeString(swordStack.getItem()));
            refreshDimensions();
        } else {
            ResourceLocation rl = BuiltInRegistries.ITEM.getKey(swordStack.getItem());
            entityData.set(DISPLAY_ITEM, rl != null ? rl.toString() : "");
            entityData.set(IS_MINECART, false);
            entityData.set(MINECART_TYPE, "");
        }
    }

    private static String getMinecartTypeString(Item item) {
        if (item == Items.CHEST_MINECART)   return "chest";
        if (item == Items.FURNACE_MINECART) return "furnace";
        if (item == Items.HOPPER_MINECART)  return "hopper";
        if (item == Items.TNT_MINECART)     return "tnt";
        return "rideable";
    }

    public BlockState getBlockState() {
        ResourceLocation rl = ResourceLocation.tryParse(entityData.get(BLOCK_ID));
        if (rl != null) {
            return BuiltInRegistries.BLOCK.getOptional(rl).orElse(Blocks.DIAMOND_BLOCK).defaultBlockState();
        }
        return Blocks.DIAMOND_BLOCK.defaultBlockState();
    }

    public String getDisplayItem() { return entityData.get(DISPLAY_ITEM); }

    // === Rotation ===
    public float getPitch(float pt) { return prevPitch + (entityData.get(PITCH) - prevPitch) * pt; }
    public float getRoll(float pt) { return prevRoll + (entityData.get(ROLL) - prevRoll) * pt; }
    public float getYawOffset(float pt) { return prevYawOffset + (entityData.get(YAW_OFFSET) - prevYawOffset) * pt; }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
        prevPitch = entityData.get(PITCH);
        prevRoll = entityData.get(ROLL);
        prevYawOffset = entityData.get(YAW_OFFSET);
        if (hitCooldown > 0) hitCooldown--;

        // Client-side speed: sample position delta every tick, smooth with moving average
        if (level().isClientSide && tickCount % 2 == 0) {
            double px = getX(); double py = getY(); double pz = getZ();
            speedSamples[speedIdx % 10] = Math.sqrt(
                    (px - _lx) * (px - _lx) + (py - _ly) * (py - _ly) + (pz - _lz) * (pz - _lz)
            ) / 0.1; // 2 ticks * 0.05s = 0.1s
            _lx = px; _ly = py; _lz = pz;
            // Average samples
            int n = Math.min(10, speedIdx + 1);
            double sum = 0;
            for (int i = 0; i < n; i++) sum += speedSamples[i];
            clientSpeed = sum / n;
            speedIdx++;
        }

        if (isVehicle()) {
            Entity p = getFirstPassenger();
            if (p instanceof Player player) pilotTick(player);
        } else if (!isNoGravity()) {
            // Gravity scales with mass: heavier objects fall faster
            float gravityScale = Math.min(1.0f + getMass() / 100.0f, 5.0f);
            setDeltaMovement(getDeltaMovement().add(0, -0.04 * gravityScale, 0));
        }

        if (!level().isClientSide) {
            float p = entityData.get(PITCH);
            float r = entityData.get(ROLL);
            entityData.set(PITCH, p + (targetPitch - p) * 0.15f);
            entityData.set(ROLL, r + (targetRoll - r) * 0.15f);
        }

        Vec3 preMoveVel = getDeltaMovement();
        double spdBefore = preMoveVel.length();
        move(MoverType.SELF, preMoveVel);
        Vec3 postMoveVel = getDeltaMovement();
        double spdAfter = postMoveVel.length();

        // Block collision: detect if any velocity component was zeroed (hit a wall)
        boolean hitX = spdBefore > 0.1 && Math.abs(preMoveVel.x) > 0.01 && Math.abs(postMoveVel.x) < 0.001;
        boolean hitY = spdBefore > 0.1 && Math.abs(preMoveVel.y) > 0.01 && Math.abs(postMoveVel.y) < 0.001;
        boolean hitZ = spdBefore > 0.1 && Math.abs(preMoveVel.z) > 0.01 && Math.abs(postMoveVel.z) < 0.001;
        boolean blockHit = !level().isClientSide && (hitX || hitY || hitZ);

        if (blockHit) {
            // Find collision point
            Vec3 dir = preMoveVel.normalize();
            BlockPos bp = BlockPos.containing(
                    getX() + (hitX ? Math.signum(preMoveVel.x) * 0.7 : 0),
                    getY() + (hitY ? Math.signum(preMoveVel.y) * 0.7 : 0),
                    getZ() + (hitZ ? Math.signum(preMoveVel.z) * 0.7 : 0));
            BlockState bs = level().getBlockState(bp);

            // TNT detonation
            if (bs.getBlock() instanceof TntBlock && (spdBefore * getMass()) > 5.0) {
                level().removeBlock(bp, false);
                level().explode(this, getX(), getY(), getZ(), 2.0f, Level.ExplosionInteraction.BLOCK);
                setDeltaMovement(dir.scale(-spdBefore * 1.5));
                return; // skip drag this tick
            }

            // Rebound
            if (!bs.isAir() && bs.getDestroySpeed(level(), bp) >= 0) {
                if (isSyncedMinecartMode()) {
                    // Minecart: ram through very weak blocks, moderate slowdown on soft blocks
                    float hardness = bs.getDestroySpeed(level(), bp);
                    if (hardness <= 0.5f && hardness >= 0) {
                        // Destroy only very weak blocks (crops, torches, flowers, etc.)
                        level().destroyBlock(bp, true);
                        setDeltaMovement(preMoveVel.scale(0.85));
                    } else if (hardness <= 2.0f) {
                        // Soft blocks: moderate slowdown, no destruction
                        double bounce = 0.08;
                        Vec3 newVel = postMoveVel;
                        if (hitX) newVel = new Vec3(-preMoveVel.x * bounce, newVel.y, newVel.z);
                        if (hitY) newVel = new Vec3(newVel.x, -preMoveVel.y * bounce, newVel.z);
                        if (hitZ) newVel = new Vec3(newVel.x, newVel.y, -preMoveVel.z * bounce);
                        setDeltaMovement(newVel);
                    } else {
                        // Hard blocks: tiny bounce, keep momentum
                        double bounce = 0.05;
                        Vec3 newVel = postMoveVel;
                        if (hitX) newVel = new Vec3(-preMoveVel.x * bounce, newVel.y, newVel.z);
                        if (hitY) newVel = new Vec3(newVel.x, -preMoveVel.y * bounce, newVel.z);
                        if (hitZ) newVel = new Vec3(newVel.x, newVel.y, -preMoveVel.z * bounce);
                        setDeltaMovement(newVel);
                    }
                    level().playSound(null, getX(), getY(), getZ(),
                            SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5f, 1.2f);
                } else {
                    boolean isSlimeSword = swordStack.getItem() instanceof BlockItem cbi
                            && cbi.getBlock() instanceof net.minecraft.world.level.block.SlimeBlock;
                    double bounce = isSlimeSword ? 1.0
                            : bs.getDestroySpeed(level(), bp) > 5 ? 0.5 : 0.3;
                    // Reflect velocity components that hit
                    Vec3 newVel = postMoveVel;
                    if (hitX) newVel = new Vec3(-preMoveVel.x * bounce, newVel.y, newVel.z);
                    if (hitY) newVel = new Vec3(newVel.x, -preMoveVel.y * bounce, newVel.z);
                    if (hitZ) newVel = new Vec3(newVel.x, newVel.y, -preMoveVel.z * bounce);
                    setDeltaMovement(newVel);
                    level().playSound(null, getX(), getY(), getZ(),
                            SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3f, 2.0f);
                }
            }
        }

        // Inertia: heavier = slower deceleration (minecart plows through)
        if (isSyncedMinecartMode()) {
            // Minecart: near-zero drag — rams through everything
            setDeltaMovement(getDeltaMovement().scale(0.998));
        } else {
            float drag = 1.0f / (1.0f + getMass() * 0.02f);
            setDeltaMovement(getDeltaMovement().scale(0.95f + drag * 0.05f - 0.05f));
        }
        if (Math.abs(getDeltaMovement().x) < 0.001) setDeltaMovement(new Vec3(0, getDeltaMovement().y, getDeltaMovement().z));
        if (Math.abs(getDeltaMovement().z) < 0.001) setDeltaMovement(new Vec3(getDeltaMovement().x, getDeltaMovement().y, 0));

        if (!level().isClientSide && isVehicle() && hitCooldown <= 0) {
            checkHitEntities();
        }
    }

    private void checkHitEntities() {
        Entity rider = getFirstPassenger();
        if (!(rider instanceof Player player)) return;

        Vec3 myVel = getDeltaMovement();
        double speed = myVel.length();
        if (speed < 0.3) return;

        // Minecart: much larger hit detection area (like a truck)
        double inflateAmount = isSyncedMinecartMode() ? 2.5 : 0.5;
        AABB box = getBoundingBox().inflate(inflateAmount);
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != rider && e.isAlive());

        float myMass = getMass();

        for (LivingEntity target : targets) {
            // Target mass: health^1.25 (exponential), min 10
            float targetMass = (float) Math.max(10f, Math.pow(target.getMaxHealth(), 1.25));

            boolean slime = swordStack.getItem() instanceof BlockItem bi
                    && bi.getBlock() instanceof net.minecraft.world.level.block.SlimeBlock;

            // Damage (zero for slime/elastic blocks)
            float damage = 0;
            if (!slime) {
                damage = 0.5f + dim(diamondLevel, DIAMOND_DAMAGE_PER_LEVEL) * 0.5f
                        + (float) (speed * myMass * 0.03f);
                damage = Math.max(0.5f, damage * ConfigUtil.getFlySwordDamageMultiplier());
            }

            if (damage > 0) {
                DamageSource source = level().damageSources().playerAttack(player);
                target.hurt(source, damage);
            }

            // === Vector-based collision ===
            // m1 * v1 + m2 * v2 = m1 * v1' + m2 * v2'
            double restitution = slime ? 1.0 : 0.3;
            double totalMass = myMass + targetMass;
            Vec3 targetVel = target.getDeltaMovement();

            // My new velocity (vector): v1' = (m1 - e*m2)/(m1+m2) * v1 + (1+e)*m2/(m1+m2) * v2
            double coef1 = (myMass - restitution * targetMass) / totalMass;
            double coef2 = (1.0 + restitution) * targetMass / totalMass;
            Vec3 myNewVel = myVel.scale(coef1).add(targetVel.scale(coef2));
            // Clamp: don't reverse completely on small targets
            if (myNewVel.length() < 0.05 || myNewVel.dot(myVel) < 0 && !slime) {
                myNewVel = myVel.scale(0.15);
            }
            setDeltaMovement(myNewVel);

            // Target velocity (vector): v2' = (1+e)*m1/(m1+m2) * v1 + (m2 - e*m1)/(m1+m2) * v2
            double coef3 = (1.0 + restitution) * myMass / totalMass;
            double coef4 = (targetMass - restitution * myMass) / totalMass;
            Vec3 targetNewVel = myVel.scale(coef3).add(targetVel.scale(coef4));
            // Apply knockback as velocity push
            target.setDeltaMovement(targetNewVel);
            target.hurtMarked = true;

            hitCooldown = slime ? 2 : 4;
            break;
        }
    }

    private void pilotTick(Player player) {
        player.setPose(Pose.STANDING);

        // Fuel: hold fuel items for thrust boost (different fuels = different power)
        ItemStack held = player.getMainHandItem();
        FuelData fuel = FUEL_TABLE.get(held.getItem());
        if (fuel != null && fuelTicks <= 0) {
            if (!level().isClientSide) held.shrink(1);
            fuelTicks = fuel.ticks;
            maxFuelTicks = fuel.ticks;
            fuelPower = fuel.power;
        }
        // Without fuel, speed is 1/10. Fuel multiplier configurable.
        float flyMultiplier = ConfigUtil.getFlySwordFuelMultiplier();
        float boost = fuelTicks > 0 ? fuelPower * flyMultiplier : 0.1f;
        if (fuelTicks > 0) {
            fuelTicks--;
            // Minecart: double fuel consumption for extra power
            if (isSyncedMinecartMode()) fuelTicks--;
        }

        float forward = player.zza;
        float strafe = player.xxa;
        float speed = player.isSprinting() ? 1.5f : 0.8f;

        // Minecart: modest speed boost for truck-like feel
        boolean minecart = isSyncedMinecartMode();
        float minecartSpeedMult = minecart ? 1.5f : 1.0f;
        float minecartAccelMult = minecart ? 1.5f : 1.0f;

        // Netherite increases max speed, config multiplier applied
        float speedMult = ConfigUtil.getFlySwordSpeedMultiplier();
        float netheriteBonus = dim(netheriteLevel, NETHERITE_SPEED_PER_LEVEL);
        float maxSpeed = (1.5f + netheriteBonus) * speedMult * minecartSpeedMult;
        float netheriteAccel = (1.0f + (float)Math.sqrt(netheriteLevel) * 0.3f) * speedMult;

        float accel = speed * netheriteAccel * boost * minecartAccelMult / (1.0f + getMass() * 0.02f);

        float yaw = player.getYRot() + entityData.get(YAW_OFFSET);
        double rad = Math.toRadians(yaw);

        Vec3 move = new Vec3(
                (-Math.sin(rad) * forward + Math.cos(rad) * strafe) * accel * 0.1,
                0,
                (Math.cos(rad) * forward + Math.sin(rad) * strafe) * accel * 0.1);

        setDeltaMovement(getDeltaMovement().add(move));

        // Sync max speed for client HUD
        if (!level().isClientSide) entityData.set(MAX_SPEED, maxSpeed * boost);
        // Cap speed
        if (getDeltaMovement().length() > maxSpeed * boost) {
            setDeltaMovement(getDeltaMovement().scale((maxSpeed * boost) / getDeltaMovement().length()));
        }

        float lookPitch = player.getXRot();
        setDeltaMovement(getDeltaMovement().add(0, -Math.sin(Math.toRadians(lookPitch)) * accel * 0.06, 0));

        float vy = (float) getDeltaMovement().y;
        targetPitch = Math.clamp(vy * -35f, -60, 60);
        targetRoll = -strafe * 25f;

        if (level().isClientSide && getDeltaMovement().length() > 0.05) {
            int particles = fuelTicks > 0 ? 5 : 2;
            for (int i = 0; i < particles; i++) {
                level().addParticle(fuelTicks > 0
                                ? net.minecraft.core.particles.ParticleTypes.FLAME
                                : net.minecraft.core.particles.ParticleTypes.CLOUD,
                        getX() + random.nextFloat() * 0.8 - 0.4, getY() - 0.1,
                        getZ() + random.nextFloat() * 0.8 - 0.4, 0, -0.02, 0);
            }
        }
    }

    // === Interaction ===

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (level().isClientSide) return false;
        // Drop stored weapon if present
        if (!swordStack.isEmpty()) {
            level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    level(), getX(), getY(), getZ(), swordStack.copy()));
        }
        // Always drop the fly sword item with upgrades
        ItemStack flyItem = buildFlySwordItem();
        if (!flyItem.isEmpty()) {
            level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    level(), getX(), getY(), getZ(), flyItem));
        }
        this.discard();
        return false;
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);

        // Upgrade with iron ingot (increases mass)
        if (held.is(Items.IRON_INGOT) && ironLevel < maxUpgradeLimit) {
            if (!level().isClientSide) {
                ironLevel++;
                held.shrink(1);
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5f, 1.5f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Upgrade with diamond (increases damage)
        if (held.is(Items.DIAMOND) && diamondLevel < maxUpgradeLimit) {
            if (!level().isClientSide) {
                diamondLevel++;
                held.shrink(1);
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5f, 2.0f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Netherite ingot (increases speed/acceleration)
        if (held.is(Items.NETHERITE_INGOT) && netheriteLevel < maxUpgradeLimit) {
            if (!level().isClientSide) {
                netheriteLevel++;
                held.shrink(1);
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5f, 2.0f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Gold ingot (increases max upgrade limit, up to 100)
        if (held.is(Items.GOLD_INGOT) && maxUpgradeLimit < 100) {
            if (!level().isClientSide) {
                maxUpgradeLimit++;
                held.shrink(1);
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Don't transfer fuel items — they're fuel while riding
        if (FUEL_TABLE.containsKey(held.getItem())) {
            return InteractionResult.PASS;
        }

        // Transfer item into sword
        if (!held.isEmpty()) {
            if (!level().isClientSide) {
                if (!swordStack.isEmpty()) {
                    player.getInventory().add(swordStack.copy());
                }
                swordStack = held.copy();
                player.setItemInHand(hand, ItemStack.EMPTY);
                updateAppearanceFromStack();
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 2.0f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Empty hand = mount
        if (!player.isPassenger() && !level().isClientSide) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override public boolean isPickable() { return true; }

    private static final EntityDimensions MINECART_DIMENSIONS = EntityDimensions.scalable(2.5f, 1.5f);

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        if (isSyncedMinecartMode()) {
            return MINECART_DIMENSIONS;
        }
        return super.getDimensions(pose);
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity p) { return getPassengers().isEmpty(); }

    @Override
    public void positionRider(@NotNull Entity p, @NotNull MoveFunction cb) {
        super.positionRider(p, cb);
        if (p instanceof Player player) {
            player.setYRot(player.getYRot() + entityData.get(YAW_OFFSET));
            player.setPose(Pose.STANDING);
        }
    }

    @Override
    protected @NotNull Vec3 getPassengerAttachmentPoint(@NotNull Entity e, @NotNull EntityDimensions d, float pt) {
        if (isSyncedMinecartMode()) {
            return new Vec3(0, 1.2, 0);
        }
        return new Vec3(0, 1.0, 0);
    }

    // === NBT ===
    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.contains("BlockId")) entityData.set(BLOCK_ID, tag.getString("BlockId"));
        if (tag.contains("DisplayItem")) entityData.set(DISPLAY_ITEM, tag.getString("DisplayItem"));
        if (tag.contains("SwordItem"))
            swordStack = ItemStack.parseOptional(level().registryAccess(), tag.getCompound("SwordItem"));
        ironLevel = tag.getInt("IronLevel");
        diamondLevel = tag.getInt("DiamondLevel");
        netheriteLevel = tag.getInt("NetheriteLevel");
        maxUpgradeLimit = tag.contains("MaxUpLimit") ? tag.getInt("MaxUpLimit") : 10;
        fuelTicks = tag.getInt("FuelTicks");
        maxFuelTicks = tag.getInt("MaxFuelTicks");
        fuelPower = tag.getFloat("FuelPower");
        entityData.set(PITCH, tag.getFloat("Pitch"));
        entityData.set(ROLL, tag.getFloat("Roll"));
        entityData.set(YAW_OFFSET, tag.getFloat("YawOffset"));
        // Sync appearance (minecart mode, etc.) from loaded swordStack
        updateAppearanceFromStack();
        if (isSyncedMinecartMode()) {
            refreshDimensions();
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putString("BlockId", entityData.get(BLOCK_ID));
        tag.putString("DisplayItem", entityData.get(DISPLAY_ITEM));
        if (!swordStack.isEmpty())
            tag.put("SwordItem", swordStack.save(level().registryAccess()));
        tag.putInt("IronLevel", ironLevel);
        tag.putInt("DiamondLevel", diamondLevel);
        tag.putInt("NetheriteLevel", netheriteLevel);
        tag.putInt("MaxUpLimit", maxUpgradeLimit);
        tag.putInt("FuelTicks", fuelTicks);
        tag.putInt("MaxFuelTicks", maxFuelTicks);
        tag.putFloat("FuelPower", fuelPower);
        tag.putFloat("Pitch", entityData.get(PITCH));
        tag.putFloat("Roll", entityData.get(ROLL));
        tag.putFloat("YawOffset", entityData.get(YAW_OFFSET));
    }
}

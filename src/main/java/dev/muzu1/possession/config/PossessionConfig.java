package dev.muzu1.possession.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.muzu1.possession.PossessionMod;
import dev.muzu1.possession.control.PossessionControlState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PossessionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "possession.json";
    private static final PossessionConfig DEFAULTS = new PossessionConfig();

    public static final General GENERAL = new General();
    public static final Client CLIENT = new Client();
    public static final Goat GOAT = new Goat();
    public static final Forms FORMS = new Forms();
    public static final Behaviors BEHAVIORS = new Behaviors();

    private final General general = new General();
    private final Client client = new Client();
    private final Goat goat = new Goat();
    private final Forms forms = new Forms();
    private final Behaviors behaviors = new Behaviors();

    public static void loadOrCreate() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            save(configPath);
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            PossessionConfig loaded = GSON.fromJson(reader, PossessionConfig.class);
            if (loaded == null) {
                PossessionMod.LOGGER.warn("Config file {} was empty, using defaults", configPath);
                save(configPath);
                return;
            }
            applyLoadedValues(loaded);
            saveCurrent(configPath);
        } catch (IOException | JsonParseException exception) {
            PossessionMod.LOGGER.error("Failed to load config {}, keeping defaults", configPath, exception);
        }
    }

    private static void applyLoadedValues(PossessionConfig loaded) {
        copyGeneral(DEFAULTS.general, GENERAL);
        copyClient(DEFAULTS.client, CLIENT);
        copyGoat(DEFAULTS.goat, GOAT);
        copyForms(DEFAULTS.forms, FORMS);
        copyBehaviors(DEFAULTS.behaviors, BEHAVIORS);

        copyGeneral(loaded.general, GENERAL);
        copyClient(loaded.client, CLIENT);
        copyGoat(loaded.goat, GOAT);
        copyForms(loaded.forms, FORMS);
        copyBehaviors(loaded.behaviors, BEHAVIORS);
        sanitize();
    }

    private static void save(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(DEFAULTS, writer);
            }
        } catch (IOException exception) {
            PossessionMod.LOGGER.error("Failed to write default config {}", configPath, exception);
        }
    }

    private static void saveCurrent(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(snapshotCurrent(), writer);
            }
        } catch (IOException exception) {
            PossessionMod.LOGGER.error("Failed to write merged config {}", configPath, exception);
        }
    }

    private static void sanitize() {
        GENERAL.minRemainingHealth = Math.max(0.1F, GENERAL.minRemainingHealth);
        GENERAL.maxAttachDistance = Math.max(1.0D, GENERAL.maxAttachDistance);
        GENERAL.monsterPossessionHealthRatio = clamp01(GENERAL.monsterPossessionHealthRatio);
        GENERAL.targetDeathBacklashRatio = clamp01(GENERAL.targetDeathBacklashRatio);
        GENERAL.targetHealthBonusFlat = Math.max(0.0F, GENERAL.targetHealthBonusFlat);
        GENERAL.targetHealthBonusRatio = Math.max(0.0F, GENERAL.targetHealthBonusRatio);
        GENERAL.possessionDamageGraceTicks = Math.max(0, GENERAL.possessionDamageGraceTicks);

        CLIENT.hudLineHeight = Math.max(1, CLIENT.hudLineHeight);
        CLIENT.hudMessageDurationMs = Math.max(250L, CLIENT.hudMessageDurationMs);
        CLIENT.soulCharmModelScale = Math.max(0.01F, CLIENT.soulCharmModelScale);

        GOAT.maxChargeTicks = Math.max(1, GOAT.maxChargeTicks);
        GOAT.ramMinTicks = Math.max(1, GOAT.ramMinTicks);
        GOAT.ramMaxTicks = Math.max(GOAT.ramMinTicks, GOAT.ramMaxTicks);
        GOAT.maxJumpVerticalVelocity = Math.max(0.1D, GOAT.maxJumpVerticalVelocity);
        GOAT.maxJumpHorizontalVelocity = Math.max(0.0D, GOAT.maxJumpHorizontalVelocity);
        GOAT.minJumpHorizontalVelocity = Math.max(0.0D, Math.min(GOAT.minJumpHorizontalVelocity, GOAT.maxJumpHorizontalVelocity));
        GOAT.maxRamDistance = Math.max(0.0D, GOAT.maxRamDistance);
        GOAT.maxRamDamage = Math.max(0.0F, GOAT.maxRamDamage);
        GOAT.minRamSpeed = Math.max(0.0D, GOAT.minRamSpeed);
        GOAT.maxRamSpeed = Math.max(GOAT.minRamSpeed, GOAT.maxRamSpeed);
        GOAT.ramHitExpand = Math.max(0.0D, GOAT.ramHitExpand);
        GOAT.ramVerticalLift = Math.max(0.0D, GOAT.ramVerticalLift);
        GOAT.ramMinTrace = Math.max(0.0D, GOAT.ramMinTrace);
        GOAT.ramMaxTrace = Math.max(GOAT.ramMinTrace, GOAT.ramMaxTrace);

        sanitizeForms(FORMS);
        sanitizeBehaviors(BEHAVIORS);
    }

    private static void sanitizeForms(Forms forms) {
        sanitizeForm(forms.defaultForm);
        sanitizeForm(forms.creeper);
        sanitizeForm(forms.skeleton);
        sanitizeForm(forms.zombie);
        sanitizeForm(forms.frog);
        sanitizeForm(forms.dolphin);
        sanitizeForm(forms.squid);
        sanitizeForm(forms.slime);
        sanitizeForm(forms.spider);
        sanitizeForm(forms.camel);
        sanitizeForm(forms.goat);
        sanitizeForm(forms.enderman);
        sanitizeForm(forms.wolf);
    }

    private static void sanitizeBehaviors(Behaviors behaviors) {
        sanitizeCreeperBehavior(behaviors.creeper);
        sanitizeSkeletonBehavior(behaviors.skeleton);
        sanitizeFrogBehavior(behaviors.frog);
        sanitizeEndermanBehavior(behaviors.enderman);
        sanitizeCamelBehavior(behaviors.camel);
        sanitizeSquidBehavior(behaviors.squid);
        sanitizeSlimeBehavior(behaviors.slime);
        sanitizeSpiderBehavior(behaviors.spider);
    }

    private static void sanitizeCreeperBehavior(CreeperBehavior behavior) {
        behavior.explosionPower = Math.max(0.0F, behavior.explosionPower);
        behavior.sneakExplosionPower = Math.max(0.0F, behavior.sneakExplosionPower);
        behavior.selfDamageMultiplier = Math.max(0.0F, behavior.selfDamageMultiplier);
        behavior.smokeParticleCount = Math.max(0, behavior.smokeParticleCount);
        behavior.particleSpread = Math.max(0.0D, behavior.particleSpread);
        behavior.particleVelocity = Math.max(0.0D, behavior.particleVelocity);
    }

    private static void sanitizeSkeletonBehavior(SkeletonBehavior behavior) {
        behavior.arrowSpeed = Math.max(0.1F, behavior.arrowSpeed);
        behavior.arrowDivergence = Math.max(0.0F, behavior.arrowDivergence);
        behavior.minArrowDamage = Math.max(0.0D, behavior.minArrowDamage);
        behavior.critParticleCount = Math.max(0, behavior.critParticleCount);
        behavior.particleSpread = Math.max(0.0D, behavior.particleSpread);
        behavior.particleVelocity = Math.max(0.0D, behavior.particleVelocity);
        behavior.soundVolume = Math.max(0.0F, behavior.soundVolume);
        behavior.soundPitch = Math.max(0.0F, behavior.soundPitch);
    }

    private static void sanitizeFrogBehavior(FrogBehavior behavior) {
        behavior.swallowRange = Math.max(0.0D, behavior.swallowRange);
        behavior.spitForwardOffset = Math.max(0.0D, behavior.spitForwardOffset);
        behavior.spitUpOffset = Math.max(0.0D, behavior.spitUpOffset);
        behavior.spitForwardSpeed = Math.max(0.0D, behavior.spitForwardSpeed);
        behavior.spitUpSpeed = Math.max(0.0D, behavior.spitUpSpeed);
        behavior.sprintJumpBoost = Math.max(0.0D, behavior.sprintJumpBoost);
        behavior.minSprintJumpVerticalVelocity = Math.max(0.0D, behavior.minSprintJumpVerticalVelocity);
        behavior.eatParticleCount = Math.max(0, behavior.eatParticleCount);
        behavior.eatParticleSpreadX = Math.max(0.0D, behavior.eatParticleSpreadX);
        behavior.eatParticleSpreadY = Math.max(0.0D, behavior.eatParticleSpreadY);
        behavior.eatParticleSpreadZ = Math.max(0.0D, behavior.eatParticleSpreadZ);
        behavior.eatParticleVelocity = Math.max(0.0D, behavior.eatParticleVelocity);
    }

    private static void sanitizeEndermanBehavior(EndermanBehavior behavior) {
        behavior.teleportDistance = Math.max(0.0D, behavior.teleportDistance);
        behavior.minTeleportPitch = Math.max(-90.0F, Math.min(90.0F, behavior.minTeleportPitch));
        behavior.maxTeleportPitch = Math.max(behavior.minTeleportPitch, Math.min(90.0F, behavior.maxTeleportPitch));
        behavior.searchHorizontalRadius = Math.max(0, behavior.searchHorizontalRadius);
        behavior.searchUp = Math.max(0, behavior.searchUp);
        behavior.searchDown = Math.max(0, behavior.searchDown);
        behavior.portalParticleCount = Math.max(0, behavior.portalParticleCount);
        behavior.portalParticleSpread = Math.max(0.0D, behavior.portalParticleSpread);
        behavior.portalParticleVelocity = Math.max(0.0D, behavior.portalParticleVelocity);
        behavior.blockParticleCount = Math.max(0, behavior.blockParticleCount);
        behavior.blockParticleSpread = Math.max(0.0D, behavior.blockParticleSpread);
        behavior.blockParticleVelocity = Math.max(0.0D, behavior.blockParticleVelocity);
        behavior.pickupSoundVolume = Math.max(0.0F, behavior.pickupSoundVolume);
        behavior.pickupSoundPitch = Math.max(0.0F, behavior.pickupSoundPitch);
        behavior.placeSoundVolume = Math.max(0.0F, behavior.placeSoundVolume);
        behavior.placeSoundPitch = Math.max(0.0F, behavior.placeSoundPitch);
        behavior.teleportSoundVolume = Math.max(0.0F, behavior.teleportSoundVolume);
        behavior.teleportSoundPitch = Math.max(0.0F, behavior.teleportSoundPitch);
    }

    private static void sanitizeCamelBehavior(CamelBehavior behavior) {
        behavior.sandBoost = Math.max(0.0D, behavior.sandBoost);
    }

    private static void sanitizeSquidBehavior(SquidBehavior behavior) {
        behavior.forwardWaterPush = Math.max(0.0D, behavior.forwardWaterPush);
        behavior.upwardPush = Math.max(0.0D, behavior.upwardPush);
        behavior.downwardPush = Math.min(0.0D, behavior.downwardPush);
    }

    private static void sanitizeSlimeBehavior(SlimeBehavior behavior) {
        behavior.minJumpVerticalVelocity = Math.max(0.0D, behavior.minJumpVerticalVelocity);
        behavior.jumpSoundVolume = Math.max(0.0F, behavior.jumpSoundVolume);
        behavior.jumpSoundPitch = Math.max(0.0F, behavior.jumpSoundPitch);
    }

    private static void sanitizeSpiderBehavior(SpiderBehavior behavior) {
        behavior.wallClimbVerticalVelocity = Math.max(0.0D, behavior.wallClimbVerticalVelocity);
        behavior.wallClimbHorizontalDamping = Math.max(0.0D, Math.min(1.0D, behavior.wallClimbHorizontalDamping));
    }

    private static void sanitizeForm(FormProfile form) {
        if (form == null) {
            return;
        }
        if (form.lowHealthThresholdRatio != null) {
            form.lowHealthThresholdRatio = clamp01(form.lowHealthThresholdRatio);
        }
        form.attackWindupTicks = clampNonNegative(form.attackWindupTicks);
        form.attackCooldownTicks = clampNonNegative(form.attackCooldownTicks);
        form.sneakAttackCooldownTicks = clampNonNegative(form.sneakAttackCooldownTicks);
        form.abilityCooldownTicks = clampNonNegative(form.abilityCooldownTicks);
        form.useCooldownTicks = clampNonNegative(form.useCooldownTicks);
    }

    private static Integer clampNonNegative(Integer value) {
        return value == null ? null : Math.max(0, value);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static void copyGeneral(General source, General target) {
        if (source == null) {
            return;
        }
        target.minRemainingHealth = source.minRemainingHealth;
        target.maxAttachDistance = source.maxAttachDistance;
        target.monsterPossessionHealthRatio = source.monsterPossessionHealthRatio;
        target.targetDeathBacklashRatio = source.targetDeathBacklashRatio;
        target.targetHealthBonusFlat = source.targetHealthBonusFlat;
        target.targetHealthBonusRatio = source.targetHealthBonusRatio;
        target.possessionDamageGraceTicks = source.possessionDamageGraceTicks;
    }

    private static void copyClient(Client source, Client target) {
        if (source == null) {
            return;
        }
        target.hudX = source.hudX;
        target.hudY = source.hudY;
        target.hudLineHeight = source.hudLineHeight;
        target.hudMessageDurationMs = source.hudMessageDurationMs;
        target.soulCharmModelScale = source.soulCharmModelScale;
        target.soulCharmOffsetX = source.soulCharmOffsetX;
        target.soulCharmOffsetY = source.soulCharmOffsetY;
        target.soulCharmOffsetZ = source.soulCharmOffsetZ;
    }

    private static void copyGoat(Goat source, Goat target) {
        if (source == null) {
            return;
        }
        target.maxChargeTicks = source.maxChargeTicks;
        target.ramMinTicks = source.ramMinTicks;
        target.ramMaxTicks = source.ramMaxTicks;
        target.maxJumpVerticalVelocity = source.maxJumpVerticalVelocity;
        target.maxJumpHorizontalVelocity = source.maxJumpHorizontalVelocity;
        target.minJumpHorizontalVelocity = source.minJumpHorizontalVelocity;
        target.maxRamDistance = source.maxRamDistance;
        target.maxRamDamage = source.maxRamDamage;
        target.minRamSpeed = source.minRamSpeed;
        target.maxRamSpeed = source.maxRamSpeed;
        target.ramHitExpand = source.ramHitExpand;
        target.ramVerticalLift = source.ramVerticalLift;
        target.ramMinTrace = source.ramMinTrace;
        target.ramMaxTrace = source.ramMaxTrace;
    }

    private static void copyForms(Forms source, Forms target) {
        if (source == null) {
            return;
        }
        target.defaultForm.copyFrom(source.defaultForm);
        target.creeper.copyFrom(source.creeper);
        target.skeleton.copyFrom(source.skeleton);
        target.zombie.copyFrom(source.zombie);
        target.frog.copyFrom(source.frog);
        target.dolphin.copyFrom(source.dolphin);
        target.squid.copyFrom(source.squid);
        target.slime.copyFrom(source.slime);
        target.spider.copyFrom(source.spider);
        target.camel.copyFrom(source.camel);
        target.goat.copyFrom(source.goat);
        target.enderman.copyFrom(source.enderman);
        target.wolf.copyFrom(source.wolf);
    }

    private static void copyBehaviors(Behaviors source, Behaviors target) {
        if (source == null) {
            return;
        }
        target.creeper.copyFrom(source.creeper);
        target.skeleton.copyFrom(source.skeleton);
        target.frog.copyFrom(source.frog);
        target.enderman.copyFrom(source.enderman);
        target.camel.copyFrom(source.camel);
        target.squid.copyFrom(source.squid);
        target.slime.copyFrom(source.slime);
        target.spider.copyFrom(source.spider);
    }

    private static PossessionConfig snapshotCurrent() {
        PossessionConfig snapshot = new PossessionConfig();
        copyGeneral(GENERAL, snapshot.general);
        copyClient(CLIENT, snapshot.client);
        copyGoat(GOAT, snapshot.goat);
        copyForms(FORMS, snapshot.forms);
        copyBehaviors(BEHAVIORS, snapshot.behaviors);
        return snapshot;
    }

    public static final class General {
        public float minRemainingHealth = 1.0F;
        public double maxAttachDistance = 8.0D;
        public float monsterPossessionHealthRatio = 0.5F;
        public float targetDeathBacklashRatio = 1.0F / 3.0F;
        public float targetHealthBonusFlat = 4.0F;
        public float targetHealthBonusRatio = 0.25F;
        public int possessionDamageGraceTicks = 12;
    }

    public static final class Client {
        public int hudX = 8;
        public int hudY = 8;
        public int hudLineHeight = 10;
        public long hudMessageDurationMs = 5000L;
        public float soulCharmModelScale = 0.24F;
        public double soulCharmOffsetX = 0.0D;
        public double soulCharmOffsetY = -0.18D;
        public double soulCharmOffsetZ = -0.17D;
    }

    public static final class Goat {
        public int maxChargeTicks = 40;
        public int ramMinTicks = 7;
        public int ramMaxTicks = 14;
        public double maxJumpVerticalVelocity = 1.02D;
        public double maxJumpHorizontalVelocity = 0.98D;
        public double minJumpHorizontalVelocity = 0.08D;
        public double maxRamDistance = 10.0D;
        public float maxRamDamage = 16.0F;
        public double minRamSpeed = 0.72D;
        public double maxRamSpeed = 1.52D;
        public double ramHitExpand = 0.85D;
        public double ramVerticalLift = 0.03D;
        public double ramMinTrace = 0.7D;
        public double ramMaxTrace = 2.3D;
    }

    public static final class Forms {
        public final FormProfile defaultForm = FormProfile.of(0.05D, 2.0D, 0.2D);
        public final FormProfile creeper = FormProfile.of(0.07D, 0.0D, 0.05D)
                .withSneakMovementSpeedBonus(0.02D)
                .withAttackWindupTicks(0)
                .withAttackCooldownTicks(0)
                .withAbilityCooldownTicks(80);
        public final FormProfile skeleton = FormProfile.of(0.06D, 0.0D, 0.04D)
                .withAttackWindupTicks(0)
                .withAttackCooldownTicks(0)
                .withAbilityCooldownTicks(24);
        public final FormProfile zombie = FormProfile.of(0.04D, 0.0D, 0.15D)
                .withLowHealthMovementSpeedBonus(0.08D)
                .withLowHealthThresholdRatio(0.5F)
                .withAttackWindupTicks(2)
                .withAttackCooldownTicks(10)
                .withSneakAttackCooldownTicks(12);
        public final FormProfile frog = FormProfile.of(0.06D, 1.0D, 0.08D)
                .withGroundedMovementSpeedBonus(0.10D)
                .withAbilityCooldownTicks(20);
        public final FormProfile dolphin = FormProfile.of(0.06D, 0.5D, 0.05D);
        public final FormProfile squid = FormProfile.of(0.0D, 0.4D, 0.02D)
                .withSubmergedMovementSpeedBonus(0.12D)
                .withDryMovementSpeedBonus(-0.02D);
        public final FormProfile slime = FormProfile.of(0.04D, 2.0D, 0.28D);
        public final FormProfile spider = FormProfile.of(0.09D, 1.0D, 0.10D);
        public final FormProfile camel = FormProfile.of(0.08D, 1.2D, 0.18D);
        public final FormProfile goat = FormProfile.of(0.06D, 1.8D, 0.22D)
                .withSprintMovementSpeedBonus(0.12D)
                .withAbilityCooldownTicks(6);
        public final FormProfile enderman = FormProfile.of(0.06D, 2.8D, 0.12D)
                .withSprintMovementSpeedBonus(0.10D)
                .withAbilityCooldownTicks(16)
                .withUseCooldownTicks(10);
        public final FormProfile wolf = FormProfile.of(0.10D, 2.5D, 0.12D);

        public FormProfile profile(String key) {
            return switch (key) {
                case "creeper" -> creeper;
                case "skeleton" -> skeleton;
                case "zombie" -> zombie;
                case "frog" -> frog;
                case "dolphin" -> dolphin;
                case "squid" -> squid;
                case "slime" -> slime;
                case "spider" -> spider;
                case "camel" -> camel;
                case "goat" -> goat;
                case "enderman" -> enderman;
                case "wolf" -> wolf;
                case "default" -> defaultForm;
                default -> defaultForm;
            };
        }
    }

    public static final class Behaviors {
        public final CreeperBehavior creeper = new CreeperBehavior();
        public final SkeletonBehavior skeleton = new SkeletonBehavior();
        public final FrogBehavior frog = new FrogBehavior();
        public final EndermanBehavior enderman = new EndermanBehavior();
        public final CamelBehavior camel = new CamelBehavior();
        public final SquidBehavior squid = new SquidBehavior();
        public final SlimeBehavior slime = new SlimeBehavior();
        public final SpiderBehavior spider = new SpiderBehavior();
    }

    public static final class CreeperBehavior {
        public float explosionPower = 3.0F;
        public float sneakExplosionPower = 4.0F;
        public float selfDamageMultiplier = 4.0F;
        public int smokeParticleCount = 24;
        public double particleSpread = 0.55D;
        public double particleVelocity = 0.03D;

        public void copyFrom(CreeperBehavior source) {
            if (source == null) {
                return;
            }
            explosionPower = source.explosionPower;
            sneakExplosionPower = source.sneakExplosionPower;
            selfDamageMultiplier = source.selfDamageMultiplier;
            smokeParticleCount = source.smokeParticleCount;
            particleSpread = source.particleSpread;
            particleVelocity = source.particleVelocity;
        }
    }

    public static final class SkeletonBehavior {
        public float arrowSpeed = 2.8F;
        public float arrowDivergence = 0.05F;
        public double minArrowDamage = 3.0D;
        public int critParticleCount = 14;
        public double particleSpread = 0.22D;
        public double particleVelocity = 0.03D;
        public float soundVolume = 1.0F;
        public float soundPitch = 0.9F;

        public void copyFrom(SkeletonBehavior source) {
            if (source == null) {
                return;
            }
            arrowSpeed = source.arrowSpeed;
            arrowDivergence = source.arrowDivergence;
            minArrowDamage = source.minArrowDamage;
            critParticleCount = source.critParticleCount;
            particleSpread = source.particleSpread;
            particleVelocity = source.particleVelocity;
            soundVolume = source.soundVolume;
            soundPitch = source.soundPitch;
        }
    }

    public static final class FrogBehavior {
        public double swallowRange = 5.0D;
        public double spitForwardOffset = 1.35D;
        public double spitUpOffset = 0.15D;
        public double spitForwardSpeed = 0.46D;
        public double spitUpSpeed = 0.18D;
        public double sprintJumpBoost = 0.18D;
        public double minSprintJumpVerticalVelocity = 0.48D;
        public int eatParticleCount = 8;
        public double eatParticleSpreadX = 0.25D;
        public double eatParticleSpreadY = 0.2D;
        public double eatParticleSpreadZ = 0.25D;
        public double eatParticleVelocity = 0.02D;

        public void copyFrom(FrogBehavior source) {
            if (source == null) {
                return;
            }
            swallowRange = source.swallowRange;
            spitForwardOffset = source.spitForwardOffset;
            spitUpOffset = source.spitUpOffset;
            spitForwardSpeed = source.spitForwardSpeed;
            spitUpSpeed = source.spitUpSpeed;
            sprintJumpBoost = source.sprintJumpBoost;
            minSprintJumpVerticalVelocity = source.minSprintJumpVerticalVelocity;
            eatParticleCount = source.eatParticleCount;
            eatParticleSpreadX = source.eatParticleSpreadX;
            eatParticleSpreadY = source.eatParticleSpreadY;
            eatParticleSpreadZ = source.eatParticleSpreadZ;
            eatParticleVelocity = source.eatParticleVelocity;
        }
    }

    public static final class EndermanBehavior {
        public double teleportDistance = 16.0D;
        public float minTeleportPitch = -65.0F;
        public float maxTeleportPitch = 35.0F;
        public int searchHorizontalRadius = 2;
        public int searchUp = 4;
        public int searchDown = 12;
        public int portalParticleCount = 36;
        public double portalParticleSpread = 0.55D;
        public double portalParticleVelocity = 0.06D;
        public int blockParticleCount = 18;
        public double blockParticleSpread = 0.35D;
        public double blockParticleVelocity = 0.03D;
        public float pickupSoundVolume = 0.35F;
        public float pickupSoundPitch = 1.35F;
        public float placeSoundVolume = 0.45F;
        public float placeSoundPitch = 1.25F;
        public float teleportSoundVolume = 1.0F;
        public float teleportSoundPitch = 1.0F;

        public void copyFrom(EndermanBehavior source) {
            if (source == null) {
                return;
            }
            teleportDistance = source.teleportDistance;
            minTeleportPitch = source.minTeleportPitch;
            maxTeleportPitch = source.maxTeleportPitch;
            searchHorizontalRadius = source.searchHorizontalRadius;
            searchUp = source.searchUp;
            searchDown = source.searchDown;
            portalParticleCount = source.portalParticleCount;
            portalParticleSpread = source.portalParticleSpread;
            portalParticleVelocity = source.portalParticleVelocity;
            blockParticleCount = source.blockParticleCount;
            blockParticleSpread = source.blockParticleSpread;
            blockParticleVelocity = source.blockParticleVelocity;
            pickupSoundVolume = source.pickupSoundVolume;
            pickupSoundPitch = source.pickupSoundPitch;
            placeSoundVolume = source.placeSoundVolume;
            placeSoundPitch = source.placeSoundPitch;
            teleportSoundVolume = source.teleportSoundVolume;
            teleportSoundPitch = source.teleportSoundPitch;
        }
    }

    public static final class CamelBehavior {
        public double sandBoost = 0.04D;

        public void copyFrom(CamelBehavior source) {
            if (source == null) {
                return;
            }
            sandBoost = source.sandBoost;
        }
    }

    public static final class SquidBehavior {
        public double forwardWaterPush = 0.09D;
        public double upwardPush = 0.03D;
        public double downwardPush = -0.02D;

        public void copyFrom(SquidBehavior source) {
            if (source == null) {
                return;
            }
            forwardWaterPush = source.forwardWaterPush;
            upwardPush = source.upwardPush;
            downwardPush = source.downwardPush;
        }
    }

    public static final class SlimeBehavior {
        public double minJumpVerticalVelocity = 0.54D;
        public float jumpSoundVolume = 0.8F;
        public float jumpSoundPitch = 1.0F;

        public void copyFrom(SlimeBehavior source) {
            if (source == null) {
                return;
            }
            minJumpVerticalVelocity = source.minJumpVerticalVelocity;
            jumpSoundVolume = source.jumpSoundVolume;
            jumpSoundPitch = source.jumpSoundPitch;
        }
    }

    public static final class SpiderBehavior {
        public double wallClimbVerticalVelocity = 0.20D;
        public double wallClimbHorizontalDamping = 0.85D;

        public void copyFrom(SpiderBehavior source) {
            if (source == null) {
                return;
            }
            wallClimbVerticalVelocity = source.wallClimbVerticalVelocity;
            wallClimbHorizontalDamping = source.wallClimbHorizontalDamping;
        }
    }

    public static final class FormProfile {
        public Double movementSpeedBonus;
        public Double sprintMovementSpeedBonus;
        public Double sneakMovementSpeedBonus;
        public Double groundedMovementSpeedBonus;
        public Double airborneMovementSpeedBonus;
        public Double submergedMovementSpeedBonus;
        public Double dryMovementSpeedBonus;
        public Double lowHealthMovementSpeedBonus;
        public Float lowHealthThresholdRatio;
        public Double attackDamageBonus;
        public Double knockbackResistanceBonus;
        public Integer attackWindupTicks;
        public Integer attackCooldownTicks;
        public Integer sneakAttackCooldownTicks;
        public Integer abilityCooldownTicks;
        public Integer useCooldownTicks;

        public static FormProfile of(double movementSpeedBonus, double attackDamageBonus, double knockbackResistanceBonus) {
            FormProfile profile = new FormProfile();
            profile.movementSpeedBonus = movementSpeedBonus;
            profile.attackDamageBonus = attackDamageBonus;
            profile.knockbackResistanceBonus = knockbackResistanceBonus;
            return profile;
        }

        public FormProfile withSprintMovementSpeedBonus(double value) {
            this.sprintMovementSpeedBonus = value;
            return this;
        }

        public FormProfile withSneakMovementSpeedBonus(double value) {
            this.sneakMovementSpeedBonus = value;
            return this;
        }

        public FormProfile withGroundedMovementSpeedBonus(double value) {
            this.groundedMovementSpeedBonus = value;
            return this;
        }

        public FormProfile withSubmergedMovementSpeedBonus(double value) {
            this.submergedMovementSpeedBonus = value;
            return this;
        }

        public FormProfile withDryMovementSpeedBonus(double value) {
            this.dryMovementSpeedBonus = value;
            return this;
        }

        public FormProfile withLowHealthMovementSpeedBonus(double value) {
            this.lowHealthMovementSpeedBonus = value;
            return this;
        }

        public FormProfile withLowHealthThresholdRatio(float value) {
            this.lowHealthThresholdRatio = value;
            return this;
        }

        public FormProfile withAttackWindupTicks(int value) {
            this.attackWindupTicks = value;
            return this;
        }

        public FormProfile withAttackCooldownTicks(int value) {
            this.attackCooldownTicks = value;
            return this;
        }

        public FormProfile withSneakAttackCooldownTicks(int value) {
            this.sneakAttackCooldownTicks = value;
            return this;
        }

        public FormProfile withAbilityCooldownTicks(int value) {
            this.abilityCooldownTicks = value;
            return this;
        }

        public FormProfile withUseCooldownTicks(int value) {
            this.useCooldownTicks = value;
            return this;
        }

        public double resolveMovementSpeedBonus(LivingEntity target, PossessionControlState control, MovementFallback fallback) {
            double resolved = coalesce(movementSpeedBonus, fallback.baseBonus());
            boolean inWater = target.isTouchingWater() || target.isSubmergedInWater();
            if (inWater) {
                resolved = coalesce(submergedMovementSpeedBonus, fallback.submergedBonus(), resolved);
            } else {
                resolved = coalesce(dryMovementSpeedBonus, fallback.dryBonus(), resolved);
            }

            if (target.isOnGround()) {
                resolved = coalesce(groundedMovementSpeedBonus, fallback.groundedBonus(), resolved);
            } else {
                resolved = coalesce(airborneMovementSpeedBonus, fallback.airborneBonus(), resolved);
            }

            float healthThresholdRatio = coalesce(lowHealthThresholdRatio, fallback.lowHealthThresholdRatio(), 0.5F);
            if (target.getMaxHealth() > 0.0F && target.getHealth() <= target.getMaxHealth() * healthThresholdRatio) {
                resolved = coalesce(lowHealthMovementSpeedBonus, fallback.lowHealthBonus(), resolved);
            }

            if (control.sneak()) {
                resolved = coalesce(sneakMovementSpeedBonus, fallback.sneakBonus(), resolved);
            }
            if (control.sprint()) {
                resolved = coalesce(sprintMovementSpeedBonus, fallback.sprintBonus(), resolved);
            }
            return resolved;
        }

        public double resolveAttackDamageBonus(StatFallback fallback) {
            return coalesce(attackDamageBonus, fallback.attackDamageBonus());
        }

        public double resolveKnockbackResistanceBonus(StatFallback fallback) {
            return coalesce(knockbackResistanceBonus, fallback.knockbackResistanceBonus());
        }

        public int resolveAttackWindupTicks(ActionFallback fallback) {
            return coalesce(attackWindupTicks, fallback.attackWindupTicks());
        }

        public int resolveAttackCooldownTicks(PossessionControlState control, ActionFallback fallback) {
            if (control.sneak()) {
                Integer configuredSneak = coalesceNullable(sneakAttackCooldownTicks, fallback.sneakAttackCooldownTicks());
                if (configuredSneak != null) {
                    return configuredSneak;
                }
            }
            return coalesce(attackCooldownTicks, fallback.attackCooldownTicks());
        }

        public int resolveAbilityCooldownTicks(ActionFallback fallback) {
            return coalesce(abilityCooldownTicks, fallback.abilityCooldownTicks());
        }

        public int resolveUseCooldownTicks(ActionFallback fallback) {
            return coalesce(useCooldownTicks, fallback.useCooldownTicks());
        }

        public void copyFrom(FormProfile source) {
            if (source == null) {
                return;
            }
            movementSpeedBonus = source.movementSpeedBonus;
            sprintMovementSpeedBonus = source.sprintMovementSpeedBonus;
            sneakMovementSpeedBonus = source.sneakMovementSpeedBonus;
            groundedMovementSpeedBonus = source.groundedMovementSpeedBonus;
            airborneMovementSpeedBonus = source.airborneMovementSpeedBonus;
            submergedMovementSpeedBonus = source.submergedMovementSpeedBonus;
            dryMovementSpeedBonus = source.dryMovementSpeedBonus;
            lowHealthMovementSpeedBonus = source.lowHealthMovementSpeedBonus;
            lowHealthThresholdRatio = source.lowHealthThresholdRatio;
            attackDamageBonus = source.attackDamageBonus;
            knockbackResistanceBonus = source.knockbackResistanceBonus;
            attackWindupTicks = source.attackWindupTicks;
            attackCooldownTicks = source.attackCooldownTicks;
            sneakAttackCooldownTicks = source.sneakAttackCooldownTicks;
            abilityCooldownTicks = source.abilityCooldownTicks;
            useCooldownTicks = source.useCooldownTicks;
        }

        private static double coalesce(Double primary, double fallback) {
            return primary != null ? primary : fallback;
        }

        private static double coalesce(Double primary, Double secondary, double fallback) {
            if (primary != null) {
                return primary;
            }
            if (secondary != null) {
                return secondary;
            }
            return fallback;
        }

        private static float coalesce(Float primary, Float secondary, float fallback) {
            if (primary != null) {
                return primary;
            }
            if (secondary != null) {
                return secondary;
            }
            return fallback;
        }

        private static int coalesce(Integer primary, int fallback) {
            return primary != null ? primary : fallback;
        }

        private static Integer coalesceNullable(Integer primary, Integer secondary) {
            return primary != null ? primary : secondary;
        }

        public record MovementFallback(double baseBonus,
                                       Double sprintBonus,
                                       Double sneakBonus,
                                       Double groundedBonus,
                                       Double airborneBonus,
                                       Double submergedBonus,
                                       Double dryBonus,
                                       Double lowHealthBonus,
                                       Float lowHealthThresholdRatio) {
        }

        public record StatFallback(double attackDamageBonus, double knockbackResistanceBonus) {
        }

        public record ActionFallback(int attackWindupTicks,
                                     int attackCooldownTicks,
                                     Integer sneakAttackCooldownTicks,
                                     int abilityCooldownTicks,
                                     int useCooldownTicks) {
        }
    }
}

package com.amatis.events;

import com.amatis.config.AmatisConfig;
import com.amatis.item.AmatisShieldItem;
import com.amatis.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Eventos del mod (Fabric):
 *  - Escudo: barrera 3x3 que anula el dano de cualquier direccion
 *  - Golpe pesado con onda de choque cada N golpes (espada pesada y guadana)
 * Las recetas de yunque estan en AnvilLogic / AnvilMenuMixin.
 */
public final class AmatisEvents {

    /** Por jugador: [golpes seguidos, tick del ultimo golpe]. */
    private static final Map<UUID, long[]> HIT_COUNTER = new HashMap<>();
    /** Evita que el dano extra / de la onda de choque cuente como un golpe nuevo. */
    private static boolean busy = false;

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(AmatisEvents::allowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(AmatisEvents::afterDamage);
        ServerTickEvents.END_SERVER_TICK.register(AmatisEvents::onServerTick);
    }

    // ================================================================== escudo: anula el dano

    /** @return false para cancelar el dano. */
    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) return true;
        if (!AmatisConfig.get().shieldBarrierEnabled) return true;
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof AmatisShieldItem)) return true;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        if (!(player.level() instanceof ServerLevel level)) return true;

        ItemStack shield = player.getUseItem();
        int cost = (int) Math.max(1.0, Math.ceil(amount / 4.0f * AmatisConfig.get().shieldDurabilityPerHit));
        EquipmentSlot slot = player.getUsedItemHand() == InteractionHand.OFF_HAND
                ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        shield.hurtAndBreak(cost, player, slot);
        barrierBurst(level, player);
        return false;
    }

    // ================================================================== golpe pesado

    private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken,
                                    float damageTaken, boolean blocked) {
        if (busy) return;
        AmatisConfig cfg = AmatisConfig.get();
        if (!cfg.heavyHitEnabled) return;
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return;
        if (!(source.getDirectEntity() instanceof ServerPlayer attacker)) return;

        ItemStack held = attacker.getMainHandItem();
        if (!held.is(ModItems.HEAVY_SWORD) && !held.is(ModItems.SCYTHE)) return;
        if (!(attacker.level() instanceof ServerLevel level)) return;

        long now = level.getGameTime();
        long[] counter = HIT_COUNTER.computeIfAbsent(attacker.getUUID(), id -> new long[]{0L, now});
        if (now - counter[1] > 60L) counter[0] = 0L; // si pasas 3 s sin pegar, se reinicia
        counter[0]++;
        counter[1] = now;
        if (counter[0] < cfg.heavyHitEvery) return;
        counter[0] = 0L;

        float boosted = (float) (damageTaken * cfg.heavyHitDamageMultiplier);
        float extra = boosted - damageTaken;
        busy = true;
        try {
            // el golpe pesado pega mas fuerte: se aplica la diferencia como un segundo impacto
            if (extra > 0.0f && entity.isAlive()) {
                entity.invulnerableTime = 0;
                entity.hurtServer(level, source, extra);
            }
            shockwave(level, attacker, entity, boosted);
        } finally {
            busy = false;
        }
    }

    // ================================================================== escudo: efectos continuos

    private static void onServerTick(MinecraftServer server) {
        if (!AmatisConfig.get().shieldBarrierEnabled) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof AmatisShieldItem)) continue;
            if (!(player.level() instanceof ServerLevel level)) continue;

            if (player.getTicksUsingItem() == 1) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
            if (player.tickCount % 3 != 0) continue;

            // contorno de la barrera: cuadrado de 3x3 alrededor del jugador, 2 bloques de alto
            RandomSource random = player.getRandom();
            for (int i = 0; i < 10; i++) {
                double t = random.nextDouble() * 4.0; // posicion sobre el perimetro
                double x, z;
                if (t < 1.0) {
                    x = -1.5 + t * 3.0;
                    z = -1.5;
                } else if (t < 2.0) {
                    x = 1.5;
                    z = -1.5 + (t - 1.0) * 3.0;
                } else if (t < 3.0) {
                    x = 1.5 - (t - 2.0) * 3.0;
                    z = 1.5;
                } else {
                    x = -1.5;
                    z = 1.5 - (t - 3.0) * 3.0;
                }
                double y = 0.1 + random.nextDouble() * 2.0;
                level.sendParticles(random.nextBoolean() ? ParticleTypes.WITCH : ParticleTypes.END_ROD,
                        player.getX() + x, player.getY() + y, player.getZ() + z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /** Destello al absorber un golpe. */
    private static void barrierBurst(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(),
                24, 1.2, 0.8, 1.2, 0.3);
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(),
                16, 1.2, 0.8, 1.2, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    // ================================================================== onda de choque

    private static void shockwave(ServerLevel level, ServerPlayer owner, LivingEntity center, float hitDamage) {
        AmatisConfig cfg = AmatisConfig.get();
        double radius = cfg.shockwaveRadius;
        float damage = (float) (hitDamage * cfg.shockwaveDamageRatio);
        Vec3 c = center.position();

        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(radius, 1.5, radius),
                e -> e != owner && e != center && e.isAlive() && !e.isSpectator()
                        && owner.canAttack(e) && !e.isAlliedTo(owner)
                        && !(e instanceof Player other && !owner.canHarmPlayer(other))
                        && e.distanceToSqr(c) <= radius * radius);

        for (LivingEntity victim : victims) {
            victim.hurtServer(level, level.damageSources().playerAttack(owner), damage);
            victim.knockback(cfg.shockwaveKnockback, c.x - victim.getX(), c.z - victim.getZ());
        }

        // efectos: anillo de particulas que se expande desde el suelo + escombros
        BlockState ground = level.getBlockState(center.blockPosition().below());
        for (int ring = 1; ring <= 3; ring++) {
            double r = radius * ring / 3.0;
            for (int deg = 0; deg < 360; deg += 20) {
                double rad = Math.toRadians(deg);
                double px = c.x + Math.cos(rad) * r;
                double pz = c.z + Math.sin(rad) * r;
                level.sendParticles(ParticleTypes.WITCH, px, c.y + 0.15, pz, 1, 0.05, 0.05, 0.05, 0.0);
                if (!ground.isAir()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                            px, c.y + 0.1, pz, 2, 0.15, 0.1, 0.15, 0.05);
                }
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.2, c.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9f, 0.6f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.5f);
    }

    private AmatisEvents() {
    }
}

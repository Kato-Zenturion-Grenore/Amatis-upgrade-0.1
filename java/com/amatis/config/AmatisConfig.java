package com.amatis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuracion del mod: config/amatis_upgrade.json (se crea sola al primer arranque).
 * Fabric no trae sistema de config propio, asi que es un JSON simple.
 */
public final class AmatisConfig {
    // ---- Yunque ----
    /** Fragmentos sobrecargados que da 1 amatista descargada + 1 aliento de dragon. */
    public int fragmentsPerCraft = 2;
    public int xpTemplate = 5;
    public int xpFragments = 8;
    public int xpHeavySword = 15;
    public int heavySwordFragments = 3;
    /** Nivel minimo de Filo de la espada de netherita para hacer la espada pesada. */
    public int heavySwordMinSharpness = 5;

    // ---- Golpe pesado (espada pesada y guadana) ----
    public boolean heavyHitEnabled = true;
    /** Cada cuantos golpes seguidos se hace el golpe pesado. */
    public int heavyHitEvery = 3;
    public double heavyHitDamageMultiplier = 1.5;
    public double shockwaveRadius = 3.5;
    /** Dano de la onda = dano del golpe x este valor (bajo a proposito). */
    public double shockwaveDamageRatio = 0.25;
    public double shockwaveKnockback = 0.7;

    // ---- Escudo ----
    public boolean shieldBarrierEnabled = true;
    /** Durabilidad que pierde por golpe bloqueado (x dano / 4, minimo 1). */
    public double shieldDurabilityPerHit = 1.0;

    private static AmatisConfig instance = new AmatisConfig();

    public static AmatisConfig get() {
        return instance;
    }

    public static void load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path path = FabricLoader.getInstance().getConfigDir().resolve("amatis_upgrade.json");
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    AmatisConfig loaded = gson.fromJson(reader, AmatisConfig.class);
                    if (loaded != null) {
                        instance = loaded;
                    }
                }
            }
            instance.sanitize();
            // se reescribe para anadir las opciones nuevas si faltaban
            try (Writer writer = Files.newBufferedWriter(path)) {
                gson.toJson(instance, writer);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[amatis_upgrade] No se pudo leer la config, se usan valores por defecto: " + e);
            instance = new AmatisConfig();
        }
    }

    private void sanitize() {
        fragmentsPerCraft = clamp(fragmentsPerCraft, 1, 64);
        xpTemplate = clamp(xpTemplate, 1, 40);
        xpFragments = clamp(xpFragments, 1, 40);
        xpHeavySword = clamp(xpHeavySword, 1, 40);
        heavySwordFragments = clamp(heavySwordFragments, 1, 64);
        heavySwordMinSharpness = clamp(heavySwordMinSharpness, 1, 255);
        heavyHitEvery = clamp(heavyHitEvery, 2, 20);
        heavyHitDamageMultiplier = Math.max(1.0, heavyHitDamageMultiplier);
        shockwaveRadius = Math.max(1.0, Math.min(12.0, shockwaveRadius));
        shockwaveDamageRatio = Math.max(0.0, shockwaveDamageRatio);
        shockwaveKnockback = Math.max(0.0, shockwaveKnockback);
        shieldDurabilityPerHit = Math.max(0.0, shieldDurabilityPerHit);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

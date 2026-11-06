package com.amatis;

import com.amatis.config.AmatisConfig;
import com.amatis.events.AmatisEvents;
import com.amatis.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Amatis Upgrade (Fabric): el nivel siguiente a la netherita (x2.9 de sus estadisticas).
 */
public class AmatisUpgrade implements ModInitializer {
    public static final String MOD_ID = "amatis_upgrade";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AmatisConfig.load();
        ModItems.init();
        AmatisEvents.register();
        LOGGER.info("[Amatis Upgrade] cargado: items y pestana creativa registrados");
    }
}

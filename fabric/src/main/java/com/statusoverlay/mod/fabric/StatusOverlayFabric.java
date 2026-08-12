package com.statusoverlay.mod.fabric;

import com.statusoverlay.mod.Constants;
import com.statusoverlay.mod.StatusOverlayMod;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric's {@code main} entrypoint — runs on <b>both</b> the physical client
 * and the dedicated server.
 *
 * <p>Declared in {@code fabric.mod.json} under {@code entrypoints.main}.
 *
 * <p>Everything here must be side-safe. Touching a {@code net.minecraft.client}
 * class from this file crashes a dedicated server with
 * {@code NoClassDefFoundError}. Client behaviour — which is where all of this
 * mod's actual features live — goes in
 * {@code com.statusoverlay.mod.fabric.client.StatusOverlayFabricClient} instead,
 * which lives in the {@code src/client} source set and physically cannot be
 * loaded on a server. {@code javadoc} cannot {@code @link} across that split
 * (this class's compilation genuinely cannot see that one), which is why this
 * is {@code @code} rather than {@code @link}.
 */
public final class StatusOverlayFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        StatusOverlayMod.init();
        Constants.LOG.info("{} loaded on Fabric.", Constants.MOD_NAME);
    }
}

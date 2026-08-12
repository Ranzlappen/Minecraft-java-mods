package com.statusoverlay.mod.fabric.client;

import com.statusoverlay.mod.Constants;
import com.statusoverlay.mod.overlay.OverlayFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Fabric's {@code client} entrypoint — runs on the physical client only.
 *
 * <p><b>Note the source set.</b> This file is in {@code src/client/java}, not
 * {@code src/main/java}. That is not a naming convention: Loom's
 * {@code splitEnvironmentSourceSets()} compiles this source set against the
 * client-only half of Minecraft, and excludes it from the server's view. The
 * practical effect is that calling client code from {@code src/main} is a
 * <i>compile error</i> rather than a crash on somebody's server three weeks
 * after release.
 *
 * <p>NeoForge has no equivalent split. There, the same guarantee comes from
 * {@code @Mod(dist = Dist.CLIENT)} plus discipline — see
 * {@code docs/02-sides-client-vs-server.md#the-two-loaders-differ}. The two
 * HUD elements registered here have a NeoForge equivalent in
 * {@code neoforge/.../client/StatusOverlayNeoForgeClient.java}, deliberately
 * written to read almost identically — the render logic is the same, only the
 * registration API differs.
 *
 * <p>Both elements are registered with {@link HudElementRegistry#addLast}
 * rather than attached relative to a vanilla layer: {@code addLast} does not
 * inherit a vanilla render condition, so this class alone decides when each
 * element is visible (spectator check, mount-type check). That is more code
 * than borrowing a vanilla condition, but it means the visibility rule is not
 * a guess about what some other layer's condition happens to cover.
 */
public final class StatusOverlayFabricClient implements ClientModInitializer {

    private static final int TEXT_COLOR = 0xFFFFFFFF;

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "saturation"),
                StatusOverlayFabricClient::renderSaturation);
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "horse_speed"),
                StatusOverlayFabricClient::renderHorseSpeed);

        Constants.LOG.info("{} client-side overlays active.", Constants.MOD_NAME);
    }

    /** Always visible outside spectator mode — food saturation applies to every player. */
    private static void renderSaturation(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        final Minecraft client = Minecraft.getInstance();
        final LocalPlayer player = client.player;
        if (player == null || player.isSpectator()) {
            return;
        }

        final String text = "Saturation: " + OverlayFormat.oneDecimal(player.getFoodData().getSaturationLevel());
        graphics.text(client.font, text, 4, 4, TEXT_COLOR, true);
    }

    /** Only visible while mounted on a horse, donkey, mule, or llama. */
    private static void renderHorseSpeed(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        final Minecraft client = Minecraft.getInstance();
        final LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        final Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof AbstractHorse horse)) {
            return;
        }

        final double blocksPerSecond = OverlayFormat.blocksPerSecond(horse.getDeltaMovement().horizontalDistance());
        final String text = "Speed: " + OverlayFormat.oneDecimal(blocksPerSecond) + " blocks/s";
        graphics.text(client.font, text, 4, 16, TEXT_COLOR, true);
    }
}

package com.statusoverlay.mod.neoforge.client;

import com.statusoverlay.mod.Constants;
import com.statusoverlay.mod.overlay.OverlayFormat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * NeoForge's client-only entrypoint.
 *
 * <p>{@code dist = Dist.CLIENT} means NeoForge never even loads this class on a
 * dedicated server. That is what makes it safe to reference
 * {@code net.minecraft.client} classes from here — and unsafe to reference this
 * class from {@link com.statusoverlay.mod.neoforge.StatusOverlayNeoForge}.
 *
 * <p><b>This is weaker than Fabric's split source sets.</b> Fabric makes the
 * mistake a compile error; NeoForge only makes it a runtime crash on a server
 * you may not be testing on. The mitigation is to actually run
 * {@code ./gradlew :neoforge:runServer} before every release — the CI workflow
 * in {@code .github/workflows/build.yml} enforces that the jar at least loads
 * on a dedicated server.
 *
 * <p>The render logic here is deliberately near-identical to Fabric's
 * {@code StatusOverlayFabricClient} — same visibility rules, same math via
 * {@link OverlayFormat}, same {@code GuiGraphicsExtractor}/{@code DeltaTracker}
 * types, because both loaders' GUI layer callback is the same underlying
 * vanilla {@code LayeredDraw.Layer} interface. Only the registration call
 * differs: {@code RegisterGuiLayersEvent} here vs {@code HudElementRegistry}
 * on Fabric.
 */
@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public final class StatusOverlayNeoForgeClient {

    private static final int TEXT_COLOR = 0xFFFFFFFF;

    /**
     * {@code RegisterGuiLayersEvent} is a <b>mod-bus</b> event — it configures
     * the client's HUD once at startup, unlike gameplay events (commands,
     * ticks) which fire on the game bus. Registering the listener on
     * {@code modBus} directly, rather than through {@code @EventBusSubscriber}'s
     * default (game) bus, is what makes this actually fire — see
     * {@code docs/01-architecture.md#neoforges-two-event-buses}.
     */
    public StatusOverlayNeoForgeClient(IEventBus modBus) {
        modBus.addListener(StatusOverlayNeoForgeClient::onRegisterGuiLayers);
        Constants.LOG.info("{} client-side overlays active.", Constants.MOD_NAME);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "saturation"),
                StatusOverlayNeoForgeClient::renderSaturation);
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "horse_speed"),
                StatusOverlayNeoForgeClient::renderHorseSpeed);
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

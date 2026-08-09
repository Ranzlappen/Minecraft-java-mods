package com.example.examplemod.neoforge.client;

import com.example.examplemod.Constants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge's client-only entrypoint.
 *
 * <p>{@code dist = Dist.CLIENT} means NeoForge never even loads this class on a
 * dedicated server. That is what makes it safe to reference
 * {@code net.minecraft.client} classes from here — and unsafe to reference this
 * class from {@link com.example.examplemod.neoforge.ExampleModNeoForge}.
 *
 * <p><b>This is weaker than Fabric's split source sets.</b> Fabric makes the
 * mistake a compile error; NeoForge only makes it a runtime crash on a server
 * you may not be testing on. The mitigation is to actually run
 * {@code ./gradlew :neoforge:runServer} before every release — the CI workflow
 * in {@code .github/workflows/build.yml} enforces that the jar at least loads
 * on a dedicated server.
 *
 * <p>Multiple {@code @Mod} classes may share one mod id. That is intentional
 * and is the documented way to separate per-dist code.
 */
@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public final class ExampleModNeoForgeClient {

    public ExampleModNeoForgeClient(IEventBus modBus) {
        Constants.LOG.info("{} client-side extras active.", Constants.MOD_NAME);

        // Client-side things that would go here:
        //
        //   - Keybinds (RegisterKeyMappingsEvent on the mod bus)
        //   - Screens and HUD layers (RegisterGuiLayersEvent)
        //   - Renderers (EntityRenderersEvent)
        //   - Handlers for payloads the server sends
        //     (RegisterClientPayloadHandlersEvent)
        //
        // Register the payload *type* in the common entrypoint and only the
        // client handler here. Worked example: docs/08-networking.md
    }
}

package com.statusoverlay.mod.platform;

import java.nio.file.Path;

/**
 * The seam between loader-agnostic code and loader-specific code.
 *
 * <p>Common code cannot call Fabric or NeoForge APIs — it does not compile
 * against them. When common code needs an answer only the loader knows ("is
 * mod X installed?", "where is the config folder?"), it asks through this
 * interface, and each loader subproject supplies its own implementation.
 *
 * <p><b>How to add a method.</b> Add it here, then implement it in
 * {@code fabric/.../FabricPlatformHelper} and
 * {@code neoforge/.../NeoForgePlatformHelper}. Forgetting one of the two is a
 * compile error in that subproject, so the build catches it.
 *
 * <p><b>What does NOT belong here.</b> Anything you can express with plain
 * Minecraft classes. Every method added here is code that must be written
 * twice, so keep the surface small — see
 * {@code docs/01-architecture.md#keeping-the-abstraction-thin}.
 */
public interface IPlatformHelper {

    /** Loader name for logs and crash reports, e.g. {@code "Fabric"}. */
    String getPlatformName();

    /** Whether another mod is present. Use before touching an optional integration. */
    boolean isModLoaded(String modId);

    /** True in a development environment. Guard expensive assertions with this. */
    boolean isDevelopmentEnvironment();

    /**
     * The directory this mod should write its config into.
     *
     * <p>Both loaders point at the instance's {@code config/} folder, but they
     * expose it through different APIs, which is why it lives behind the seam.
     */
    Path getConfigDirectory();

    /**
     * Whether this process is a dedicated server.
     *
     * <p>This is the <i>physical</i> side. Do not use it to decide game logic —
     * use {@code Level#isClientSide()} for that. Use this only to decide whether
     * client-only classes may be touched at all.
     *
     * @see <a href="../../../../../../../docs/02-sides-client-vs-server.md">docs/02-sides-client-vs-server.md</a>
     */
    boolean isDedicatedServer();
}

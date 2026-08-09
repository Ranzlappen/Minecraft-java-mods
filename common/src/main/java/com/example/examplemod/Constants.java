package com.example.examplemod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constants shared by every loader.
 *
 * <p>{@link #MOD_ID} is duplicated here as a Java constant because annotations
 * ({@code @Mod}, {@code @Mixin}) and registry keys need a compile-time constant
 * and cannot read {@code gradle.properties}. It must stay in sync with the
 * {@code mod_id} property — {@code ./scripts/rename-mod.sh} updates both.
 */
public final class Constants {

    /** Registry namespace, config filename, and resource-pack namespace. */
    public static final String MOD_ID = "examplemod";

    /** Human-readable name, used in log lines and command feedback. */
    public static final String MOD_NAME = "Example Mod";

    /**
     * One logger for the whole mod.
     *
     * <p>Minecraft ships SLF4J, so there is no need to add a logging dependency.
     * Use {@code LOG.debug} freely: it is filtered out in production but visible
     * in the dev run configurations.
     */
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private Constants() {
    }
}

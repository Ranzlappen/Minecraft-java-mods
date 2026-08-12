package com.statusoverlay.mod.overlay;

/**
 * Pure math and formatting for the two HUD overlays.
 *
 * <p>Deliberately the most boring class in the mod: plain Java, no Minecraft
 * imports, no loader imports. That makes it unit-testable without a game
 * running — see {@code common/src/test/java/.../overlay/OverlayFormatTest.java}.
 * The client rendering code (loader-specific, see {@code fabric/src/client/java}
 * and {@code neoforge/.../client}) calls into this rather than repeating the
 * arithmetic in two places.
 */
public final class OverlayFormat {

    private static final double TICKS_PER_SECOND = 20.0;

    /**
     * Converts a per-tick horizontal displacement (as returned by
     * {@code Entity#getDeltaMovement()}, e.g. {@code Vec3#horizontalDistance()})
     * into blocks per second.
     *
     * <p>This is the entity's actual current velocity, not its
     * {@code movement_speed} attribute — the attribute is an input to
     * Minecraft's movement formula, not the resulting speed, and reading it
     * directly would show a number that does not match what the player feels.
     * Measuring the real per-tick displacement is simpler and correct
     * regardless of terrain, jumping, or potion effects.
     */
    public static double blocksPerSecond(double horizontalDeltaPerTick) {
        return horizontalDeltaPerTick * TICKS_PER_SECOND;
    }

    /** Formats a value to one decimal place, e.g. {@code 8.3}. */
    public static String oneDecimal(double value) {
        return String.format("%.1f", value);
    }

    private OverlayFormat() {
    }
}

package com.example.examplemod.playtime;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks how long each player has been online in the current session.
 *
 * <p>This is deliberately the most boring class in the template: plain Java,
 * no Minecraft imports, no loader imports. That makes it unit-testable without
 * a game running — see {@code common/src/test/java/.../PlaytimeTrackerTest.java}.
 *
 * <p><b>Sides.</b> This state lives on the <i>logical server</i> only. The
 * server is the single source of truth; a client copy would immediately drift.
 * If a client needs the number, the server sends it a packet.
 *
 * <p><b>Threading.</b> Minecraft ticks the server on one thread, but player
 * connection events can arrive on netty threads. A {@link ConcurrentHashMap}
 * costs nothing here and removes a whole class of rare, unreproducible bugs.
 */
public final class PlaytimeTracker {

    private final Map<UUID, Long> sessionStartMillis = new ConcurrentHashMap<>();
    private final Map<UUID, Long> accumulatedMillis = new ConcurrentHashMap<>();

    /** Call when a player joins. Idempotent — a duplicate join will not double-count. */
    public void onJoin(UUID playerId, long nowMillis) {
        sessionStartMillis.putIfAbsent(playerId, nowMillis);
    }

    /** Call when a player leaves. Banks the elapsed session time. */
    public void onLeave(UUID playerId, long nowMillis) {
        final Long start = sessionStartMillis.remove(playerId);
        if (start != null) {
            accumulatedMillis.merge(playerId, Math.max(0L, nowMillis - start), Long::sum);
        }
    }

    /** Total time for this player, including the session currently in progress. */
    public Duration totalFor(UUID playerId, long nowMillis) {
        long total = accumulatedMillis.getOrDefault(playerId, 0L);
        final Long start = sessionStartMillis.get(playerId);
        if (start != null) {
            total += Math.max(0L, nowMillis - start);
        }
        return Duration.ofMillis(total);
    }

    /** True if the player is currently counted as online. */
    public boolean isOnline(UUID playerId) {
        return sessionStartMillis.containsKey(playerId);
    }

    /** Drops all state. Called when a server shuts down or a world is unloaded. */
    public void reset() {
        sessionStartMillis.clear();
        accumulatedMillis.clear();
    }

    /**
     * Formats a duration the way a player expects to read it: {@code 3h 07m},
     * {@code 12m 30s}, {@code 45s}.
     */
    public static String format(Duration duration) {
        final long totalSeconds = Math.max(0L, duration.getSeconds());
        final long hours = totalSeconds / 3600;
        final long minutes = (totalSeconds % 3600) / 60;
        final long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        }
        if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        }
        return seconds + "s";
    }
}

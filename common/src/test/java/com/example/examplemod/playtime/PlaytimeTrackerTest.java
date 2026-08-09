package com.example.examplemod.playtime;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests that run without Minecraft.
 *
 * <p>This is the payoff for keeping game logic in plain Java classes: these
 * tests execute in milliseconds under {@code ./gradlew :common:test}, with no
 * game launch and no mod loader. Anything you can push into this shape gets
 * cheap, fast test coverage.
 *
 * <p>For tests that genuinely need a running game, use NeoForge's GameTest
 * framework instead — see {@code docs/07-testing.md}.
 */
class PlaytimeTrackerTest {

    private static final UUID PLAYER = UUID.nameUUIDFromBytes("player".getBytes());

    @Test
    void countsTimeWhileOnline() {
        final PlaytimeTracker tracker = new PlaytimeTracker();
        tracker.onJoin(PLAYER, 1_000L);

        assertEquals(Duration.ofSeconds(5), tracker.totalFor(PLAYER, 6_000L));
        assertTrue(tracker.isOnline(PLAYER));
    }

    @Test
    void banksTimeOnLeaveAndResumesOnRejoin() {
        final PlaytimeTracker tracker = new PlaytimeTracker();
        tracker.onJoin(PLAYER, 0L);
        tracker.onLeave(PLAYER, 10_000L);

        assertFalse(tracker.isOnline(PLAYER));
        assertEquals(Duration.ofSeconds(10), tracker.totalFor(PLAYER, 99_000L));

        tracker.onJoin(PLAYER, 100_000L);
        assertEquals(Duration.ofSeconds(13), tracker.totalFor(PLAYER, 103_000L));
    }

    @Test
    void duplicateJoinDoesNotDoubleCount() {
        final PlaytimeTracker tracker = new PlaytimeTracker();
        tracker.onJoin(PLAYER, 0L);
        tracker.onJoin(PLAYER, 5_000L);

        assertEquals(Duration.ofSeconds(10), tracker.totalFor(PLAYER, 10_000L));
    }

    @Test
    void unknownPlayerHasNoTime() {
        final PlaytimeTracker tracker = new PlaytimeTracker();
        assertEquals(Duration.ZERO, tracker.totalFor(UUID.randomUUID(), 50_000L));
    }

    @Test
    void clockGoingBackwardsDoesNotProduceNegativeTime() {
        // Not paranoia: NTP corrections and container clock drift do happen on
        // long-running servers, and a negative Duration formats as garbage.
        final PlaytimeTracker tracker = new PlaytimeTracker();
        tracker.onJoin(PLAYER, 10_000L);

        assertEquals(Duration.ZERO, tracker.totalFor(PLAYER, 5_000L));
    }

    @Test
    void formatsDurationsForPlayers() {
        assertEquals("45s", PlaytimeTracker.format(Duration.ofSeconds(45)));
        assertEquals("12m 30s", PlaytimeTracker.format(Duration.ofSeconds(750)));
        assertEquals("3h 07m", PlaytimeTracker.format(Duration.ofMinutes(187)));
        assertEquals("0s", PlaytimeTracker.format(Duration.ZERO));
    }
}

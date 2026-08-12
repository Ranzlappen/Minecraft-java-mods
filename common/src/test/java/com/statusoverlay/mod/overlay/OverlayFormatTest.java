package com.statusoverlay.mod.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlayFormatTest {

    @Test
    void blocksPerSecondScalesByTicksPerSecond() {
        // A horse moving 0.215 blocks/tick horizontally is walking at a
        // realistic ~4.3 blocks/second.
        assertEquals(4.3, OverlayFormat.blocksPerSecond(0.215), 1e-9);
    }

    @Test
    void blocksPerSecondHandlesStandingStill() {
        assertEquals(0.0, OverlayFormat.blocksPerSecond(0.0), 1e-9);
    }

    @Test
    void oneDecimalRoundsAndPadsAWholeNumber() {
        assertEquals("8.3", OverlayFormat.oneDecimal(8.27));
        assertEquals("4.0", OverlayFormat.oneDecimal(4.0));
    }

    @Test
    void oneDecimalHandlesZero() {
        assertEquals("0.0", OverlayFormat.oneDecimal(0.0));
    }
}

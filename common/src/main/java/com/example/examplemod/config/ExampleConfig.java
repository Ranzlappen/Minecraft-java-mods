package com.example.examplemod.config;

import com.example.examplemod.Constants;
import com.example.examplemod.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The mod's configuration, loaded from {@code config/examplemod.json}.
 *
 * <p><b>Why hand-rolled GSON instead of a config library?</b> Minecraft already
 * ships GSON, so this adds zero dependencies and works identically on both
 * loaders. Loader-specific config systems (NeoForge's {@code ModConfigSpec},
 * Fabric's community libraries) are nicer but cannot live in common code. If
 * you outgrow this, put the real implementation behind
 * {@link com.example.examplemod.platform.IPlatformHelper} rather than pulling a
 * config library into common.
 *
 * <p><b>Sides.</b> This is a <i>server-side</i> config: it is read by the
 * logical server and is authoritative. A client that also has the mod installed
 * has its own copy of this file, but the server's copy is what governs
 * gameplay. Never assume the two match — see
 * {@code docs/02-sides-client-vs-server.md#configuration-belongs-to-the-server}.
 */
public final class ExampleConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = Constants.MOD_ID + ".json";

    private static ExampleConfig instance = new ExampleConfig();

    // --- Fields are the config schema. -------------------------------------
    // Keep them public and simple: GSON maps them directly to JSON keys, so a
    // field rename is a breaking change to every existing user's config file.

    /** Master switch. When false the mod registers nothing gameplay-facing. */
    public boolean enabled = true;

    /**
     * How often, in seconds, the server pushes a playtime update to clients that
     * have the mod installed. Vanilla clients are unaffected.
     */
    public int clientSyncIntervalSeconds = 30;

    /** Whether {@code /playtime} may be used to look up other players. */
    public boolean allowQueryingOtherPlayers = true;

    /** Permission level required for the admin subcommands. 0 = everyone, 4 = owner. */
    public int adminPermissionLevel = 2;

    public static ExampleConfig get() {
        return instance;
    }

    /**
     * Reads the config from disk, writing a default file if none exists.
     *
     * <p>Call this once during mod initialisation, before anything reads
     * {@link #get()}. A malformed file is logged and ignored rather than
     * crashing the server — a broken config should not take a server down.
     */
    public static void load() {
        final Path path = Services.PLATFORM.getConfigDirectory().resolve(FILE_NAME);

        if (Files.notExists(path)) {
            instance = new ExampleConfig();
            save();
            Constants.LOG.info("Wrote default config to {}", path);
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            final ExampleConfig parsed = GSON.fromJson(reader, ExampleConfig.class);
            // GSON returns null for an empty file rather than throwing.
            instance = parsed != null ? parsed : new ExampleConfig();
            instance.clamp();
            Constants.LOG.info("Loaded config from {}", path);
        } catch (Exception e) {
            Constants.LOG.error("Could not read {} — falling back to defaults. "
                    + "Fix or delete the file to silence this.", path, e);
            instance = new ExampleConfig();
        }
    }

    public static void save() {
        final Path path = Services.PLATFORM.getConfigDirectory().resolve(FILE_NAME);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            Constants.LOG.error("Could not write config to {}", path, e);
        }
    }

    /** Pulls out-of-range values back into a sane range instead of failing. */
    private void clamp() {
        if (clientSyncIntervalSeconds < 1) {
            clientSyncIntervalSeconds = 1;
        }
        if (adminPermissionLevel < 0 || adminPermissionLevel > 4) {
            adminPermissionLevel = 2;
        }
    }
}

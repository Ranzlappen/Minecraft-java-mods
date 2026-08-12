package com.statusoverlay.mod.platform;

import com.statusoverlay.mod.Constants;

import java.util.ServiceLoader;

/**
 * Locates the loader-specific implementations of the platform interfaces.
 *
 * <p>This uses the plain JDK {@link ServiceLoader}. There is no magic and no
 * extra dependency: each loader subproject ships a file at
 * {@code META-INF/services/com.statusoverlay.mod.platform.IPlatformHelper}
 * containing the fully-qualified name of its implementation, and the JDK wires
 * them together at runtime.
 *
 * <p><b>Why not Architectury's {@code @ExpectPlatform}?</b> That works too, but
 * it needs an extra Gradle plugin and a runtime library, and it fails at
 * runtime rather than at compile time when an implementation is missing. For a
 * template that should stay dependency-light, ServiceLoader is the smaller
 * commitment. See {@code docs/01-architecture.md#why-not-architectury}.
 *
 * <p><b>If you see {@code ServiceConfigurationError} on startup:</b> the
 * services file is missing or names a class that was renamed. That file is a
 * plain text resource, so refactoring tools will not update it for you.
 */
public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    /**
     * Loads exactly one implementation of {@code clazz} from the classpath.
     *
     * @throws NullPointerException if no implementation is registered, which in
     *                              practice means the {@code META-INF/services}
     *                              file is missing from the loader subproject
     */
    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException(
                        "Failed to load service for " + clazz.getName()
                                + ". This almost always means the META-INF/services file is "
                                + "missing or names a class that no longer exists."));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz.getSimpleName());
        return loadedService;
    }

    private Services() {
    }
}

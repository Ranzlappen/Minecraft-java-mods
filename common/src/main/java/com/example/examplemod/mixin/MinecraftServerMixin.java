package com.example.examplemod.mixin;

import com.example.examplemod.Constants;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A deliberately minimal Mixin, kept as a working reference.
 *
 * <p><b>What a Mixin is.</b> Minecraft is not designed to be extended, so most
 * of the interesting hooks do not exist. Mixin rewrites Minecraft's bytecode at
 * load time to add them. Both Fabric and NeoForge ship the same Mixin library,
 * which is why this class lives in {@code common} and works on both.
 *
 * <p><b>Use it as a last resort.</b> Reach for a loader event first — events
 * are stable, composable and do not conflict with other mods. A Mixin binds to
 * a specific method in a specific class, so it is the part of your mod most
 * likely to break on a Minecraft update. Every Mixin you write is future
 * porting work you are signing up for.
 *
 * <p><b>{@code require = 0} on the injection.</b> This tells Mixin "do not fail
 * the game if this injection point cannot be found". That is the right choice
 * for a purely informational hook like this one. For a Mixin your mod actually
 * depends on, remove it: you want a loud failure at startup rather than a
 * feature that silently does nothing.
 *
 * <p>See {@code docs/06-mixins-and-access-transformers.md} for the full
 * treatment, including how to pick injection points and how to read a Mixin
 * crash report.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    /**
     * Runs at the end of the server constructor.
     *
     * <p>The constructor is a safe target because it always exists and its name
     * never changes, unlike ordinary methods which Mojang renames freely.
     */
    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void examplemod$onServerConstructed(CallbackInfo ci) {
        Constants.LOG.debug("MinecraftServer constructed — {} mixins are applying correctly.",
                Constants.MOD_NAME);
    }
}

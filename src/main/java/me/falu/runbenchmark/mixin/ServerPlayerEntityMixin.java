package me.falu.runbenchmark.mixin;

import com.mojang.authlib.GameProfile;
import me.falu.runbenchmark.RunBenchmark;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    public ServerPlayerEntityMixin(World world, BlockPos blockPos, GameProfile gameProfile) {
        super(world, blockPos, gameProfile);
    }

    @Inject(method = "dimensionChanged", at = @At("HEAD"))
    private void addTime(ServerWorld targetWorld, CallbackInfo ci) {
        RegistryKey<World> current = targetWorld.getRegistryKey();
        RegistryKey<World> destination = this.world.getRegistryKey();
        if (current.equals(World.OVERWORLD) && destination.equals(World.NETHER)) {
            RunBenchmark.addTime(RunBenchmark.BenchmarkTypes.OVERWORLD);
        } else if (current.equals(World.NETHER) && destination.equals(World.OVERWORLD)) {
            RunBenchmark.addTime(RunBenchmark.BenchmarkTypes.NETHER);
        } else if (current.equals(World.OVERWORLD) && destination.equals(World.END)) {
            RunBenchmark.addTime(RunBenchmark.BenchmarkTypes.POST_BLIND);
        }
    }
}

package me.falu.runbenchmark.mixin;

import me.falu.runbenchmark.RunBenchmark;
import me.falu.runbenchmark.gui.AverageOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow public abstract boolean isInSingleplayer();

    @Shadow @Nullable public Screen currentScreen;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/toast/ToastManager;draw(Lnet/minecraft/client/util/math/MatrixStack;)V"))
    private void renderOverlay(boolean tick, CallbackInfo ci) {
        if (this.isInSingleplayer() && !(this.currentScreen instanceof LevelLoadingScreen)) {
            AverageOverlay.getInstance().render(new MatrixStack());
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("TAIL"))
    private void onJoinWorld(Screen screen, CallbackInfo ci) {
        AverageOverlay.getInstance().setAverages(RunBenchmark.getAverageTimes());
        RunBenchmark.LAST_TIMESTAMP = 0L;
        RunBenchmark.COMPLETED_TYPES.clear();
    }
}

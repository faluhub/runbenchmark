package me.falu.runbenchmark.gui;

import me.falu.runbenchmark.RunBenchmark;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import java.util.Map;

public class AverageOverlay {
    private static AverageOverlay INSTANCE;
    private final MinecraftClient client;
    private Map<String, Long> averages;

    public AverageOverlay() {
        this.client = MinecraftClient.getInstance();
    }

    public static AverageOverlay getInstance() {
        if (INSTANCE == null) { INSTANCE = new AverageOverlay(); }
        return INSTANCE;
    }

    public void render(MatrixStack matrices) {
        if (MinecraftClient.getInstance().options.debugEnabled || MinecraftClient.getInstance().isPaused()) { return; }
        float guiScale = 1.0F;
        Pair<Integer, Integer> guiPosition = new Pair<>((int) (this.client.getWindow().getScaledWidth() / guiScale * 0.983F), (int) (this.client.getWindow().getScaledHeight() / guiScale * 0.137F));
        Pair<Boolean, Boolean> guiAlign = new Pair<>(true, false);
        this.drawGuiText(matrices, this.client.textRenderer, new LiteralText("Averages").formatted(Formatting.BOLD, Formatting.LIGHT_PURPLE), guiPosition.getLeft(), guiPosition.getRight(), 0.0F, guiAlign);
        float advance = 12.0F;
        if (this.averages == null || this.averages.isEmpty()) {
            this.drawGuiText(matrices, this.client.textRenderer, new LiteralText("N/A").formatted(Formatting.ITALIC), guiPosition.getLeft(), guiPosition.getRight(), advance, guiAlign);
            return;
        }
        for (RunBenchmark.BenchmarkTypes type : RunBenchmark.BenchmarkTypes.values()) {
            long value = this.averages.get(type.key);
            String text = "N/A";
            if (value > 0) { text = RunBenchmark.getTimeFormat(value); }
            this.drawGuiText(matrices, this.client.textRenderer, new LiteralText(type.name + ": " + Formatting.ITALIC + text), guiPosition.getLeft(), guiPosition.getRight(), advance, guiAlign);
            advance += 12.0F;
        }
    }

    private void drawGuiText(MatrixStack matrixStack, TextRenderer textRenderer, Text text, float x, float y, float advanceY, Pair<Boolean, Boolean> alignPair) {
        Pair<Float, Float> position = this.getDrawPosition(x, y, advanceY, textRenderer.getWidth(text), alignPair);
        textRenderer.drawWithShadow(matrixStack, text, position.getLeft(), position.getRight(), 16777215);
    }

    private Pair<Float, Float> getDrawPosition(float x, float y, float advanceY, float width, Pair<Boolean, Boolean> alignPair) {
        return new Pair<>(x - (alignPair.getLeft() ? (width + (float) 0.0) : -(float) 0.0), y + (alignPair.getRight() ? -((float) 9.0 - advanceY + 32.0F) : advanceY));
    }

    public void setAverages(Map<String, Long> averages) {
        this.averages = averages;
    }
}

package com.adityabarwal.deathloc.mixin;

import com.adityabarwal.deathloc.DeathLocationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void deathloc$init(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        DeathLocationManager.captureIfDead(client);

        DeathLocationManager.DeathLocation location = DeathLocationManager.getLastDeathLocation();
        if (location == null) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int availableWidth = Math.max(44, screenWidth - (screenWidth / 2 + 108));
        int buttonWidth = Math.min(112, availableWidth);
        int buttonX = Math.min(screenWidth / 2 + 102, screenWidth - buttonWidth - 6);
        int buttonY = Math.min(screenHeight - 20, screenHeight / 4 + 72);

        ButtonWidget copyButton = ButtonWidget.builder(
                        Text.translatable("deathloc.button.copy"),
                        button -> DeathLocationManager.copyToClipboard(client, location)
                )
                .dimensions(buttonX, buttonY, buttonWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("deathloc.button.copy.tooltip")))
                .build();

        ((ScreenAccessor) (Object) this).deathloc$addDrawableChild(copyButton);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void deathloc$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        DeathLocationManager.DeathLocation location = DeathLocationManager.getLastDeathLocation();
        if (location == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int panelWidth = Math.min(260, screenWidth - 20);
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = Math.min(screenHeight / 4 + 48, screenHeight - 52);
        int centerX = screenWidth / 2;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + 24, 0xB30B1020);
        context.drawBorder(panelX, panelY, panelWidth, 24, 0xFFB86BFF);
        context.drawCenteredTextWithShadow(
                client.inGameHud.getTextRenderer(),
                DeathLocationManager.panelTitle(location),
                centerX,
                panelY + 2,
                0xFFFFFFFF
        );
        context.drawCenteredTextWithShadow(
                client.inGameHud.getTextRenderer(),
                DeathLocationManager.panelCoordinates(location),
                centerX,
                panelY + 13,
                0xFFFFFFFF
        );
    }
}

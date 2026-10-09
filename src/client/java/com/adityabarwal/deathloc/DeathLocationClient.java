package com.adityabarwal.deathloc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class DeathLocationClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DeathLocationManager.load();
        ClientTickEvents.END_CLIENT_TICK.register(DeathLocationManager::onClientTick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("deathloc")
                        .then(ClientCommandManager.literal("show").executes(context -> {
                            DeathLocationManager.showLastDeath(context.getSource());
                            return 1;
                        }))
                        .executes(context -> {
                            context.getSource().sendFeedback(
                                    Text.translatable("deathloc.command.usage").formatted(Formatting.GRAY)
                            );
                            return 1;
                        }))
        );
    }
}

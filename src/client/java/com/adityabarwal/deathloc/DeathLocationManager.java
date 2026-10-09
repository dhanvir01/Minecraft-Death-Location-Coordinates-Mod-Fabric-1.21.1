package com.adityabarwal.deathloc;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Stores the latest client-observed death location and presents it to the player. */
public final class DeathLocationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Death Location Coordinates");
    private static final String SAVE_FILE_NAME = "deathloc.properties";

    private static DeathLocation lastDeathLocation;
    private static boolean loaded;
    private static boolean capturedThisDeath;

    private DeathLocationManager() {
    }

    /** A block-coordinate snapshot of the player's most recent death. */
    public record DeathLocation(int x, int y, int z, String dimension) {
        public String coordinatesForClipboard() {
            return x + " " + y + " " + z;
        }
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        Path file = getSaveFile();
        if (!Files.isRegularFile(file)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            int x = Integer.parseInt(properties.getProperty("x"));
            int y = Integer.parseInt(properties.getProperty("y"));
            int z = Integer.parseInt(properties.getProperty("z"));
            String dimension = properties.getProperty("dimension", "unknown");
            if (dimension.isBlank()) {
                dimension = "unknown";
            }
            lastDeathLocation = new DeathLocation(x, y, z, dimension);
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Could not load the saved death location from {}", file, exception);
        }
    }

    /** Capture exactly once between respawns, even if Minecraft reinitializes the death screen. */
    public static void captureIfDead(MinecraftClient client) {
        load();

        ClientPlayerEntity player = client.player;
        if (player == null || player.isAlive() || capturedThisDeath) {
            return;
        }

        BlockPos position = player.getBlockPos();
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        DeathLocation location = new DeathLocation(
                position.getX(),
                position.getY(),
                position.getZ(),
                dimension
        );

        lastDeathLocation = location;
        capturedThisDeath = true;
        save(location);
        addChatMessage(client, formatLocationMessage(location, "deathloc.chat.captured"));
    }

    /** Reset the one-capture guard once the player has respawned. */
    public static void onClientTick(MinecraftClient client) {
        if (client.player != null && client.player.isAlive()) {
            capturedThisDeath = false;
        }
    }

    public static DeathLocation getLastDeathLocation() {
        load();
        return lastDeathLocation;
    }

    public static void showLastDeath(FabricClientCommandSource source) {
        DeathLocation location = getLastDeathLocation();
        if (location == null) {
            source.sendFeedback(
                    Text.translatable("deathloc.command.no_location").formatted(Formatting.YELLOW)
            );
            return;
        }

        source.sendFeedback(formatLocationMessage(location, "deathloc.chat.previous"));
    }

    public static void copyToClipboard(MinecraftClient client, DeathLocation location) {
        String coordinates = location.coordinatesForClipboard();
        try {
            GLFW.glfwSetClipboardString(client.getWindow().getHandle(), coordinates);
            addChatMessage(client, Text.translatable("deathloc.chat.copied")
                    .formatted(Formatting.GREEN, Formatting.BOLD)
                    .append(Text.literal(" " + coordinates).formatted(Formatting.WHITE)));
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not copy death coordinates to the clipboard", exception);
            addChatMessage(client, Text.translatable("deathloc.chat.copy_failed").formatted(Formatting.RED));
        }
    }

    public static Text panelTitle(DeathLocation location) {
        return Text.literal("◆ ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.translatable("deathloc.ui.last_death").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("  •  ").formatted(Formatting.DARK_GRAY))
                .append(dimensionName(location.dimension()).formatted(Formatting.AQUA));
    }

    public static Text panelCoordinates(DeathLocation location) {
        return coordinateText(location);
    }

    private static Text formatLocationMessage(DeathLocation location, String headingKey) {
        return Text.literal("◆ ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.translatable(headingKey).formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("  "))
                .append(coordinateText(location))
                .append(Text.literal("  •  ").formatted(Formatting.DARK_GRAY))
                .append(dimensionName(location.dimension()).formatted(Formatting.AQUA));
    }

    private static MutableText coordinateText(DeathLocation location) {
        return Text.empty()
                .append(Text.literal("X: ").formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal(Integer.toString(location.x())).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal("   "))
                .append(Text.literal("Y: ").formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal(Integer.toString(location.y())).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal("   "))
                .append(Text.literal("Z: ").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(Integer.toString(location.z())).formatted(Formatting.WHITE, Formatting.BOLD));
    }

    private static MutableText dimensionName(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> Text.translatable("deathloc.dimension.overworld");
            case "minecraft:the_nether" -> Text.translatable("deathloc.dimension.nether");
            case "minecraft:the_end" -> Text.translatable("deathloc.dimension.end");
            default -> Text.literal(dimension);
        };
    }

    private static void addChatMessage(MinecraftClient client, Text message) {
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(message);
        }
    }

    private static void save(DeathLocation location) {
        Path file = getSaveFile();
        Path temporaryFile = file.resolveSibling(file.getFileName() + ".tmp");

        Properties properties = new Properties();
        properties.setProperty("x", Integer.toString(location.x()));
        properties.setProperty("y", Integer.toString(location.y()));
        properties.setProperty("z", Integer.toString(location.z()));
        properties.setProperty("dimension", location.dimension());

        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                properties.store(output, "Most recent death location for Death Location Coordinates");
            }

            try {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save the death location to {}", file, exception);
        }
    }

    private static Path getSaveFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(SAVE_FILE_NAME);
    }
}

# Death Location Coordinates

A **client-side Fabric mod for Minecraft 1.21.1** that remembers where you died so you can find your items again.

## Features

- Captures your block coordinates the moment the death screen opens.
- Shows a colorful **Last Death** panel on the death screen, including the dimension.
- Adds a **Copy** button to copy `X Y Z` to your clipboard.
- Announces the coordinates in chat and keeps them available with the client-side command:

  ```text
  /deathloc show
  ```

- Saves the most recent location in `config/deathloc.properties`, so it is still available after restarting Minecraft.
- Client-side only: install it on your own client; no server mod is required.

## Install

1. Install Fabric Loader for **Minecraft 1.21.1** and make sure **Fabric API** is installed.
2. Download or build the mod jar and put it in your Minecraft `mods` folder.
3. Launch the Fabric 1.21.1 profile and play. The next time you die, the coordinates will be recorded automatically.

## Build from source

Requires **Java 21**.

```bash
./gradlew build
```

The built jar will be in `build/libs/`.
The repository contains the mod’s source code; Gradle builds it into a .jar.

Install a Java 21 JDK and verify it with java -version.

Open a terminal in the project folder—the one containing gradlew and build.gradle.

## Run the build:

### Windows

#### PowerShell

```.\gradlew.bat build```

### macOS/Linux

```/gradlew build```

Find the built mod in ```build/libs/```. The main jar should be named *death-location-coordinates-1.0.0.jar*. Use that jar—not one ending in -sources.jar or -dev.jar—and place it in your Minecraft mods folder. You’ll also need Fabric Loader and Fabric API for Minecraft 1.21.1.

## Credits

**Made by Aditya Barwal.**

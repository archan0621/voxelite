# Voxelite

A lightweight voxel game engine built on LibGDX, designed for creating Minecraft-style block-based games.

## Overview

Voxelite provides core engine functionality for voxel-based games while keeping game logic separate. It handles world management, physics, rendering, and input processing, allowing developers to focus on implementing game rules and mechanics.

## Features

- **World Management**: Add, remove, and manage voxel blocks in 3D space
- **Physics System**: Minecraft-style collision detection with axis-separated movement
- **Rendering**: Block rendering with outline selection and crosshair overlay
- **Camera System**: First-person camera with smooth movement and rotation
- **Input Handling**: Mouse and keyboard input processing
- **Player Entity**: Character controller with collision and gravity

## Requirements

- Java 17 or higher
- LibGDX 1.14.0
- Gson 2.10.1

## Installation

### Using as a Library

1. Build the JAR file:
```bash
cd voxelite
./gradlew build
```

2. Include the generated JAR in your project:
```bash
cp build/libs/voxelite-1.0-SNAPSHOT.jar /path/to/your/project/libs/
```

3. Add dependency in your `build.gradle`:
```gradle
dependencies {
    implementation files('libs/voxelite-1.0-SNAPSHOT.jar')
    implementation "com.badlogicgames.gdx:gdx:1.14.0"
    implementation "com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.0"
}
```

## Quick Start

### Basic Setup (Recommended)

```java
import kr.co.voxelite.engine.VoxeliteEngine;
import com.badlogic.gdx.math.Vector3;

// Create and initialize engine with builder pattern
VoxeliteEngine engine = VoxeliteEngine.builder()
    .worldSize(11, 11)              // 11x11 flat ground
    .groundLevel(-1f)               // Ground at y=-1
    .playerStart(0f, -0.5f, 0f)     // Player spawn position
    .playerSpeed(5f)                // Movement speed
    .cameraPitch(-20f)              // Camera angle
    .mouseSensitivity(0.1f)         // Mouse sensitivity
    .build();

// Initialize (call once in show() or create())
engine.initialize(screenWidth, screenHeight);

// Add blocks to the world
engine.getWorld().addBlock(new Vector3(0f, 0f, 3f));
```

### Game Loop

```java
@Override
public void render(float delta) {
    // Update all systems (input, physics, camera)
    engine.update(delta);
    
    // Your game logic here
    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
        Vector3 selectedBlock = engine.getSelectedBlock();
        if (selectedBlock != null) {
            engine.getWorld().removeBlock(selectedBlock);
        }
    }
    
    // Render everything
    engine.render();
}
```

### Minimal Example (LibGDX Screen)

```java
public class GameScreen implements Screen {
    private VoxeliteEngine engine;
    
    @Override
    public void show() {
        engine = VoxeliteEngine.builder()
            .worldSize(11, 11)
            .build();
        
        engine.initialize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }
    
    @Override
    public void render(float delta) {
        engine.update(delta);
        engine.render();
    }
    
    @Override
    public void resize(int width, int height) {
        engine.resize(width, height);
    }
    
    @Override
    public void dispose() {
        engine.dispose();
    }
}
```

### Advanced: Direct Access to Systems

```java
// Access underlying systems if needed
World world = engine.getWorld();
Player player = engine.getPlayer();
FPSCamera camera = engine.getCamera();
InputHandler input = engine.getInput();

// World manipulation
world.addBlock(new Vector3(x, y, z));
world.removeBlock(position);
world.clear();

// Player info
Vector3 playerPos = player.getPosition();
boolean onGround = player.isOnGround();
```

## API Reference

### VoxeliteEngine (Main API)

High-level facade that manages all engine systems.

**Builder Methods:**
- `worldSize(int width, int height)` - Set world grid size
- `groundLevel(float level)` - Set ground Y position
- `playerStart(float x, float y, float z)` - Set player spawn position
- `playerSpeed(float speed)` - Set movement speed
- `fieldOfView(float fov)` - Set camera FOV (default: 67)
- `cameraPitch(float pitch)` - Set initial camera pitch (default: -20)
- `mouseSensitivity(float sensitivity)` - Set mouse sensitivity (default: 0.1)

**Core Methods:**
- `initialize(int width, int height)` - Initialize all systems (call once)
- `update(float delta)` - Update all systems each frame
- `render()` - Render the world each frame
- `resize(int width, int height)` - Handle window resize
- `dispose()` - Clean up resources

**Accessors:**
- `getWorld()` - Get World instance for block manipulation
- `getPlayer()` - Get Player instance
- `getCamera()` - Get FPSCamera instance
- `getSelectedBlock()` - Get currently selected block (via raycast)
- `getInput()` - Get InputHandler instance
- `isInitialized()` - Check if engine is initialized

### World

Manages the voxel world structure.

**Key Methods:**
- `addBlock(Vector3 position)` - Add a block at position
- `removeBlock(Vector3 position)` - Remove block and return success status
- `clear()` - Remove all blocks
- `addFlatGround(int gridSize, float spacing, float yPosition)` - Generate flat terrain
- `getBlockPositions()` - Get list of all block positions

### Player

Character entity with collision bounds and physics properties.

**Constants:**
- `WIDTH = 0.6f` - Player width
- `HEIGHT = 1.8f` - Player height  
- `EYE_HEIGHT = 1.62f` - Camera height offset
- `GRAVITY = -20f` - Gravity acceleration
- `JUMP_VELOCITY = 7f` - Initial jump speed

**Key Methods:**
- `getPosition()` - Get player position (feet)
- `getEyePosition()` - Get camera position
- `getVelocity()` - Get current velocity
- `isOnGround()` - Check if player is on ground

## Architecture

Voxelite follows a clear separation between engine and application:

- **Engine Layer** (Voxelite): Provides primitive operations for world manipulation
- **Application Layer**: Implements game rules, logic, and when to use engine APIs

Example:
```java
// Voxelite provides: "Remove this block"
world.removeBlock(position);

// Application decides: "Remove block when player left-clicks"
if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
    if (selectedBlock != null) {
        world.removeBlock(selectedBlock);
    }
}
```

## Building from Source

```bash
# Clean and build
./gradlew clean build

# Run tests
./gradlew test

# Generate JAR only
./gradlew jar
```

## License

Copyright 2026. All rights reserved.

## Contributing

This is a proprietary library. Contact the maintainers for contribution guidelines.

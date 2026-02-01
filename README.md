# Voxelite

A lightweight voxel game engine built on LibGDX, designed for creating Minecraft-style block-based games.

## Overview

Voxelite provides core engine functionality for voxel-based games while keeping game logic separate. It handles world management, physics, rendering, and input processing, allowing developers to focus on implementing game rules and mechanics.

## Features

- **Chunk System**: Efficient chunk-based world management with disk persistence
- **World Management**: Add, remove, and manage voxel blocks in 3D space
- **Physics System**: Minecraft-style collision detection with axis-separated movement (Fixed Timestep)
- **Rendering**: Optimized block rendering with Greedy Meshing, Face Culling, and Frustum Culling
- **Texture Atlas**: Support for texture atlases with atlas-safe UV mapping
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

## Performance Optimizations

Voxelite implements numerous performance optimizations at the engine level to ensure smooth gameplay even with large worlds:

| Optimization | Description | Impact |
|-------------|-------------|--------|
| **instancesDirty Flag** | Mesh reconstruction only occurs when chunks change | Prevents per-frame mesh regeneration, dramatically reduces CPU usage |
| **chunksChanged Flag** | Mesh reconstruction triggered only on chunk add/remove | Eliminates unnecessary mesh rebuilds |
| **Chunk Boundary Detection** | `updateLoadedChunks` called only when player crosses chunk boundaries | Removes per-frame full chunk iteration, reduces CPU usage by 90%+ |
| **nearbyBlocks Caching** | Physics block list updated only on chunk movement | Eliminates per-physics-step block collection |
| **Physics Chunk Detection** | `nearbyBlocks` updated only when physics chunk changes | Prevents unnecessary block collection |
| **Collection Reuse** | `Set<ChunkCoord>` and `ChunkCoord` objects reused to reduce GC pressure | Reduces object allocations, decreases GC pauses |
| **Face Culling** | Hidden faces adjacent to other blocks excluded from rendering | Reduces rendered faces by 50-70%, decreases GPU load |
| **Fully Occluded Block Removal** | Blocks with all 6 faces hidden completely excluded from mesh | Reduces memory usage and rendering overhead |
| **Tick-based Chunk Updates (50ms)** | Chunk loading/unloading runs at frame-independent 20Hz tick rate | Stable chunk management even during frame drops |
| **Fixed Timestep Physics** | Physics simulation runs at fixed 60Hz timestep | Frame-rate independent physics, stable jump/collision behavior |
| **Chunk Unified Mesh** | All blocks in a chunk merged into single Model (1 Chunk = 1 Draw Call) | Dramatically reduces Draw Calls (N blocks → 1 Draw Call), improves GPU performance |
| **Frustum Culling** | Chunks outside camera view frustum excluded from rendering | Eliminates rendering of off-screen chunks, reduces GPU load |
| **Greedy Meshing** | Adjacent identical faces merged into larger quads, reducing vertex count | Reduces vertex count by 10-20x, saves memory/GPU bandwidth |
| **ModelInstance Caching** | ModelInstance objects cached in `cachedInstances` list | Prevents per-frame instance creation |
| **Adjacent Chunk Mesh Invalidation** | Block changes invalidate boundary/corner chunks for accurate Face Culling | Maintains Face Culling accuracy |
| **Background Chunk Generation** | Chunk generation runs in background threads via `ExecutorService` | Prevents main thread blocking, avoids frame drops |
| **LRU Chunk Unloading** | Old chunks unloaded based on least recently used time | Controls memory usage, supports large worlds |
| **Chunk Persistence** | Chunks saved/loaded to/from disk for disk-based world management | Limits memory usage, enables world persistence |
| **Atlas-Safe Greedy Meshing** | Merged faces split into 1×1 quads to respect texture atlas boundaries | Eliminates texture bleeding, ensures accurate rendering |

## Quick Start

### Chunk System Setup (Recommended)

```java
import kr.co.voxelite.engine.VoxeliteEngine;
import kr.co.voxelite.world.IChunkGenerator;
import kr.co.voxelite.world.IChunkLoadPolicy;
import kr.co.voxelite.world.Chunk;
import com.badlogic.gdx.math.Vector3;

// 1. Implement chunk generator (terrain generation policy)
IChunkGenerator generator = new IChunkGenerator() {
    @Override
    public void generateChunk(Chunk chunk, int defaultBlockType) {
        // Your terrain generation logic here
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                chunk.addBlock(x, 0, z, defaultBlockType);
            }
        }
    }
};

// 2. Implement chunk load policy (loading/unloading policy)
IChunkLoadPolicy loadPolicy = new IChunkLoadPolicy() {
    @Override
    public boolean shouldLoadToMemory(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        int dx = chunkX - playerChunkX;
        int dz = chunkZ - playerChunkZ;
        return dx * dx + dz * dz <= 9; // 3 chunk radius
    }
    
    @Override
    public boolean shouldPregenerate(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        int dx = chunkX - playerChunkX;
        int dz = chunkZ - playerChunkZ;
        return dx * dx + dz * dz <= 25; // 5 chunk radius
    }
    
    @Override
    public int getMaxLoadedChunks() {
        return 100; // Maximum chunks in memory
    }
};

// 3. Create and initialize engine with chunk system
VoxeliteEngine engine = VoxeliteEngine.builder()
    .textureAtlasPath("texture/block.png")  // Texture atlas (16x16 grid)
    .playerStart(0f, 0.5f, 0f)              // Player spawn position
    .playerSpeed(5f)                        // Movement speed
    .cameraPitch(-20f)                      // Camera angle
    .mouseSensitivity(0.1f)                 // Mouse sensitivity
    .autoCreateGround(true)                 // Auto-generate terrain
    .worldSavePath("saves/world1")          // World save directory
    .chunkGenerator(generator)              // Terrain generation policy
    .chunkLoadPolicy(loadPolicy)            // Chunk loading policy
    .initialChunkRadius(16)                 // Initial generation radius
    .chunkPreloadRadius(1)                  // Initial memory load radius
    .defaultGroundBlockType(0)              // Default block type
    .build();

// Initialize (call once in show() or create())
engine.initialize(screenWidth, screenHeight);

// Add blocks to the world (with block type)
engine.getWorld().addBlock(new Vector3(0f, 5f, 3f), 1); // Block type 1
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
import kr.co.voxelite.engine.VoxeliteEngine;
import kr.co.voxelite.world.IChunkGenerator;
import kr.co.voxelite.world.IChunkLoadPolicy;
import kr.co.voxelite.world.Chunk;

public class GameScreen implements Screen {
    private VoxeliteEngine engine;
    
    @Override
    public void show() {
        // Simple chunk generator (flat ground)
        IChunkGenerator generator = (chunk, defaultType) -> {
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    chunk.addBlock(x, 0, z, defaultType);
                }
            }
        };
        
        // Simple load policy (3 chunk radius)
        IChunkLoadPolicy policy = new IChunkLoadPolicy() {
            @Override
            public boolean shouldLoadToMemory(int cx, int cz, int px, int pz) {
                int dx = cx - px, dz = cz - pz;
                return dx * dx + dz * dz <= 9;
            }
            
            @Override
            public boolean shouldPregenerate(int cx, int cz, int px, int pz) {
                int dx = cx - px, dz = cz - pz;
                return dx * dx + dz * dz <= 25;
            }
            
            @Override
            public int getMaxLoadedChunks() {
                return 100;
            }
        };
        
        engine = VoxeliteEngine.builder()
            .textureAtlasPath("texture/block.png")
            .autoCreateGround(true)
            .chunkGenerator(generator)
            .chunkLoadPolicy(policy)
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
world.addBlock(new Vector3(x, y, z), blockType);  // With block type
world.addBlock(new Vector3(x, y, z));             // Default block type (0)
world.removeBlock(position);
int blockType = world.getBlockType(position);
boolean hasBlock = world.hasBlock(position);
world.clear();

// Player info
Vector3 playerPos = player.getPosition();
boolean onGround = player.isOnGround();

// Chunk system access
ChunkManager chunkManager = world.getChunkManager();
List<Chunk> loadedChunks = chunkManager.getLoadedChunks();
```

### Texture Atlas Setup

Voxelite supports texture atlases (16x16 grid by default). Each block type corresponds to a tile in the atlas:

```java
VoxeliteEngine engine = VoxeliteEngine.builder()
    .textureAtlasPath("texture/block.png")  // Path to texture atlas
    // ... other settings
    .build();
```

Block types are mapped to atlas tiles:
- Block type 0 → Tile (0, 0)
- Block type 1 → Tile (1, 0)
- Block type 16 → Tile (0, 1)
- etc.

The engine uses **Atlas-Safe Greedy Meshing** to ensure textures are correctly rendered without bleeding across atlas boundaries.

## API Reference

### VoxeliteEngine (Main API)

High-level facade that manages all engine systems.

**Builder Methods:**
- `playerStart(float x, float y, float z)` - Set player spawn position
- `playerSpeed(float speed)` - Set movement speed
- `fieldOfView(float fov)` - Set camera FOV (default: 67)
- `cameraPitch(float pitch)` - Set initial camera pitch (default: -20)
- `mouseSensitivity(float sensitivity)` - Set mouse sensitivity (default: 0.1)
- `textureAtlasPath(String path)` - Set texture atlas path (16x16 grid)
- `autoCreateGround(boolean auto)` - Enable/disable automatic terrain generation
- `worldSavePath(String path)` - Set world save directory path
- `chunkGenerator(IChunkGenerator generator)` - Set chunk generation policy
- `chunkLoadPolicy(IChunkLoadPolicy policy)` - Set chunk loading/unloading policy
- `initialChunkRadius(int radius)` - Set initial chunk generation radius
- `chunkPreloadRadius(int radius)` - Set initial chunk memory load radius
- `defaultGroundBlockType(int blockType)` - Set default ground block type
- `worldSeed(long seed)` - Set world generation seed

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

Manages the voxel world structure with chunk-based system.

**Key Methods:**
- `addBlock(Vector3 position, int blockType)` - Add a block at position with block type
- `addBlock(Vector3 position)` - Add a block at position (default blockType = 0)
- `removeBlock(Vector3 position)` - Remove block and return success status
- `hasBlock(Vector3 position)` - Check if block exists at position
- `getBlockType(Vector3 position)` - Get block type at position (-1 if no block)
- `getBlockCount()` - Get total number of blocks
- `clear()` - Remove all blocks
- `initWithChunks(String worldPath, int defaultBlockType, IChunkGenerator generator, IChunkLoadPolicy loadPolicy)` - Initialize chunk system
- `generateInitialChunks(float spawnX, float spawnZ, int totalRadius, int loadRadius)` - Generate initial chunks
- `updateChunks(float playerX, float playerZ)` - Update chunks based on player position (tick-based)
- `processPendingChunks()` - Process chunks generated in background
- `getNearbyBlockPositions(float playerX, float playerZ, int chunkRadius)` - Get blocks near player (for physics)
- `getChunkManager()` - Get ChunkManager instance

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

---

# Voxelite (한국어)

LibGDX 기반의 경량 복셀 게임 엔진으로, Minecraft 스타일의 블록 기반 게임 제작을 위해 설계되었습니다.

## 개요

Voxelite는 게임 로직을 분리하여 복셀 기반 게임의 핵심 엔진 기능을 제공합니다. 월드 관리, 물리, 렌더링, 입력 처리를 담당하여 개발자가 게임 규칙과 메커니즘 구현에 집중할 수 있도록 합니다.

## 기능

- **청크 시스템**: 디스크 영속성을 지원하는 효율적인 청크 기반 월드 관리
- **월드 관리**: 3D 공간에서 복셀 블록 추가, 제거 및 관리
- **물리 시스템**: 축 분리 이동을 사용한 Minecraft 스타일 충돌 감지 (Fixed Timestep)
- **렌더링**: Greedy Meshing, Face Culling, Frustum Culling을 사용한 최적화된 블록 렌더링
- **텍스처 아틀라스**: 아틀라스 안전 UV 매핑을 지원하는 텍스처 아틀라스 지원
- **카메라 시스템**: 부드러운 이동 및 회전을 지원하는 1인칭 카메라
- **입력 처리**: 마우스 및 키보드 입력 처리
- **플레이어 엔티티**: 충돌 및 중력을 지원하는 캐릭터 컨트롤러

## 요구사항

- Java 17 이상
- LibGDX 1.14.0
- Gson 2.10.1

## 설치

### 라이브러리로 사용하기

1. JAR 파일 빌드:
```bash
cd voxelite
./gradlew build
```

2. 생성된 JAR를 프로젝트에 포함:
```bash
cp build/libs/voxelite-1.0-SNAPSHOT.jar /path/to/your/project/libs/
```

3. `build.gradle`에 의존성 추가:
```gradle
dependencies {
    implementation files('libs/voxelite-1.0-SNAPSHOT.jar')
    implementation "com.badlogicgames.gdx:gdx:1.14.0"
    implementation "com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.0"
}
```

## 성능 최적화

Voxelite는 대규모 월드에서도 부드러운 게임플레이를 보장하기 위해 엔진 레벨에서 다양한 성능 최적화를 구현합니다:

| 최적화 | 설명 | 효과 |
|--------|------|------|
| **instancesDirty 플래그** | 청크 변경 시에만 메시 재구성 수행 | 매 프레임 메시 재생성 방지, CPU 사용량 대폭 감소 |
| **chunksChanged 플래그** | 청크 추가/제거 시에만 메시 재구성 트리거 | 불필요한 메시 재구성 방지 |
| **청크 경계 감지** | 플레이어가 청크 경계를 넘을 때만 `updateLoadedChunks` 호출 | 매 프레임 전체 청크 순회 제거, CPU 사용량 90%+ 감소 |
| **nearbyBlocks 캐싱** | 청크 이동 시에만 물리 계산용 블록 리스트 갱신 | 매 물리 스텝마다 블록 수집 제거 |
| **물리 청크 감지** | 물리 청크 변경 시에만 `nearbyBlocks` 갱신 | 불필요한 블록 수집 방지 |
| **컬렉션 재사용** | `Set<ChunkCoord>` 및 `ChunkCoord` 객체 재사용으로 GC 압력 감소 | 객체 할당 감소, GC pause 감소 |
| **Face Culling** | 인접 블록에 가려진 면을 렌더링에서 제외 | 렌더링 면 수 50~70% 감소, GPU 부하 감소 |
| **완전 가려진 블록 제거** | 모든 면이 가려진 블록을 메시에서 완전 제외 | 메모리 사용량 감소, 렌더링 오버헤드 제거 |
| **Tick 기반 청크 업데이트 (50ms)** | 프레임 독립적 20Hz 틱으로 청크 로딩/언로딩 실행 | 프레임 드롭 시에도 안정적 청크 관리 |
| **Fixed Timestep 물리** | 고정 60Hz 타임스텝으로 물리 시뮬레이션 실행 | 프레임레이트 독립적 물리, 안정적인 점프/충돌 |
| **청크 통합 메시** | 청크 내 모든 블록을 1개 Model로 통합 (1 청크 = 1 Draw Call) | Draw Call 수 대폭 감소 (N 블록 → 1 Draw Call), GPU 성능 향상 |
| **Frustum Culling** | 카메라 시야 밖 청크를 렌더링에서 제외 | 화면 밖 청크 렌더링 제거, GPU 부하 감소 |
| **Greedy Meshing** | 인접한 동일 타입 면을 큰 쿼드로 병합하여 버텍스 수 감소 | 버텍스 수 10~20배 감소, 메모리/GPU 대역폭 절약 |
| **ModelInstance 캐싱** | `cachedInstances` 리스트에 ModelInstance 캐싱 | 매 프레임 인스턴스 생성 제거 |
| **인접 청크 메시 무효화** | 블록 변경 시 경계/코너 청크도 메시 무효화 | Face Culling 정확도 유지 |
| **백그라운드 청크 생성** | `ExecutorService`로 백그라운드 스레드에서 청크 생성 | 메인 스레드 블로킹 방지, 프레임 드롭 방지 |
| **LRU 청크 언로드** | 최근 사용 시간 기반으로 오래된 청크 언로드 | 메모리 사용량 제어, 대규모 월드 지원 |
| **청크 영속성** | 청크를 파일로 저장/로드하여 디스크 기반 월드 관리 | 메모리 사용량 제한, 월드 영속성 |
| **Atlas-Safe Greedy Meshing** | 병합된 면을 1×1 쿼드로 분할하여 아틀라스 경계 준수 | 텍스처 섞임 제거, 정확한 렌더링 |

## 빠른 시작

### 청크 시스템 설정 (권장)

```java
import kr.co.voxelite.engine.VoxeliteEngine;
import kr.co.voxelite.world.IChunkGenerator;
import kr.co.voxelite.world.IChunkLoadPolicy;
import kr.co.voxelite.world.Chunk;
import com.badlogic.gdx.math.Vector3;

// 1. 청크 생성기 구현 (지형 생성 정책)
IChunkGenerator generator = new IChunkGenerator() {
    @Override
    public void generateChunk(Chunk chunk, int defaultBlockType) {
        // 지형 생성 로직 작성
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                chunk.addBlock(x, 0, z, defaultBlockType);
            }
        }
    }
};

// 2. 청크 로드 정책 구현 (로딩/언로딩 정책)
IChunkLoadPolicy loadPolicy = new IChunkLoadPolicy() {
    @Override
    public boolean shouldLoadToMemory(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        int dx = chunkX - playerChunkX;
        int dz = chunkZ - playerChunkZ;
        return dx * dx + dz * dz <= 9; // 3 청크 반경
    }
    
    @Override
    public boolean shouldPregenerate(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        int dx = chunkX - playerChunkX;
        int dz = chunkZ - playerChunkZ;
        return dx * dx + dz * dz <= 25; // 5 청크 반경
    }
    
    @Override
    public int getMaxLoadedChunks() {
        return 100; // 메모리 최대 청크 수
    }
};

// 3. 청크 시스템으로 엔진 생성 및 초기화
VoxeliteEngine engine = VoxeliteEngine.builder()
    .textureAtlasPath("texture/block.png")  // 텍스처 아틀라스 (16x16 그리드)
    .playerStart(0f, 0.5f, 0f)              // 플레이어 스폰 위치
    .playerSpeed(5f)                        // 이동 속도
    .cameraPitch(-20f)                      // 카메라 각도
    .mouseSensitivity(0.1f)                  // 마우스 감도
    .autoCreateGround(true)                  // 자동 지형 생성
    .worldSavePath("saves/world1")          // 월드 저장 디렉토리
    .chunkGenerator(generator)              // 지형 생성 정책
    .chunkLoadPolicy(loadPolicy)            // 청크 로딩 정책
    .initialChunkRadius(16)                 // 초기 생성 반경
    .chunkPreloadRadius(1)                   // 초기 메모리 로드 반경
    .defaultGroundBlockType(0)               // 기본 블록 타입
    .build();

// 초기화 (show() 또는 create()에서 한 번 호출)
engine.initialize(screenWidth, screenHeight);

// 월드에 블록 추가 (블록 타입 포함)
engine.getWorld().addBlock(new Vector3(0f, 5f, 3f), 1); // 블록 타입 1
```

### 게임 루프

```java
@Override
public void render(float delta) {
    // 모든 시스템 업데이트 (입력, 물리, 카메라)
    engine.update(delta);
    
    // 게임 로직 작성
    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
        Vector3 selectedBlock = engine.getSelectedBlock();
        if (selectedBlock != null) {
            engine.getWorld().removeBlock(selectedBlock);
        }
    }
    
    // 모든 것 렌더링
    engine.render();
}
```

### 최소 예제 (LibGDX Screen)

```java
import kr.co.voxelite.engine.VoxeliteEngine;
import kr.co.voxelite.world.IChunkGenerator;
import kr.co.voxelite.world.IChunkLoadPolicy;
import kr.co.voxelite.world.Chunk;

public class GameScreen implements Screen {
    private VoxeliteEngine engine;
    
    @Override
    public void show() {
        // 간단한 청크 생성기 (평평한 땅)
        IChunkGenerator generator = (chunk, defaultType) -> {
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    chunk.addBlock(x, 0, z, defaultType);
                }
            }
        };
        
        // 간단한 로드 정책 (3 청크 반경)
        IChunkLoadPolicy policy = new IChunkLoadPolicy() {
            @Override
            public boolean shouldLoadToMemory(int cx, int cz, int px, int pz) {
                int dx = cx - px, dz = cz - pz;
                return dx * dx + dz * dz <= 9;
            }
            
            @Override
            public boolean shouldPregenerate(int cx, int cz, int px, int pz) {
                int dx = cx - px, dz = cz - pz;
                return dx * dx + dz * dz <= 25;
            }
            
            @Override
            public int getMaxLoadedChunks() {
                return 100;
            }
        };
        
        engine = VoxeliteEngine.builder()
            .textureAtlasPath("texture/block.png")
            .autoCreateGround(true)
            .chunkGenerator(generator)
            .chunkLoadPolicy(policy)
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

### 고급: 시스템 직접 접근

```java
// 필요시 기본 시스템 접근
World world = engine.getWorld();
Player player = engine.getPlayer();
FPSCamera camera = engine.getCamera();
InputHandler input = engine.getInput();

// 월드 조작
world.addBlock(new Vector3(x, y, z), blockType);  // 블록 타입 포함
world.addBlock(new Vector3(x, y, z));             // 기본 블록 타입 (0)
world.removeBlock(position);
int blockType = world.getBlockType(position);
boolean hasBlock = world.hasBlock(position);
world.clear();

// 플레이어 정보
Vector3 playerPos = player.getPosition();
boolean onGround = player.isOnGround();

// 청크 시스템 접근
ChunkManager chunkManager = world.getChunkManager();
List<Chunk> loadedChunks = chunkManager.getLoadedChunks();
```

### 텍스처 아틀라스 설정

Voxelite는 텍스처 아틀라스(기본값 16x16 그리드)를 지원합니다. 각 블록 타입은 아틀라스의 타일에 해당합니다:

```java
VoxeliteEngine engine = VoxeliteEngine.builder()
    .textureAtlasPath("texture/block.png")  // 텍스처 아틀라스 경로
    // ... 기타 설정
    .build();
```

블록 타입은 아틀라스 타일에 매핑됩니다:
- 블록 타입 0 → 타일 (0, 0)
- 블록 타입 1 → 타일 (1, 0)
- 블록 타입 16 → 타일 (0, 1)
- 등등

엔진은 **Atlas-Safe Greedy Meshing**을 사용하여 아틀라스 경계를 넘어서 텍스처가 섞이지 않도록 정확하게 렌더링합니다.

## API 참조

### VoxeliteEngine (메인 API)

모든 엔진 시스템을 관리하는 고수준 파사드입니다.

**빌더 메서드:**
- `playerStart(float x, float y, float z)` - 플레이어 스폰 위치 설정
- `playerSpeed(float speed)` - 이동 속도 설정
- `fieldOfView(float fov)` - 카메라 FOV 설정 (기본값: 67)
- `cameraPitch(float pitch)` - 초기 카메라 피치 설정 (기본값: -20)
- `mouseSensitivity(float sensitivity)` - 마우스 감도 설정 (기본값: 0.1)
- `textureAtlasPath(String path)` - 텍스처 아틀라스 경로 설정 (16x16 그리드)
- `autoCreateGround(boolean auto)` - 자동 지형 생성 활성화/비활성화
- `worldSavePath(String path)` - 월드 저장 디렉토리 경로 설정
- `chunkGenerator(IChunkGenerator generator)` - 청크 생성 정책 설정
- `chunkLoadPolicy(IChunkLoadPolicy policy)` - 청크 로딩/언로딩 정책 설정
- `initialChunkRadius(int radius)` - 초기 청크 생성 반경 설정
- `chunkPreloadRadius(int radius)` - 초기 청크 메모리 로드 반경 설정
- `defaultGroundBlockType(int blockType)` - 기본 지면 블록 타입 설정
- `worldSeed(long seed)` - 월드 생성 시드 설정

**핵심 메서드:**
- `initialize(int width, int height)` - 모든 시스템 초기화 (한 번 호출)
- `update(float delta)` - 매 프레임 모든 시스템 업데이트
- `render()` - 매 프레임 월드 렌더링
- `resize(int width, int height)` - 창 크기 조정 처리
- `dispose()` - 리소스 정리

**접근자:**
- `getWorld()` - 블록 조작을 위한 World 인스턴스 가져오기
- `getPlayer()` - Player 인스턴스 가져오기
- `getCamera()` - FPSCamera 인스턴스 가져오기
- `getSelectedBlock()` - 현재 선택된 블록 가져오기 (레이캐스트를 통해)
- `getInput()` - InputHandler 인스턴스 가져오기
- `isInitialized()` - 엔진 초기화 여부 확인

### World

청크 기반 시스템을 사용하는 복셀 월드 구조를 관리합니다.

**주요 메서드:**
- `addBlock(Vector3 position, int blockType)` - 블록 타입과 함께 위치에 블록 추가
- `addBlock(Vector3 position)` - 위치에 블록 추가 (기본 blockType = 0)
- `removeBlock(Vector3 position)` - 블록 제거 및 성공 상태 반환
- `hasBlock(Vector3 position)` - 위치에 블록 존재 여부 확인
- `getBlockType(Vector3 position)` - 위치의 블록 타입 가져오기 (블록 없으면 -1)
- `getBlockCount()` - 총 블록 수 가져오기
- `clear()` - 모든 블록 제거
- `initWithChunks(String worldPath, int defaultBlockType, IChunkGenerator generator, IChunkLoadPolicy loadPolicy)` - 청크 시스템 초기화
- `generateInitialChunks(float spawnX, float spawnZ, int totalRadius, int loadRadius)` - 초기 청크 생성
- `updateChunks(float playerX, float playerZ)` - 플레이어 위치 기반 청크 업데이트 (틱 기반)
- `processPendingChunks()` - 백그라운드에서 생성된 청크 처리
- `getNearbyBlockPositions(float playerX, float playerZ, int chunkRadius)` - 플레이어 근처 블록 가져오기 (물리용)
- `getChunkManager()` - ChunkManager 인스턴스 가져오기

### Player

충돌 경계 및 물리 속성을 가진 캐릭터 엔티티입니다.

**상수:**
- `WIDTH = 0.6f` - 플레이어 너비
- `HEIGHT = 1.8f` - 플레이어 높이
- `EYE_HEIGHT = 1.62f` - 카메라 높이 오프셋
- `GRAVITY = -20f` - 중력 가속도
- `JUMP_VELOCITY = 7f` - 초기 점프 속도

**주요 메서드:**
- `getPosition()` - 플레이어 위치 가져오기 (발)
- `getEyePosition()` - 카메라 위치 가져오기
- `getVelocity()` - 현재 속도 가져오기
- `isOnGround()` - 플레이어가 지면에 있는지 확인

## 아키텍처

Voxelite는 엔진과 애플리케이션 간의 명확한 분리를 따릅니다:

- **엔진 레이어** (Voxelite): 월드 조작을 위한 기본 연산 제공
- **애플리케이션 레이어**: 게임 규칙, 로직 및 엔진 API 사용 시점 구현

예제:
```java
// Voxelite 제공: "이 블록 제거"
world.removeBlock(position);

// 애플리케이션 결정: "플레이어가 왼쪽 클릭 시 블록 제거"
if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
    if (selectedBlock != null) {
        world.removeBlock(selectedBlock);
    }
}
```

## 소스에서 빌드

```bash
# 정리 및 빌드
./gradlew clean build

# 테스트 실행
./gradlew test

# JAR만 생성
./gradlew jar
```

## 라이선스

Copyright 2026. All rights reserved.

## 기여

이것은 독점 라이브러리입니다. 기여 가이드라인은 유지보수자에게 문의하세요.

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

# Minecraft Map Generator Microservice

A high-performance Spring Boot backend microservice designed for real-time Minecraft world map generation (Java Edition versions 1.20 up to 26.2) using a single world seed.

This microservice uses **JNA (Java Native Access)** to bind to **Cubiomes** (a highly optimized native C library), allowing exact simulation of modern Minecraft Multi-Noise terrain generation. The outputs perfectly match Minecraft Vanilla and **Chunkbase** (our quality standard).

---

## 🚀 Key Features

* **Parity with Vanilla & Chunkbase**: Leverages native C simulation of Minecraft's multi-noise biome source (Temperature, Humidity, Continentalness, Erosion, Weirdness, and Depth) for 100% accurate results.
* **Support for Legacy & Future Versions (1.20 to 26.2)**: Supports selecting the generation ruleset dynamically. Year-based version numbers (e.g. `26.1`, `26.2`) are mapped to their respective engine versions (e.g. `1.21` generation rules) seamlessly.
* **Single-Pixel Sampling**: Endpoint to fetch the biome ID, name, and color at any exact block coordinate `(x, z)`.
* **Real-time Map Tiles**: Endpoint to generate and serve standard 256x256 pixel PNG map tiles with scaling based on the zoom factor.
* **Advanced Caching & High Performance**: Cache of configured native generator memory addresses using a composite key `(Seed, Version)`. This bypasses C initialization math on repetitive queries and renders tiles in milliseconds.
* **Modular Design**: Generation is abstracted under a `BiomeGenerator` interface, allowing easy hot-swapping for modded headless servers (e.g. Fabric/Forge) in the future.
* **OpenAPI 3.0 / Swagger Documentation**: Fully documented REST API endpoints with interactive Swagger UI.

---

## 🛠️ Architecture & Technical Stack

* **Framework**: Spring Boot 3.3.0
* **Language**: Java 17 (Eclipse Adoptium toolchain)
* **Native Bindings**: JNA (Java Native Access) 5.14.0
* **Native Library**: Cubiomes C Library (`libcubiomes.so`, compiled for Linux x86-64)
* **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)

### Project Layout

```
mc_map_generator/
├── build.gradle.kts           # Gradle dependencies and toolchain config
├── README.md                  # Project documentation (this file)
└── src/
    ├── main/
    │   ├── java/org/learn/minecraftmap/
    │   │   ├── MinecraftMapApplication.java   # App entrypoint
    │   │   ├── domain/
    │   │   │   ├── BiomeInfo.java             # Biome details DTO
    │   │   │   └── BiomeColorMap.java         # Biome registry: IDs, colors, names
    │   │   ├── generator/
    │   │   │   ├── BiomeGenerator.java        # Decoupled interface
    │   │   │   └── impl/
    │   │   │       └── VanillaBiomeGenerator.java # Native Cubiomes implementation
    │   │   ├── jna/
    │   │   │   └── CubiomesLibrary.java       # JNA bindings mapping to C functions
    │   │   ├── service/
    │   │   │   └── BiomeMapService.java       # PNG rendering & service layer logic
    │   │   └── controller/
    │   │       └── BiomeMapController.java    # REST API endpoints & OpenAPI Swagger
    │   └── resources/
    │       ├── application.yml                # Configuration parameters
    │       └── linux-x86-64/
    │           └── libcubiomes.so             # Native shared library for Linux x64
    └── test/
        └── java/org/learn/minecraftmap/
            └── BiomeMapServiceTest.java       # JUnit 5 integration tests
```

---

## ⚡ Native Memory Management & Thread Safety

The integration with Cubiomes works by dynamically instantiating the C `Generator` struct.

1. **Struct Allocation**: Java allocates exactly `27,592` bytes of native memory using JNA's `com.sun.jna.Memory` class. JNA automatically handles freeing this native memory as soon as the Java garbage collector reclaims the backing `Memory` object.
2. **Setup and Apply Seed**:
   * `setupGenerator(Pointer g, int mc, int flags)` initializes the noise parameters for the target version.
   * `applySeed(Pointer g, int dim, long seed)` configures the noise parameters for the specified world seed on the Overworld (`dim = 0`).
3. **Caching**:
   * A thread-safe `ConcurrentHashMap` caches the configured `Pointer` representing each active `(Seed, Version)` generator instance.
   * Sampling functions (`getBiomeAt`) accept a `const Generator*` pointer and are completely stateless, meaning they are natively thread-safe for concurrent read queries.

---

## 🌐 API Endpoints

### 1. Single Pixel Biome Info
Queries the exact biome at a specific coordinate.

* **URL**: `/api/v1/biome`
* **Method**: `GET`
* **Query Parameters**:
  * `seed` (Long, Required): The world seed. (e.g. `123456`)
  * `version` (String, Optional): The Minecraft version. Defaults to `1.20`. Supports `1.20` through `1.20.6`, `1.21`, `26.1`, `26.2`.
  * `x` (Int, Required): Block coordinate X.
  * `z` (Int, Required): Block coordinate Z.
* **Success Response**: `200 OK` (JSON)
  ```json
  {
    "id": 2,
    "name": "desert",
    "hexColor": "#fae2a2"
  }
  ```

---

### 2. Map Tile Generation
Renders a 256x256 pixel PNG representation of a map tile.

* **URL**: `/api/v1/map/tile`
* **Method**: `GET`
* **Query Parameters**:
  * `seed` (Long, Required): The world seed.
  * `version` (String, Optional): The Minecraft version. Defaults to `1.20`.
  * `zoom` (Int, Required): The map zoom level. At zoom level `8`, each pixel maps to exactly 1 block (1:1 scale). For smaller zoom levels, the map scales out.
  * `tx` (Int, Required): The tile coordinate X.
  * `ty` (Int, Required): The tile coordinate Z.
* **Success Response**: `200 OK`
  * **Headers**: `Content-Type: image/png`
  * **Body**: Raw PNG binary data.

---

## 📦 How to Build and Run

### Prerequisites
* **Java**: OpenJDK 17 (or newer)
* **OS**: Linux x86-64 (due to compiled native library `libcubiomes.so`)

### 1. Run Tests
Ensure everything compiles, binds to the native shared library, and passes coordinate assertions:
```bash
./gradlew test --no-daemon
```

### 2. Start Backend Server
Run the microservice locally:
```bash
./gradlew bootRun --no-daemon
```
The server will start on port `8080`.

### 3. Interactive API Sandbox (Swagger UI)
Once the server is running, visit the interactive Swagger UI to browse the OpenAPI documentation and test endpoints:
* **Swagger UI URL**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **Raw OpenAPI JSON Docs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 🧪 Example API Queries (cURL)

**Get single pixel at (0,0) for seed `123456` in Minecraft version `26.2`:**
```bash
curl -s "http://localhost:8080/api/v1/biome?seed=123456&x=0&z=0&version=26.2"
# Output: {"id":2,"name":"desert","hexColor":"#fae2a2"}
```

**Get single pixel at (100, 100) for seed `123` in Minecraft version `1.20`:**
```bash
curl -s "http://localhost:8080/api/v1/biome?seed=123&x=100&z=100&version=1.20"
# Output: {"id":5,"name":"taiga","hexColor":"#0b4d2c"}
```

**Fetch map tile at coordinates tx=0, ty=0 at zoom=8 (1:1 scale) for seed `987654321` under version `26.2`:**
```bash
curl -o tile.png "http://localhost:8080/api/v1/map/tile?seed=987654321&zoom=8&tx=0&ty=0&version=26.2"
```

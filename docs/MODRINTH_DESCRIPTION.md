# EchoVision

**Hear the world. See its echoes.**

EchoVision is a **client-side Fabric mod** that turns Minecraft into an acoustic world. Activate it and the familiar terrain falls into darkness. Sounds produce expanding, pixelated GLSL waves that briefly trace the edges and shapes of nearby blocks, surfaces, and entities.

Instead of ordinary night vision, you navigate by listening. Footsteps illuminate the ground ahead. A sound in a cave reveals the surrounding walls. A nearby mob can emerge as a fleeting voxel silhouette when an echo passes through it.

## Features

- **Sound-driven vision** — events already heard by Minecraft create localized expanding waves.
- **Pixel-art shader** — procedural GLSL rendering, crisp wave fronts and short afterimages; no full-screen noise overlay.
- **Minecraft-shaped geometry** — highlights actual terrain edges and supports complex block outlines including slabs, stairs and fences.
- **Entity echoes** — nearby entities may be revealed by waves, even when they are not making noise. Some models use simplified fallback shapes.
- **Enhanced hearing** — local amplification for supported sound categories while the effect is active.
- **No gameplay items** — one simple client command controls the effect.

## Commands

```text
/echovision on
/echovision off
```

The command runs locally and does not require server operator permissions. EchoVision is off by default and resets when you disconnect.

## Compatibility and installation

- **Minecraft:** 1.21.1 (Java Edition)
- **Loader:** Fabric
- **Dependencies:** Fabric API
- **Environment:** Client only. Nothing to install on a server.

Install Fabric Loader and Fabric API, then place the EchoVision release `.jar` in your client `mods` folder. Remove old `Warder` development versions first.

## Notes and limitations

- EchoVision visualizes **sounds and entities already known to the client**. It does not receive additional events beyond Minecraft's normal client/server behavior.
- Some modded or complex entity models use approximate shapes.
- Compatibility with other shader/rendering mods has not yet been broadly tested.
- There are no custom networking packets or server-side gameplay changes.

**Author:** Thomas_Draco  
**License:** MIT

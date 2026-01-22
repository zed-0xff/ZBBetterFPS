# ZBBetterFPS

A comprehensive Project Zomboid performance mod that optimizes rendering, logic, and CPU usage through runtime patching.

## ☕ Support the Developer

If you find this mod useful, consider supporting the developer with a coffee!

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/zed_0xff)

## Features

ZBBetterFPS provides several layers of optimization that can be toggled in the Mod Options menu:

- ✅ **Smart Render Distance**: Adjust the number of rendered tiles to fit your hardware.
- ✅ **Optimized Draw Calls**: Merges sprites and 3D models into fewer graphics commands, significantly reducing Render thread overhead.
- ✅ **Low-Level State Caching**: Suppresses redundant graphics state changes (blend modes, shaders, masks) to save CPU time.
- ✅ **Enhanced GPU Buffers**: Increases the internal rendering buffer size to 1MB to reduce frequent GPU flushes.
- ✅ **3D Model & Shader Optimization**: Caches uniform locations and optimizes matrix updates for faster character and terrain rendering.
- ✅ **Streamlined Physics**: An optimized implementation of object separation logic to reduce CPU load during dense physical updates.
- ✅ **Background CPU Throttling**: Prevents "busy-waiting" by lowering CPU usage when the game is paused or minimized.
- ✅ **Uncapped FPS Control**: Tri-state configuration (Default/Force Uncapped/Force Capped) for frame rate settings.

## Requirements

- **[ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy)** - Required framework for Java bytecode manipulation.

## Installation

1. **Prerequisites**: You must have [ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy) installed and configured first.
2. **Enable the mod**: Enable ZBBetterFPS in the Project Zomboid mod manager.

## Usage & Settings

### Graphics Optimizations
- **Optimize IndieGL State**: Caches last applied graphics states to avoid redundant commands.
- **Optimize Sprite Batching**: Merges drawing operations for sprites and 3D models.
- **Optimize RingBuffer**: Increases the GPU upload buffer capacity. (Requires game restart).
- **Optimize Chunk Shaders**: Speeds up terrain rendering by caching shader data.
- **Optimize 3D Models**: Enhances the performance of character and item rendering.

### Logic & CPU Optimizations
- **Background CPU Optimization**: Throttles threads when the window is minimized or the game is paused to prevent 100% CPU usage in the background.
- **Optimize Object Separation**: Uses an efficient math implementation for physics updates.

### Render Distance
Adjust the number of rendered tiles to improve performance:
- **Default**: Resolution-based (typically 152x152 tiles).
- **Range**: From 24x24 to 312x312 tiles.
- **Mod Default**: 104x104 tiles (optimized for balanced performance).
**Note**: You must reload your savegame after changing this setting.

### Uncapped FPS
- **Default**: Uses game settings.
- **Force Uncapped**: Maximizes frame rate.
- **Force Capped**: Ensures frame limiting is active.

## How It Works

ZBBetterFPS uses the ZombieBuddy framework to perform high-performance runtime patching of the game's Java engine. This allows for deep optimizations that are normally impossible with standard Lua mods, all while maintaining compatibility with other mods.

## Credits

All credits go to **[BetterFPS_B42](https://steamcommunity.com/sharedfiles/filedetails/?id=3423115544)** by Alree, which inspired the initial version of this mod. ZBBetterFPS extends these concepts with modern runtime patching and additional low-level optimizations.

## Related Mods

- **[ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy)**: The framework this mod is built on.
- **[ZBLuaPerfMon](https://github.com/zed-0xff/ZBLuaPerfMon)**: Real-time Lua performance monitoring and OSD.

## Links

- **GitHub Repository**: https://github.com/zed-0xff/ZBBetterFPS
- **ZombieBuddy**: https://github.com/zed-0xff/ZombieBuddy

## Disclaimer

This mod modifies core game rendering behavior. Always backup your save files before using mods that modify game functionality. Use at your own risk.

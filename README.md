# ZBBetterFPS

A Project Zomboid mod that optimizes FPS through configurable render distance settings and optional uncapped FPS control.

## ☕ Support the Developer

If you find this mod useful, consider supporting the developer with a coffee!

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/zed_0xff)

## What It Does

ZBBetterFPS allows you to optimize FPS performance in Project Zomboid through two configurable options:
- **Render Distance**: Reduce the number of rendered tiles to improve performance, especially on lower-end systems or in areas with many objects
- **Uncapped FPS**: Optionally force enable or disable uncapped FPS settings

## Features

- ✅ **FPS optimization**: Reduces the number of rendered tiles to improve performance
- ✅ **Uncapped FPS control**: Tri-state configuration (Default/Force Uncapped/Force Capped) for uncapped FPS settings
- ✅ **Runtime patching**: Uses ZombieBuddy to patch the game's render engine at runtime
- ✅ **Configurable**: All settings can be adjusted through the mod options menu

## Requirements

- **[ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy)** - Required framework for Java bytecode manipulation

## Installation

1. **Prerequisites**: You must have [ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy) installed and configured first
2. **Enable the mod**: Enable ZBBetterFPS in the Project Zomboid mod manager

## Usage

### Render Distance
The render distance slider allows you to adjust the number of rendered tiles:
- **Default**: Uses the game's default calculation (based on your screen resolution, typically 152x152 tiles)
- **Minimum**: 24x24 tiles
- **Maximum**: 312x312 tiles

**Default Setting**: The mod defaults to 104x104 tiles. This provides approximately 95% screen coverage at 2336x1460 resolution on minimal zoom, offering a good balance between performance and visual quality.

**Important**: You must reload your savegame after changing the render distance setting for the changes to take effect, as the render engine is initialized during the game load phase. The mod options description will dynamically display the calculated default render distance for your screen resolution.

### Uncapped FPS
The uncapped FPS setting has three options:
- **Default**: Don't modify the uncapped FPS setting (leave it as configured in game options)
- **Force Uncapped**: Enable uncapped FPS by calling `SystemDisabler.setUncappedFPS(true)` and `getCore():setFramerate(1)`
- **Force Capped**: Disable uncapped FPS by calling `SystemDisabler.setUncappedFPS(false)`

The uncapped FPS setting takes effect immediately when applied through the mod options menu.

## How It Works

ZBBetterFPS uses ZombieBuddy to patch the `IsoChunkMap.CalcChunkWidth` method, which controls the render distance. Additionally, it provides a configuration option to control the uncapped FPS setting through `SystemDisabler.setUncappedFPS()` and `getCore():setFramerate()`.

## Technical Details

- **Render Distance**:
  - Range: Default (game default, typically 152x152 tiles) to 312x312 tiles
  - Minimum size: 24x24 tiles
  - Maximum size: 312x312 tiles
  - Game default: 152x152 tiles
  - Mod default: 104x104 tiles - optimized for ~95% screen coverage at 2336x1460 resolution on minimal zoom
  - Default option uses the game's default calculation (same as `IsoChunkMap.CalcChunkWidth()`, typically 152x152 tiles)
  - The description dynamically displays the calculated default render distance for your screen resolution
  - Patches `zombie.iso.IsoChunkMap.CalcChunkWidth` method
  - Uses ByteBuddy for runtime bytecode manipulation
  - Values are converted to odd numbers internally (required for proper tile rendering)

- **Uncapped FPS**:
  - Tri-state configuration (Default/Force Uncapped/Force Capped)
  - When Force Uncapped: Calls `SystemDisabler.setUncappedFPS(true)` and `getCore():setFramerate(1)`
  - When Force Capped: Calls `SystemDisabler.setUncappedFPS(false)`
  - When Default: No modification to the uncapped FPS setting

## Development History

Initially, I wanted to make ZBBetterFPS as an add-on to the original ZBetterFPS mod. However, maintaining compatibility with both the mod's and game's code resulted in the implementation being too fragile. As a result, I decided to make it a standalone, non-compatible implementation. This approach allowed for a cleaner codebase. Despite this, all credits go to **BetterFPS_B42** by Alree, which served as the inspiration for this mod.

## Credits

All credits go to **[BetterFPS_B42](https://steamcommunity.com/sharedfiles/filedetails/?id=3423115544)** by Alree, which inspired this mod. ZBBetterFPS provides a ZombieBuddy-based implementation that allows runtime configuration without requiring manual file replacement.

## Related Mods

Other mods built with ZombieBuddy:

- **[ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy)**: The framework this mod is built on
- **[ZBLuaPerfMon](https://github.com/zed-0xff/ZBLuaPerfMon)**: Real-time Lua performance monitoring and OSD
- **[ZBHelloWorld](https://github.com/zed-0xff/ZBHelloWorld)**: A simple example mod demonstrating patches-only mods and UI patching
- **[ZBetterWorkshopUpload](https://github.com/zed-0xff/ZBetterWorkshopUpload)**: Filters unwanted files from Steam Workshop uploads and provides upload previews
- **[ZBMacOSHideMenuBar](https://github.com/zed-0xff/ZBMacOSHideMenuBar)**: Fixes the macOS menu bar issue in borderless windowed mode

## Links

- **GitHub Repository**: https://github.com/zed-0xff/ZBBetterFPS
- **ZombieBuddy**: https://github.com/zed-0xff/ZombieBuddy
- **BetterFPS_B42**: https://steamcommunity.com/sharedfiles/filedetails/?id=3423115544

## Disclaimer

This mod modifies core game rendering behavior. Always backup your save files before using mods that modify game functionality. Use at your own risk.


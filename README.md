# ZBBetterFPS

A Project Zomboid mod that optimizes FPS by decreasing the number of rendered tiles through configurable render distance settings.

## ☕ Support the Developer

If you find this mod useful, consider supporting the developer with a coffee!

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/zed_0xff)

## What It Does

ZBBetterFPS allows you to reduce the render distance in Project Zomboid, which decreases the number of tiles rendered and can significantly improve FPS performance, especially on lower-end systems or in areas with many objects.

## Features

- ✅ **FPS optimization**: Reduces the number of rendered tiles to improve performance
- ✅ **Runtime patching**: Uses ZombieBuddy to patch the game's render engine at runtime
- ✅ **Non-intrusive**: Does not modify the `UncappedFPS` global setting

## Requirements

- **[ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy)** - Required framework for Java bytecode manipulation

## Installation

1. **Prerequisites**: You must have [ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy) installed and configured first
2. **Enable the mod**: Enable ZBBetterFPS in the Project Zomboid mod manager

## Usage

**Important**: You must reload your savegame after changing the render distance setting for the changes to take effect, as the render engine is initialized during the game load phase.

## How It Works

ZBBetterFPS uses ZombieBuddy to patch the `IsoChunkMap.CalcChunkWidth` method, which controls the render distance.

## Technical Details

- Render distance range: Extended from 16x16 to 400x400 (default: 128x128)
- Patches `zombie.iso.IsoChunkMap.CalcChunkWidth` method
- Uses ByteBuddy for runtime bytecode manipulation
- Does not change the value of the `UncappedFPS` global setting

## Development History

Initially, I wanted to make ZBBetterFPS as an add-on to the original ZBetterFPS mod. However, maintaining compatibility with both the mod's and game's code resulted in the implementation being too fragile. As a result, I decided to make it a standalone, non-compatible implementation. This approach allowed for a cleaner codebase. Despite this, all credits go to **BetterFPS_B42** by Alree, which served as the inspiration for this mod.

## Credits

All credits go to **[BetterFPS_B42](https://steamcommunity.com/sharedfiles/filedetails/?id=3423115544)** by Alree, which inspired this mod. ZBBetterFPS provides a ZombieBuddy-based implementation that allows runtime configuration without requiring manual file replacement.

## Links

- **GitHub Repository**: https://github.com/zed-0xff/ZBBetterFPS (if applicable)
- **ZombieBuddy**: https://github.com/zed-0xff/ZombieBuddy
- **BetterFPS_B42**: https://steamcommunity.com/sharedfiles/filedetails/?id=3423115544

## Disclaimer

This mod modifies core game rendering behavior. Always backup your save files before using mods that modify game functionality. Use at your own risk.


# ReTerraForged
a 1.19+ continuation of https://github.com/TerraForged/TerraForged

## For modpack authors

A modpack can choose the terrain its players start with, without taking away their ability to change it. Everything
lives in the `config/reterraforged` folder, in files the mod never writes, so a modpack update can replace them without
touching anything a player saved.

1. Tune a preset in the Terrain tab of Create World and click **Save As...**. It's written to
   `config/reterraforged/presets/<name>.json`.
2. Move that file to `config/reterraforged/modpack-presets/`. Presets in this folder appear first in the preset list
   and can't be deleted or overwritten from the game. Optionally add `"name"` and `"description"` fields next to the
   settings; they are shown in the preset list and may be translation keys.
3. Create `config/reterraforged/modpack.json`:

   ```json
   {
     "defaultPreset": "My Preset",
     "showBuiltinPresets": true,
     "useAsDefaultWorldType": true
   }
   ```

   | Key | Default | Meaning |
   | --- | --- | --- |
   | `defaultPreset` | `default` | The preset new worlds start from: a file name in `modpack-presets` (without `.json`), a built-in preset such as `highlands`, or a full id such as `builtin/highlands`. Unknown names fall back to Default and log a warning. |
   | `showBuiltinPresets` | `true` | Set to `false` to offer only the modpack's presets. Ignored if `modpack-presets` has none. |
   | `useAsDefaultWorldType` | `true` | Whether ReTerraForged is the selected world type in Create World. A player's own `client.json` takes precedence. |

Players still start from the modpack's default, can switch to any other preset, edit every setting, and save their own
presets. Dedicated servers that create a new world with `level-type=reterraforged:reterraforged` seed
`config/reterraforged/server-preset.json` from the same default the first time; ship that file directly to pin a
server's terrain.

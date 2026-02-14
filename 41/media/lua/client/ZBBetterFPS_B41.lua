-- ZBBetterFPS options using ModOptions (older API, compatible with ModOptions mod)
-- Requires: ModOptions (!ModOptionsEngine.lua in shared, !!CustomOptions.lua in client)
local MOD_ID   = "ZBBetterFPS"
local MOD_NAME = "Zed's Better FPS"

local isDebug = getCore():getDebug()
local chunksPerWidth = 0

if ZBBetterFPS and ZBBetterFPS.getChunksPerWidth then
    chunksPerWidth = ZBBetterFPS.getChunksPerWidth()
end
if not chunksPerWidth or chunksPerWidth <= 0 then
    chunksPerWidth = 10 -- B41
end

-- XXX numericValue has to be odd AND > 1, or tile rendering will break; 0 means "default"
local function toOddValue(x)
    if x == 0 then return 0 end
    if x < 0 then return 0 end
    return x * 2 + 1
end

-- Bandits spawn at 55–65 tiles; need ChunkGridWidth*5 >= 65 => ChunkGridWidth >= 13
local MIN_CHUNK_GRID_FOR_BANDITS = 13

local function isBanditsModActive()
    if getActivatedMods and type(getActivatedMods) == "function" then
        local mods = getActivatedMods()
        if mods then
            if mods.size and type(mods.size) == "function" then
                for i = 0, mods:size() - 1 do
                    if mods:get(i) == "Bandits2" then return true end
                end
            elseif type(mods) == "table" then
                for _, id in ipairs(mods) do
                    if id == "Bandits2" then return true end
                end
            end
        end
    end
    return (Bandit ~= nil) or (BanditServer ~= nil)
end

local function clampRenderDistanceForBandits(numericValue)
    if numericValue == 0 then return numericValue end
    if not isBanditsModActive() then return numericValue end
    if numericValue >= MIN_CHUNK_GRID_FOR_BANDITS then return numericValue end
    print("[ZBBetterFPS] Bandits mod detected: limiting minimum render distance to " .. tostring(MIN_CHUNK_GRID_FOR_BANDITS * chunksPerWidth) .. " tiles for compatibility.")
    return MIN_CHUNK_GRID_FOR_BANDITS
end

-- Build render distance combo list: index 1 = Default, 2-20 = 8, 16, ... 152 tiles (translated strings for Mod Options combo)
local renderDistanceChoices = { getText("UI_options_ZBBetterFPS_uncappedFPS_default") }
for i = 1, 19 do
    renderDistanceChoices[i + 1] = tostring(toOddValue(i) * chunksPerWidth)
end

-- Mod Options (B41) expects combo choices as numeric indices [1], [2], ... with getText() display strings, not translation keys
local chunk = {
    mod_id = MOD_ID,
    mod_shortname = MOD_ID,
    mod_fullname = MOD_NAME,
    options = {
        optimizeIndieGL = false,
        optimizeSpriteBatching = false,
        optimizeRingBuffer = false,
        optimizeIsoMovingObject = false,
        lowerCPUMode = 1,       -- 1-based index: 1=pausedOrBackground, 2=pausedAndBackground, 3=always, 4=never
        renderDistance = 1,     -- 1-based index: 1=default, 2-20=slider 1-19
        uncappedFPS = 1,        -- 1=Default, 2=Uncapped, 3=Capped
        enableMetrics = false,
    },
    options_data = {
        optimizeIndieGL = { name = "UI_options_ZBBetterFPS_optimizeIndieGL", tooltip = "UI_options_ZBBetterFPS_optimizeIndieGL_desc" },
        optimizeSpriteBatching = { name = "UI_options_ZBBetterFPS_optimizeSpriteBatching", tooltip = "UI_options_ZBBetterFPS_optimizeSpriteBatching_desc" },
        optimizeRingBuffer = { name = "UI_options_ZBBetterFPS_optimizeRingBuffer", tooltip = "UI_options_ZBBetterFPS_optimizeRingBuffer_desc" },
        optimizeIsoMovingObject = { name = "UI_options_ZBBetterFPS_optimizeIsoMovingObject", tooltip = "UI_options_ZBBetterFPS_optimizeIsoMovingObject_desc" },
        lowerCPUMode = {
            getText("UI_options_ZBBetterFPS_lowerCPUMode_pausedOrBackground"),
            getText("UI_options_ZBBetterFPS_lowerCPUMode_pausedAndBackground"),
            getText("UI_options_ZBBetterFPS_lowerCPUMode_always"),
            getText("UI_options_ZBBetterFPS_lowerCPUMode_never"),
            name = "UI_options_ZBBetterFPS_lowerCPUMode",
            tooltip = "UI_options_ZBBetterFPS_lowerCPUMode_desc",
            default = 1,
        },
        renderDistance = (function()
            local t = { name = "UI_options_ZBBetterFPS_renderDistance", tooltip = "UI_options_ZBBetterFPS_renderDistance_desc", default = 1 }
            for i = 1, 20 do t[i] = renderDistanceChoices[i] end
            return t
        end)(),
        uncappedFPS = {
            getText("UI_options_ZBBetterFPS_uncappedFPS_default"),
            getText("UI_options_ZBBetterFPS_uncappedFPS_enabled"),
            getText("UI_options_ZBBetterFPS_uncappedFPS_disabled"),
            name = "UI_options_ZBBetterFPS_uncappedFPS",
            tooltip = "UI_options_ZBBetterFPS_uncappedFPS_desc",
            default = 1,
        },
        enableMetrics = { name = "UI_options_ZBBetterFPS_enableMetrics", tooltip = "UI_options_ZBBetterFPS_enableMetrics_desc" },
    },
}

-- Remove enableMetrics from options_data if not debug (old API shows all options in options_data; hide by not registering)
if not isDebug then
    chunk.options.enableMetrics = nil
    chunk.options_data.enableMetrics = nil
end

local options = ModOptions:getInstance(chunk)
if ZBBetterFPS then
    ZBBetterFPS.options = options
end

local function applyZBBetterFPSSettings(callee)
    print("[ZBBetterFPS] applying settings... (" .. tostring(callee) .. ")")
    local opts = options.options
    local data = options.options_data

    -- Uncapped FPS: 1=Default, 2=Uncapped, 3=Capped
    local uncappedIndex = opts.uncappedFPS or 1
    if uncappedIndex == 2 then
        print("[ZBBetterFPS] Setting uncapped FPS to true")
        if SystemDisabler and type(SystemDisabler.setUncappedFPS) == "function" then
            SystemDisabler.setUncappedFPS(true)
        end
        if getCore and type(getCore().setFramerate) == "function" then
            getCore():setFramerate(1)
        end
    elseif uncappedIndex == 3 then
        print("[ZBBetterFPS] Setting uncapped FPS to false")
        if SystemDisabler and type(SystemDisabler.setUncappedFPS) == "function" then
            SystemDisabler.setUncappedFPS(false)
        end
    end

    -- Java/ZBBetterFPS optimizations
    if ZBBetterFPS and type(ZBBetterFPS.setOptimizeIndieGL) == "function" then
        ZBBetterFPS.setOptimizeIndieGL(opts.optimizeIndieGL == true)
        ZBBetterFPS.setOptimizeSpriteBatching(opts.optimizeSpriteBatching == true)
        ZBBetterFPS.setOptimizeRingBuffer(opts.optimizeRingBuffer == true)
        ZBBetterFPS.setOptimizeIsoMovingObject(opts.optimizeIsoMovingObject == true)
        ZBBetterFPS.setLowerCPUMode(opts.lowerCPUMode)
        if ZBBetterFPS.setEnableMetrics and data.enableMetrics then
            ZBBetterFPS.setEnableMetrics(opts.enableMetrics == true)
        end
    else
        print("[ZBBetterFPS] Error! type(ZBBetterFPS) = " .. tostring(type(ZBBetterFPS)))
    end

    -- Render distance: combo index 1 = default (0), 2-20 = slider 1-19
    local rdIndex = opts.renderDistance or 1
    local numericValue = (rdIndex == 1) and 0 or (rdIndex - 1)
    numericValue = toOddValue(numericValue)
    numericValue = clampRenderDistanceForBandits(numericValue)
    if numericValue and ZBBetterFPS and type(ZBBetterFPS.setMaxRenderDistance) == "function" then
        print("[ZBBetterFPS] ZBBetterFPS.setMaxRenderDistance(" .. tostring(numericValue) .. ")")
        ZBBetterFPS.setMaxRenderDistance(numericValue)
    end
end

function options:OnApply()
    applyZBBetterFPSSettings("OnApply")
end

-- B41 ModOptions loads ini data only when game Options screen is opened, and in OnGameStart event
-- but user may not open the Options screen, and OnGameStart is too late for us, so here we call loadIniData() explicitly
Events.OnGameBoot.Add(function()
    ModOptions.loadFile()
    applyZBBetterFPSSettings("OnGameBoot")
end)

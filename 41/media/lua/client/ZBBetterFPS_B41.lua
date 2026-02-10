-- ZBBetterFPS options using ModOptions (older API, compatible with ModOptions mod)
-- Requires: ModOptions (!ModOptionsEngine.lua in shared, !!CustomOptions.lua in client)
local MOD_ID   = "ZBBetterFPS"
local MOD_NAME = "Zed's Better FPS"

local isDebug = getCore():getDebug()

-- XXX numericValue has to be odd AND > 1, or tile rendering will break; 0 means "default"
local function toOddValue(x)
    if x == 0 then return 0 end
    if x < 0 then return 0 end
    return x * 2 + 1
end

-- Build render distance combo list: index 1 = Default, 2-20 = 8, 16, ... 152 tiles
local renderDistanceChoices = { getText("UI_options_ZBBetterFPS_uncappedFPS_default") }
for i = 1, 19 do
    renderDistanceChoices[i + 1] = tostring(toOddValue(i) * 8)
end

local chunk = {
    mod_id = MOD_ID,
    mod_shortname = MOD_ID,
    mod_fullname = MOD_NAME,
    options = {
        optimizeIndieGL = false,
        optimizeSpriteBatching = false,
        optimizeRingBuffer = false,
        optimizeDefaultShader = false,
        optimize3DModels = false,
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
        optimizeDefaultShader = { name = "UI_options_ZBBetterFPS_optimizeDefaultShader", tooltip = "UI_options_ZBBetterFPS_optimizeDefaultShader_desc" },
        optimize3DModels = { name = "UI_options_ZBBetterFPS_optimize3DModels", tooltip = "UI_options_ZBBetterFPS_optimize3DModels_desc" },
        optimizeIsoMovingObject = { name = "UI_options_ZBBetterFPS_optimizeIsoMovingObject", tooltip = "UI_options_ZBBetterFPS_optimizeIsoMovingObject_desc" },
        lowerCPUMode = {
            name = "UI_options_ZBBetterFPS_lowerCPUMode",
            tooltip = "UI_options_ZBBetterFPS_lowerCPUMode_desc",
            [1] = {
                "UI_options_ZBBetterFPS_lowerCPUMode_pausedOrBackground",
                "UI_options_ZBBetterFPS_lowerCPUMode_pausedAndBackground",
                "UI_options_ZBBetterFPS_lowerCPUMode_always",
                "UI_options_ZBBetterFPS_lowerCPUMode_never",
            },
            default = 1,
        },
        renderDistance = {
            name = "UI_options_ZBBetterFPS_renderDistance",
            tooltip = "UI_options_ZBBetterFPS_renderDistance_desc",
            [1] = renderDistanceChoices,
            default = 1,
        },
        uncappedFPS = {
            name = "UI_options_ZBBetterFPS_uncappedFPS",
            tooltip = "UI_options_ZBBetterFPS_uncappedFPS_desc",
            [1] = {
                "UI_options_ZBBetterFPS_uncappedFPS_default",
                "UI_options_ZBBetterFPS_uncappedFPS_enabled",
                "UI_options_ZBBetterFPS_uncappedFPS_disabled",
            },
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

local function applyZBBetterFPSSettings()
    print("[ZBBetterFPS] applying settings...")
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
        ZBBetterFPS.setOptimizeDefaultShader(opts.optimizeDefaultShader == true)
        ZBBetterFPS.setOptimize3DModels(opts.optimize3DModels == true)
        ZBBetterFPS.setOptimizeIsoMovingObject(opts.optimizeIsoMovingObject == true)
        -- lowerCPUMode: index 2 = true (pausedAndBackground), else false
        ZBBetterFPS.setLowerCPUMode(opts.lowerCPUMode == 2)
        if ZBBetterFPS.setEnableMetrics and data.enableMetrics then
            ZBBetterFPS.setEnableMetrics(opts.enableMetrics == true)
        end
    end

    -- Render distance: combo index 1 = default (0), 2-20 = slider 1-19
    local rdIndex = opts.renderDistance or 1
    local numericValue = (rdIndex == 1) and 0 or (rdIndex - 1)
    numericValue = toOddValue(numericValue)
    if numericValue and ZBBetterFPS and type(ZBBetterFPS.setMaxRenderDistance) == "function" then
        print("[ZBBetterFPS] ZBBetterFPS.setMaxRenderDistance(" .. tostring(numericValue) .. ")")
        ZBBetterFPS.setMaxRenderDistance(numericValue)
    end
end

function options:OnApply()
    applyZBBetterFPSSettings()
end

Events.OnMainMenuEnter.Add(function()
    applyZBBetterFPSSettings()
end)

Events.OnGameStart.Add(function()
    applyZBBetterFPSSettings()
end)

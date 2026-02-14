local MOD_ID   = "ZBBetterFPS"
local MOD_NAME = "Zed's Better FPS"

local isDebug = getCore():getDebug()

local config = {
    renderDistance = nil,
    uncappedFPS = nil,
    optimizeIndieGL = nil,
    optimizeSpriteBatching = nil,
    optimizeRingBuffer = nil,
    optimizeDefaultShader = nil,
    optimize3DModels = nil,
    optimizeIsoMovingObject = nil,
    lowerCPUMode = nil,
    enableMetrics = nil,
}

local options = PZAPI.ModOptions:create(MOD_ID, MOD_NAME)

local function getDefaultRenderDistance()
    -- copied from java IsoChunkMap.CalcChunkWidth()
    local delx = getCore():getScreenWidth() / 1920.0
    local dely = getCore():getScreenHeight() / 1080.0
    local del = math.max(delx, dely)
    if del > 1.0 then
        del = 1.0
    end
    local chunkGridWidth = math.floor(13.0 * del * 1.5)
    if (chunkGridWidth / 2) * 2 == chunkGridWidth then
        chunkGridWidth = chunkGridWidth + 1
    end
    chunkGridWidth = math.min(chunkGridWidth, 19)
    return chunkGridWidth * 8
end

-- XXX numericValue has to be odd AND > 1, or tile rendering will break
-- 0 means "default"
local function toOddValue(x)
    if x == 0 then return 0 end
    if x < 0  then return 0 end

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
    print("[ZBBetterFPS] Bandits mod detected: limiting minimum render distance to " .. tostring(MIN_CHUNK_GRID_FOR_BANDITS * 10) .. " tiles for compatibility.")
    return MIN_CHUNK_GRID_FOR_BANDITS
end

local function updateSlider(slider, newValue)
    if not slider then return end
    if not slider.element then return end
    if not slider.element.label then return end
    if not slider.element.label.setName then return end

    if not slider.element.label.setName_ZBBetterFPS then
        slider.element.label.setName_ZBBetterFPS = slider.element.label.setName
        slider.element.label.setName = function(self, name) end
    end

    newValue = newValue or slider:getValue()
    local text
    if newValue == 0 then
        text = getText("UI_options_ZBBetterFPS_uncappedFPS_default")
    else
        local x = toOddValue(newValue) * 8 -- x8 because UI shows distance in tiles
        text = tostring(x)
    end
    
    local label = slider.element.label
    label:setName_ZBBetterFPS(text)
end

config.optimizeIndieGL = options:addTickBox("optimizeIndieGL", "UI_options_ZBBetterFPS_optimizeIndieGL", false, "UI_options_ZBBetterFPS_optimizeIndieGL_desc")
config.optimizeSpriteBatching = options:addTickBox("optimizeSpriteBatching", "UI_options_ZBBetterFPS_optimizeSpriteBatching", false, "UI_options_ZBBetterFPS_optimizeSpriteBatching_desc")
config.optimizeRingBuffer = options:addTickBox("optimizeRingBuffer", "UI_options_ZBBetterFPS_optimizeRingBuffer", false, "UI_options_ZBBetterFPS_optimizeRingBuffer_desc")
config.optimizeDefaultShader = options:addTickBox("optimizeDefaultShader", "UI_options_ZBBetterFPS_optimizeDefaultShader", false, "UI_options_ZBBetterFPS_optimizeDefaultShader_desc")
config.optimize3DModels = options:addTickBox("optimize3DModels", "UI_options_ZBBetterFPS_optimize3DModels", false, "UI_options_ZBBetterFPS_optimize3DModels_desc")
config.optimizeIsoMovingObject = options:addTickBox("optimizeIsoMovingObject", "UI_options_ZBBetterFPS_optimizeIsoMovingObject", false, "UI_options_ZBBetterFPS_optimizeIsoMovingObject_desc")

config.lowerCPUMode = options:addComboBox("lowerCPUMode", "UI_options_ZBBetterFPS_lowerCPUMode", "UI_options_ZBBetterFPS_lowerCPUMode_desc")
config.lowerCPUMode:addItem("UI_options_ZBBetterFPS_lowerCPUMode_pausedOrBackground", false)
config.lowerCPUMode:addItem("UI_options_ZBBetterFPS_lowerCPUMode_pausedAndBackground", true)
config.lowerCPUMode:addItem("UI_options_ZBBetterFPS_lowerCPUMode_always", false)
config.lowerCPUMode:addItem("UI_options_ZBBetterFPS_lowerCPUMode_never", false)

-- default to 0 = use game default render distance
config.renderDistance = options:addSlider("renderDistance", "UI_options_ZBBetterFPS_renderDistance", 0, 19, 1, 0)
config.renderDistance.getValue = function(self)
    updateSlider(self, self.value)
    return self.value
end
config.renderDistance.setValue = function(self, value)
    self.value = value
    updateSlider(self, value)
end
config.renderDistance.onChange = function(self, newValue)
    updateSlider(self, newValue)
end
options:addDescription(getText("UI_options_ZBBetterFPS_renderDistance_desc", getDefaultRenderDistance()))

config.uncappedFPS = options:addComboBox("uncappedFPS", "UI_options_ZBBetterFPS_uncappedFPS", "UI_options_ZBBetterFPS_uncappedFPS_desc")
config.uncappedFPS:addItem("UI_options_ZBBetterFPS_uncappedFPS_default", true)
config.uncappedFPS:addItem("UI_options_ZBBetterFPS_uncappedFPS_enabled", false)
config.uncappedFPS:addItem("UI_options_ZBBetterFPS_uncappedFPS_disabled", false)

if isDebug then
    config.enableMetrics = options:addTickBox("enableMetrics", "UI_options_ZBBetterFPS_enableMetrics", false, "UI_options_ZBBetterFPS_enableMetrics_desc")
end

options.apply = function(self)
    print("[ZBBetterFPS] applying settings...")
    updateSlider(config.renderDistance)

    -- Apply uncapped FPS setting (tri-state: 1=Default, 2=Force Uncapped, 3=Force Capped)
    if config.uncappedFPS then
        local selectedIndex = config.uncappedFPS:getValue()
        if selectedIndex == 2 then
            -- Force Uncapped
            print("[ZBBetterFPS] Setting uncapped FPS to true")
            if SystemDisabler then
                if type(SystemDisabler.setUncappedFPS) == "function" then
                    SystemDisabler.setUncappedFPS(true)
                else
                    print("[ZBBetterFPS] invalid type of SystemDisabler.setUncappedFPS (" .. tostring(type(SystemDisabler.setUncappedFPS)) .. ")!")
                end
            else
                print("[ZBBetterFPS] can't find SystemDisabler class!")
            end
            if getCore then
                if type(getCore().setFramerate) == "function" then
                    getCore():setFramerate(1)
                    print("[ZBBetterFPS] Set framerate to 1 (uncapped)")
                else
                    print("[ZBBetterFPS] invalid type of getCore().setFramerate (" .. tostring(type(getCore().setFramerate)) .. ")!")
                end
            else
                print("[ZBBetterFPS] can't find getCore()!")
            end
        elseif selectedIndex == 3 then
            -- Force Capped
            print("[ZBBetterFPS] Setting uncapped FPS to false")
            if SystemDisabler then
                if type(SystemDisabler.setUncappedFPS) == "function" then
                    SystemDisabler.setUncappedFPS(false)
                else
                    print("[ZBBetterFPS] invalid type of SystemDisabler.setUncappedFPS (" .. tostring(type(SystemDisabler.setUncappedFPS)) .. ")!")
                end
            else
                print("[ZBBetterFPS] can't find SystemDisabler class!")
            end
        else
            -- Default (index 1) - don't modify
            print("[ZBBetterFPS] Uncapped FPS set to Default (not modifying)")
        end
    end

    if ZBBetterFPS then
        if type(ZBBetterFPS.setOptimizeIndieGL) == "function" then
            ZBBetterFPS.setOptimizeIndieGL(config.optimizeIndieGL:getValue())
            ZBBetterFPS.setOptimizeSpriteBatching(config.optimizeSpriteBatching:getValue())
            ZBBetterFPS.setOptimizeRingBuffer(config.optimizeRingBuffer:getValue())
            ZBBetterFPS.setOptimizeDefaultShader(config.optimizeDefaultShader:getValue())
            ZBBetterFPS.setOptimize3DModels(config.optimize3DModels:getValue())
            ZBBetterFPS.setOptimizeIsoMovingObject(config.optimizeIsoMovingObject:getValue())
            ZBBetterFPS.setLowerCPUMode(config.lowerCPUMode:getValue())

            if ZBBetterFPS.setEnableMetrics and config.enableMetrics then
                ZBBetterFPS.setEnableMetrics(config.enableMetrics:getValue())
            end
        end
    end

    -- Apply render distance setting
    local numericValue = config.renderDistance:getValue()
    print("[ZBBetterFPS] Retrieved render distance value " .. tostring(numericValue))
    if not numericValue then return end

    numericValue = toOddValue(numericValue)
    numericValue = clampRenderDistanceForBandits(numericValue)

    if numericValue then
        print("[ZBBetterFPS] calling ZBBetterFPS.setMaxRenderDistance( " .. tostring(numericValue) .. " )")
        if ZBBetterFPS then
            if type(ZBBetterFPS.setMaxRenderDistance) == "function" then
                ZBBetterFPS.setMaxRenderDistance(numericValue)
            else
                print("[ZBBetterFPS] invalid type of ZBBetterFPS.setMaxRenderDistance (" .. tostring(type(ZBBetterFPS.setMaxRenderDistance)) .. ")! Check ZombieBuddy mod installation.")
            end
		else
			print("[ZBBetterFPS] can't find ZBBetterFPS class! Check ZombieBuddy mod installation.")
		end
    end
end

Events.OnGameBoot.Add(function()
    options:apply()
end)

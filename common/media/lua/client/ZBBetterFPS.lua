local MOD_ID   = "ZBBetterFPS"
local MOD_NAME = "Zed's Better FPS"

local config = {
    renderDistance = nil,
    uncappedFPS = nil,
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

-- setting default to 6 => 104x104, 95% of screen in 2336x1460 resolution on minimal zoom
config.renderDistance = options:addSlider("renderDistance", "UI_options_ZBBetterFPS_renderDistance", 0, 19, 1, 6)
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

    -- Apply render distance setting
    local numericValue = config.renderDistance:getValue()
    print("[ZBBetterFPS] Retrieved render distance value " .. tostring(numericValue))
    if not numericValue then return end

    numericValue = toOddValue(numericValue)

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

Events.OnMainMenuEnter.Add(function()
    options:apply()
end)

-- mostly for patching options label text
Events.OnGameStart.Add(function()
    options:apply()
end)

local MOD_ID   = "ZBBetterFPS"
local MOD_NAME = "Zed's Better FPS Options"

local config = {
    renderDistance = nil,
}

local options = PZAPI.ModOptions:create(MOD_ID, MOD_NAME)

config.renderDistance = options:addSlider("renderDistance", getText("UI_options_ZBBetterFPS"), 16, 400, 8, 128)
options:addDescription(getText("UI_options_ZBBetterFPS_descr"))

options.apply = function(self)
    print("[ZBBetterFPS] applying settings...")

    local numericValue = config.renderDistance:getValue() / 8
    print("[ZBBetterFPS] Retrieved render distance value " .. tostring(numericValue))
    if not numericValue then return end

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


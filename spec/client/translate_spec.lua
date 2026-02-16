describe("ZBBetterFPS", function()
    KEY = "UI_options_ZBBetterFPS_renderDistance"
    it("translates " .. KEY, function()
        assert.eq("Render distance", getText(KEY))
    end)
end)

return ZBSpec.run()

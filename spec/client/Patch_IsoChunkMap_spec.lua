describe("Patch_IsoChunkMap", function()
    local cpw
    if getCore():getGameVersion():getMajor() == 41 then
        cpw = 10
    else
        cpw = 8
    end

    it("has chunksPerWidth of " .. tostring(cpw), function()
        assert.is_equal(cpw, ZBBetterFPS.getChunksPerWidth())
    end)

    it("sets chunkWidthInTiles to " .. tostring(cpw*3) .. " (min)", function()
        assert.is_equal(cpw*3, getCell():getChunkMap(0):getWidthInTiles())
    end)
end)

return ZBSpec.run()

describe("Patch_IsoChunkMap", function()
    local CPW
    if getCore():getGameVersion():getMajor() == 41 then
        CPW = 10
    else
        CPW = 8
    end

    local CGW = 3

    -- min value is preset in loaded modconfig

    context("java side", function()
        it("has chunksPerWidth of " .. tostring(CPW), function()
            assert.is_equal(CPW, ZBBetterFPS.getChunksPerWidth())
        end)

        it("sets chunkGridWidth to " .. tostring(CGW), function()
            assert.is_equal(CGW, IsoChunkMap.class:zbGet('ChunkGridWidth') or IsoChunkMap.class:zbGet('chunkGridWidth'))
        end)

        it("sets chunkWidthInTiles to " .. tostring(CPW*3), function()
            assert.is_equal(CPW*3, getCell():getChunkMap(0):getWidthInTiles())
        end)
    end)

    local function check_userdata(obj, expected_value, ...)
        local names = {...}
        local found = false
        for _, name in ipairs(names) do
            local field = obj[name]
            if field then
                assert.is_equal("userdata", type(field))
                assert.is_equal(expected_value, field:intValue())
                found = true
                break
            end
        end
        assert(found, "None of the fields " .. table.concat(names, ", ") .. " were found in the object")
    end

    context("lua side IsoChunkMap", function()
        check_userdata(IsoChunkMap, CPW,   "ChunksPerWidth",    "CHUNKS_PER_WIDTH")
        check_userdata(IsoChunkMap, CPW*3, "ChunkWidthInTiles", "chunkWidthInTiles")
    end)
end)

return ZBSpec.run()

-- Stub spec (created by zbspec --init)
require "ZBSpec"

describe("ZBBetterFPS", function()
    it("is defined", function()
        assert(ZBBetterFPS)
    end)

    describe(".getChunksPerWidth()", function()
        it("returns correct number", function()
            if getCore():getGameVersion():getMajor() == 41 then
                assert.is_equal(10, ZBBetterFPS.getChunksPerWidth())
            else
                assert.is_equal( 8, ZBBetterFPS.getChunksPerWidth())
            end
        end)
    end)
end)

return ZBSpec.run()

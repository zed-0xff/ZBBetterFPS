describe("ZBBetterFPS", function()
    if isServer() then
        it("is NOT defined on server", function()
            assert.is_nil(ZBBetterFPS)
        end)
    else
        it("is defined on SP", function()
            assert.is_not_nil(ZBBetterFPS)
        end)
    end
end)

return ZBSpec.run()

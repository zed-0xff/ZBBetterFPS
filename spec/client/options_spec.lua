describe("ZBBetterFPS ModOptions", function()
    if getCore():getGameVersion():getMajor() == 41 then
        -- B41
        it("has renderDistance set to 2", function() -- from test ini file
            assert.is_equal(2, ZBBetterFPS.options.options.renderDistance)
        end)
    else
        -- B42+
        it("has renderDistance set to 1", function() -- from test ini file
            assert.is_equal(1, ZBBetterFPS.options:getOption("renderDistance"):getValue())
        end)
    end
end)

return ZBSpec.run()

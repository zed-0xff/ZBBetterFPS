local is_B41 = getCore():getGameVersion():getMajor() == 41

describe("ZBBetterFPS ModOptions", function()
    if is_B41 then
        -- B41
        it("has renderDistance set to 2", function() -- from test ini file
            assert.eq(2, ZBBetterFPS.options.options.renderDistance)
        end)
    else
        -- B42+
        it("has renderDistance set to 1", function() -- from test ini file
            assert.eq(1, ZBBetterFPS.options:getOption("renderDistance"):getValue())
        end)
    end
end)

describe("ZBBetterFPS java object", function()
    local expected_params = {
        g_MaxRenderDistance       = 3,
        g_OptimizeIndieGL         = true,
        g_OptimizeSpriteBatching  = true,
        g_OptimizeRingBuffer      = true,
        g_OptimizeDefaultShader   = not is_B41,
        g_Optimize3DModels        = not is_B41,
        g_OptimizeIsoMovingObject = true,
        g_LowerCPUMode            = ZBBetterFPS.class:zbGet('CPU_MODE_ALWAYS'),
        g_EnableMetrics           = false,
    }

    -- cannot use just ZBBetterFPS[key] because it is actually a Lua mirror-object, and it lies
    for key, value in pairs(expected_params) do
        it("has " .. key .. " set to " .. tostring(value), function()
            assert.eq(value, ZBBetterFPS.class:zbGet(key))
        end)
    end
end)

return ZBSpec.run()

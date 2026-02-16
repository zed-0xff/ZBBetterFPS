local patches = {
    Patch_DefaultShader             = false,
    Patch_IndieGL                   = true,
    Patch_IsoChunkMap               = true,
    Patch_IsoMovingObject           = true,
    Patch_MainLoop                  = true,
    Patch_RingBuffer_IsStateChanged = true,
    Patch_RingBuffer                = true,
    Patch_VertexBufferObject        = false,
}

local is_B41 = getCore():getGameVersion():getMajor() == 41

for name, is_common in pairs(patches) do
    local fullName = "me.zed_0xff.zb_better_fps." .. name

    describe(fullName, function()
        if is_common or not is_B41 then
            it("should exist in B" .. getCore():getVersionNumber(), function()
                assert(Accessor.findClass(fullName))
            end)

            it("should find all fields", function()
                assert(Accessor.findClass(fullName):zbGet("ALL_FIELDS_FOUND"))
            end)
        else
            it("should not exist in B" .. getCore():getVersionNumber(), function()
                assert.is_nil(Accessor.findClass(fullName))
            end)
        end
    end)
end

return ZBSpec.run()

local version = getCore():getGameVersion()
local major = version:getMajor()
local minor = version:getMinor()
local versionStr = getCore():getVersionNumber() or ("%d.%d"):format(major, minor)

-- Flat list of all patch class names (any build)
local classes = {
    "Patch_DefaultShader_B42",
    "Patch_IndieGL",
    "Patch_IsoChunkMap",
    "Patch_IsoMovingObject_B41",
    "Patch_IsoMovingObject_B42_12",
    "Patch_IsoMovingObject_B42_13",
    "Patch_MainLoop_B41",
    "Patch_MainLoop_B42",
    "Patch_RingBuffer_IsStateChanged_B41",
    "Patch_RingBuffer_IsStateChanged_B42",
    "Patch_RingBuffer",
    "Patch_VertexBufferObject_B42",
}

-- Parse _B42_13 / _B42_12 / _B41 / _B42 → wantMajor, wantMinor (nil,nil = common)
local function versionFromSuffix(className)
    local a, b = className:match("_B(%d+)_(%d+)$")
    if a then return tonumber(a), tonumber(b) end

    a = className:match("_B(%d+)$")
    return a and tonumber(a) or nil, a and nil or nil
end

local function shouldExist(className)
    local wantMajor, wantMinor = versionFromSuffix(className)
    if not wantMajor then return true end
    return major == wantMajor and (not wantMinor or minor == wantMinor)
end

for _, className in ipairs(classes) do
    local fullName = "me.zed_0xff.zb_better_fps." .. className
    describe(fullName, function()
        if shouldExist(className) then
            it("should exist in " .. versionStr, function()
                assert(Accessor.findClass(fullName), "class not found: " .. fullName)
            end)
            it("should find all fields", function()
                assert(Accessor.findClass(fullName):zbGet("ALL_FIELDS_FOUND"),
                    "ALL_FIELDS_FOUND not true for " .. fullName)
            end)
        else
            it("should not exist in " .. versionStr, function()
                assert.is_nil(Accessor.findClass(fullName))
            end)
        end
    end)
end

return ZBSpec.run()

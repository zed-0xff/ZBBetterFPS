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

-- Declared minors per major (from class suffixes like _B42_12, _B42_13)
local declaredMinors = {}
for _, className in ipairs(classes) do
    local wantMajor, wantMinor = versionFromSuffix(className)
    if wantMajor and wantMinor then
        declaredMinors[wantMajor] = declaredMinors[wantMajor] or {}
        declaredMinors[wantMajor][wantMinor] = true
    end
end

-- Effective minor for current game: latest declared minor <= game minor, or latest declared if game is newer
local function effectiveMinorForMajor(wantMajor)
    local minors = declaredMinors[wantMajor]
    if not minors then return nil end
    local list = {}
    for m, _ in pairs(minors) do list[#list + 1] = m end
    if #list == 0 then return nil end
    table.sort(list)
    local maxDeclared = list[#list]
    if minor >= maxDeclared then return maxDeclared end
    local best = nil
    for _, m in ipairs(list) do
        if m <= minor and (not best or m > best) then best = m end
    end
    return best
end

local function shouldExist(className)
    local wantMajor, wantMinor = versionFromSuffix(className)
    if not wantMajor then return true end
    if major ~= wantMajor then return false end
    if not wantMinor then return true end -- e.g. _B42 with no minor = any 42.x
    return wantMinor == effectiveMinorForMajor(wantMajor)
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

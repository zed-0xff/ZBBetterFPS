if not ZBBetterFPS then return end

ZBBetterFPS.PKG_NAME = "me.zed_0xff.zb_better_fps"
ZBBetterFPS.PATCHES  = {
    "Patch_DefaultShader_B42",
    "Patch_IndieGL",
    "Patch_IsoChunkMap",
    "Patch_IsoMovingObject_B41",
    "Patch_IsoMovingObject_B42_12",
    "Patch_IsoMovingObject_B42_13",
    "Patch_MainLoop",
    "Patch_MultiTextureFBO2",
    "Patch_RingBuffer_IsStateChanged",
    "Patch_RingBuffer",
    "Patch_VertexBufferObject_B42",
}

function ZBBetterFPS:stats()
    local results = {}
    if Accessor and Accessor.findClass then
        for _, patch in ipairs(ZBBetterFPS.PATCHES) do
            local klass = Accessor.findClass(self.PKG_NAME .. "." .. patch)
            if klass then
                local st = {
                    ok   = klass:zbget("N_OK"),
                    fail = klass:zbget("N_FAIL"),
                    skip = klass:zbget("N_SKIP"),
                }
                if st.fail == 0 then st.fail = nil end
                if st.skip == 0 then st.skip = nil end
                results[patch] = st
            end
        end
    end
    return results
end

describe("me.zed_0xff.zb_better_fps.Utils", function()
    local klass = Accessor.findClass(subject)

    it("is defined on client", function()
        assert(klass)
    end)

    describe("isDebug", function()
        it("returns true", function()
            assert(klass:zbcall(subject))
        end)
    end)

    describe("isGameStarted", function()
        it("returns true", function()
            assert(klass:zbcall(subject))
        end)
    end)
end)

return ZBSpec.run()

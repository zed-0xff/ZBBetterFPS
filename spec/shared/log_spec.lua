describe("console.log", function()
  local exceptions = {
    "NoSuchFieldException",
    "NullPointerException",
    "ClassNotFoundException",
    "IllegalAccessException",
    "InstantiationException",
    "InvocationTargetException",
    "ArrayIndexOutOfBoundsException",
    "IndexOutOfBoundsException",
  }

  for _, exception in ipairs(exceptions) do
    it("should not have any " .. exception, function()
      assert.same({}, zbGrepLog(exception))
    end)
  end
end)

return ZBSpec.run()

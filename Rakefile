task :default => :build

VERSIONS = {
  "41"    => "17",
  "42.12" => "17",
  "42.13" => "24",
}

VERSIONS.each do |ver, jdk_ver|
  desc "build for #{ver}"
  task "build:#{ver}" do
    Dir.chdir("java") do
      env = {
        "JAVA_HOME" => "/Library/Java/JavaVirtualMachines/openjdk-#{jdk_ver}.jdk/Contents/Home"
      }
      sh env, "gradle build -PZVersion=#{ver}"
    end
    dst_dir = "#{ver}/media/java/client/libs"
    FileUtils.mkdir_p dst_dir
    FileUtils.mv "java/build/libs/ZBBetterFPS-#{ver}.jar", "#{dst_dir}/ZBBetterFPS.jar"
  end
end

desc "build all"
task :build => VERSIONS.keys.map { |ver| "build:#{ver}" }

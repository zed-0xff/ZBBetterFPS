task :default => :build

VERSIONS = %w'41 42.12 42.13'

VERSIONS.each do |ver|
desc "build for #{ver}"
  task "build:#{ver}" do
    Dir.chdir("#{ver}/media/java") do
      sh "gradle build"
    end
  end
end

desc "build all"
task :build => VERSIONS.map { |ver| "build:#{ver}" }

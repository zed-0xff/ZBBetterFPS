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

namespace :java do
  desc "symlink common files"
  task :symlink_common do
    require 'digest/md5'

    v0 = VERSIONS.first
    Dir["#{v0}/media/java/src/**/*.java"].each do |fname0|
      next if File.symlink?(fname0)

      rel_fname = fname0.sub("#{v0}/", '')
      next unless VERSIONS.all? { |ver| File.exist?("#{ver}/#{rel_fname}") }
      next unless VERSIONS.map  { |ver| Digest::MD5.file("#{ver}/#{rel_fname}").hexdigest }.uniq.size == 1

      FileUtils.cp(fname0, "common/media/java/src/")

      VERSIONS.each do |ver|
        FileUtils.rm("#{ver}/#{rel_fname}")
        Dir.chdir("#{ver}/media/java/src") do
          sh "ln -s ../../../../common/#{rel_fname}"
        end
      end
    end
  end
end

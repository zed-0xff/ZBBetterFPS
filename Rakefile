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
  desc "symlink common files when 2+ versions are identical (others stay as regular files)"
  task :symlink_common do
    require 'digest/md5'

    # rel_fname => [versions that have it and it's not a symlink]
    path_to_versions = Hash.new { |h, k| h[k] = [] }
    VERSIONS.each do |ver|
      Dir["#{ver}/media/java/src/**/*.java"].each do |fname|
        next if File.symlink?(fname)
        rel_fname = fname.sub("#{ver}/", '')
        path_to_versions[rel_fname] << ver
      end
    end

    path_to_versions.each do |rel_fname, versions|
      next if versions.size < 2
      digest_to_versions = Hash.new { |h, k| h[k] = [] }
      versions.each do |ver|
        dig = Digest::MD5.file("#{ver}/#{rel_fname}").hexdigest
        digest_to_versions[dig] << ver
      end
      group = digest_to_versions.values.select { |v| v.size >= 2 }.max_by(&:size)
      next unless group

      common_dest = "common/#{rel_fname}"
      FileUtils.mkdir_p(File.dirname(common_dest))
      FileUtils.cp("#{group.first}/#{rel_fname}", common_dest)

      group.each do |ver|
        link_path = "#{ver}/#{rel_fname}"
        link_dir = File.dirname(link_path)
        depth = link_dir.split("/").size
        target = ([".."] * depth).join("/") + "/" + common_dest
        FileUtils.rm(link_path)
        Dir.chdir(link_dir) do
          sh "ln -s #{target} #{File.basename(rel_fname)}"
        end
      end
    end
  end
end

require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|

  s.name                = package['name']
  s.version             = package['version']
  #s.name           = "RNVlcPlayer"
  #s.version        = '1.0.0'
  s.summary        = "VLCPlayer supports various formats (mp4, m3u8, flv, mov, rtsp, rtmp, etc.)."
  s.homepage       = "https://github.com/rich1111/react-native-vlcplayer"
  s.license        = "MIT"
  s.author         = { "Rich111 Chen" => "rich111.chen@gmail.com" }
  s.platforms      = { :ios => "9.0", :tvos => "9.2" }
  s.source         = { :git => "https://github.com/rich1111/react-native-vlcplayer.git", :tag => "v#{s.version}" }
  #s.source_files   = 'ios/RCTVLCPlayer/**/*.{h,m}'
  s.source_files        = 'ios/**/*.{h,m}'
  s.libraries           = 'iconv.2', 'c++.1', 'z.1', 'bz2.1.0'
  s.framework           = 'AudioToolbox','VideoToolbox', 'CoreMedia', 'CoreVideo', 'CoreAudio', 'AVFoundation', 'MediaPlayer'

  #s.resources      = "Fonts/*.ttf"
  s.preserve_paths = "**/*.js"
  s.dependency          'React'
  s.dependency          'MobileVLCKit','~> 3.3'

end

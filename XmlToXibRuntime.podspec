Pod::Spec.new do |s|
  s.name             = 'XmlToXibRuntime'
  s.version          = '1.0.4'
  s.summary          = 'A runtime used in conversion of Android resources to iOS resources.'

  s.description      = <<-DESC
A runtime used in conversion of Android resources to iOS resources.
Contains equivalents to Android drawables, checkboxes, radio buttons, text styling, and more.
                       DESC

  s.homepage         = 'https://github.com/lightningkite/android-layout-translator'
  # s.screenshots     = 'www.example.com/screenshots_1', 'www.example.com/screenshots_2'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'Joseph' => 'joseph@lightningkite.com' }
  s.source           = { :git => 'https://github.com/lightningkite/android-layout-translator.git', :tag => s.version.to_s }
  # s.social_media_url = 'https://twitter.com/<TWITTER_USERNAME>'
  s.swift_version    = "5.0"

  s.ios.deployment_target = '12.0'

  s.source_files = 'XmlToXibRuntime/XmlToXibRuntime/Classes/**/*'

  s.dependency 'M13Checkbox', '3.4.0-LK'
end

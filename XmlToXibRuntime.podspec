Pod::Spec.new do |s|
  s.name             = 'XmlToXibRuntime'
  s.version          = '0.1.0'
  s.summary          = 'A runtime used in conversion of Android resources to iOS resources.'

  s.description      = <<-DESC
A runtime used in conversion of Android resources to iOS resources.
Contains equivalents to Android drawables, checkboxes, radio buttons, text styling, and more.
                       DESC

  s.homepage         = 'https://github.com/UnknownJoe796/XmlToXibRuntime'
  # s.screenshots     = 'www.example.com/screenshots_1', 'www.example.com/screenshots_2'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'UnknownJoe796' => 'joseph@lightningkite.com' }
  s.source           = { :git => 'https://github.com/UnknownJoe796/XmlToXibRuntime.git', :tag => s.version.to_s }
  # s.social_media_url = 'https://twitter.com/<TWITTER_USERNAME>'

  s.ios.deployment_target = '11.0'

  s.source_files = 'XmlToXibRuntime/XmlToXibRuntime/Classes/**/*'

  s.dependency 'M13Checkbox'
end

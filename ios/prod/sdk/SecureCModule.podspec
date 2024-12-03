Pod::Spec.new do |spec|
    spec.name              = 'SecureCModule'
    spec.version           = '2.6.4'
    spec.summary           = 'Antelop iOS SecureCModule'
    spec.homepage          = 'https://www.entrust.com/'

    spec.author            = { 'Name' => 'Entrust' }
    spec.license           = 'Proprietary'

    spec.platform          = :ios

    spec.source = { path: '.' }

    spec.ios.deployment_target = '11.0'
    spec.ios.vendored_frameworks = 'SecureCModule.xcframework'
end

* FVEye Android Application

사용한 외부 라이브러리
    Kotlin
        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8-1.4.0-rc'
        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.8-1.4.0-rc'

    CameraX
        def camerax_version = "1.0.0-beta07"
        implementation "androidx.camera:camera-core:${camerax_version}"
        implementation "androidx.camera:camera-camera2:${camerax_version}"
        implementation "androidx.camera:camera-lifecycle:${camerax_version}"
        implementation "androidx.camera:camera-view:1.0.0-alpha14"

    Qr Scanner
        implementation 'com.google.mlkit:barcode-scanning:16.0.2'

    Socket.io
        implementation('io.socket:socket.io-client:1.0.0') {
            exclude group: 'org.json', module: 'json'
        }
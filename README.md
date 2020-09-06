* FVEye Android Application

##사용한 외부 라이브러리

    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8-1.4.0-rc'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.8-1.4.0-rc'

    implementation 'com.google.mlkit:barcode-scanning:16.0.3'
    implementation 'com.google.mlkit:face-detection:16.0.2'

    def camerax_version = "1.0.0-beta08"
    implementation "androidx.camera:camera-core:${camerax_version}"
    implementation "androidx.camera:camera-camera2:${camerax_version}"
    implementation "androidx.camera:camera-lifecycle:${camerax_version}"
    implementation "androidx.camera:camera-view:1.0.0-alpha15"

    implementation('io.socket:socket.io-client:1.0.0') {
        exclude group: 'org.json', module: 'json'
    }
    
    implementation 'com.quickbirdstudios:opencv:4.3.0'

    implementation ('org.tensorflow:tensorflow-lite:2.3.0')
    // This dependency adds the necessary TF op support.
    implementation ('org.tensorflow:tensorflow-lite-select-tf-ops:0.0.0-nightly'){ changing = true }
    implementation('org.tensorflow:tensorflow-lite-gpu:2.3.0')
    implementation('org.tensorflow:tensorflow-lite-support:0.0.0-nightly') { changing = true }

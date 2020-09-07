# FBEye Android Application

## What is FBEye?
 #### Find Bad Eye
 * 시선 추적을 통한 온라인 시험 부정행위 탐지 프로그램 입니다.
 * 추가적으로 서버와의 QR코드 대조를 통해서 실시간 접속 확인을 합니다.
 * 적외선 카메라 등의 특수한 장치 없이 스마트폰으로 시선 추적을 할 수 있습니다.
 * [인증서버](https://github.com/ddoo-ddah/fbeye-processing-server) 및 [관리자용 웹페이지](https://github.com/ddoo-ddah/fbeye-web-server), [수험자용 클라이언트](https://github.com/ddoo-ddah/fbeye-desktop_windows) 와 연동하여 사용할 수 있습니다.

## How it works?
#### Eye LandMark Detection
 1. 이 기능은 [Learning to Find Eye Region Landmarks for Remote Gaze Estimation in Unconstrained Settings](https://ait.ethz.ch/projects/2018/landmarks-gaze/) 을 인용하여 제작되었습니다. 상세 모델 구현은 논문을 참고하시기 바랍니다.
 2. 눈 영역의 그레이스케일 이미지를 입력하면, 해당하는 눈 이미지에서 Eyelid, Iris edge와 Iris center, Eyeball Center가 출력됩니다.
 3. 입력크기는 108\*180\*1 이며 출력크기는 36\*60\*18 입니다.
 4. HeatMap 형태로 출력되며, 각 영역에 해당하는 값중 최대치를 찾아 좌표로 변환하면 사용 할 수 있습니다.
 
 * 모델은 데스크탑에서 학습되었으며 충분히 학습 완료된 모델을 안드로이드에서 사용 할 수 있도록 변환하였습니다.
 * 변환된 모델은 [Tensorflow-lite](https://www.tensorflow.org/lite/) 라이브러리를 사용하여 동작시킬수 있습니다. 하지만 추가적인 학습은 불가능 합니다.
 * 학습에 사용된 데이터셋은 420만개 가량입니다.
 
#### Eye Gaze Estimation
 1. [Google Mlkit](https://developers.google.com/ml-kit/vision/face-detection) 라이브러리를 통해 얼굴 및 얼굴 특징점을 찾습니다.
 2. 얼굴 특징점 중 왼쪽 및 오른쪽 눈의 위치를 찾고, 이를 이미지로 추출합니다.
 3. 미리 제작한 딥러닝 모델을 통해 Eye Landmark를 얻어냅니다. 
 4. 3에서 얻은 Eye Landmark를 기반으로 3D 모델 기반 시선 추적을 통해 시선 방향 벡터를 구합니다.
 5. 시선 방향 벡터를 인증서버로 전송합니다.

#### Eye Position Tracking on Display
 1. 수험자 클라이언트와의 연동을 통해 스크린 특징점과 시선 방향을 맵핑합니다.
 2. 연동이 완료 된 이후부터는 동차 좌표계 변환을 통해 입력되는 시선 방향을 스크린 좌표로 변환하여 수험자가 보고 있는 좌표를 얻어 냅니다.
 
#### QR Code Scan
 1. [Google Mlkit](https://developers.google.com/ml-kit/vision/barcode-scanning) 라이브러리와 [CameraX](https://developer.android.com/training/camerax)의 ImageAnlysis를 사용해서
 클라이언트에서 보여주는 QR Code를 인식합니다.
 2. 같은 QR Code를 5초 이상 인식해야만 서버로 데이터를 전송합니다.
 3. 이때 화면의 흔들림이 감지되면 핸드폰을 사용한 것으로 간주 다시 5초 이상 인식해야 서버로 데이터를 전송합니다.
 
#### Processing Server Connection
 1. 보안을 위해 SSLSocket과 프로토콜 TLSv1.2를 사용해서 연결했습니다.
 2. 원활한 통신을 위해 [JSONOBJECT](https://developer.android.com/reference/org/json/JSONObject)을 사용해서 데이터를 주고 받습니다.

#### Image Server Connection
 1. [Socket.IO](https://socket.io/blog/native-socket-io-and-android/)를 사용해서 연결했습니다.
 2. 서버로부터 요청이 들어오면 2가지 과정을 거친 후 전면 카메라 이미지를 전송하기 시작합니다.
 3. 원본 이미지가 너무 크기 때문에 리사이징을 하는 과정과 base64로 인코딩을 하는 과정 2가지를 거칩니다.
 4. 이미지 전송은 stop명령이 오기 전까지 계속해서 보냅니다.
 
#### Pages
 1. 각각의 화면들은 wakelock을 이용해서 실행 중에 꺼지지 않습니다.
 2. 사용자의 편의성을 위해 처음 카메라 조정을 제외하면 특별한 조작이 필요없습니다.
 3. 시험 도중엔 UI가 사라집니다.

## Requirements
 * Recommended Device : Samsung Galaxy S10 series or latest
 * Android : At least Oreo (API 22+)
 * ABI : armeabi-v7a or arm64-v8a

 #### Used APIs
 
   * [kotlinx-coroutines](https://developer.android.com/kotlin/coroutines)
   * [Google MLkit barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning)
   * [Google MLkit Face detection](https://developers.google.com/ml-kit/vision/face-detection)
   * [Android CameraX](https://developer.android.com/training/camerax)
   * [Socket.io](https://github.com/socketio/socket.io-client-java)
   * [OpenCV](https://github.com/quickbirdstudios/opencv-android)
   * [Tensorflow-lite](https://www.tensorflow.org/lite/)
 

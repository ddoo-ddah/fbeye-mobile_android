# FBEye Android Application

## What is FBEye?
 #### Find Bad Eye
 * 시선 추적을 통한 온라인 시험 부정행위 탐지 프로그램 입니다.
 * 추가적으로 서버와의 QR코드 대조를 통해서 실시간 접속 확인을 합니다.
 * 적외선 카메라 등의 특수한 장치 없이 스마트폰으로 시선 추적을 할 수 있습니다.
 * [인증서버](https://github.com/ddoo-ddah/fbeye-processing-server) 및 [관리자용 웹페이지](https://github.com/ddoo-ddah/fbeye-web-server), [수험자용 클라이언트](https://github.com/ddoo-ddah/fbeye-desktop_windows) 와 연동하여 사용할 수 있습니다.

 * 시선추적 정확도의 평가는 아직 완료되지 않았습니다.
 
## How it works?
#### Eye LandMark Detection
 1. 이 기능은 [Learning to Find Eye Region Landmarks for Remote Gaze Estimation in Unconstrained Settings](https://ait.ethz.ch/projects/2018/landmarks-gaze/) 을 인용하여 제작되었습니다. 상세 모델 구현은 논문을 참고하시기 바랍니다.
 2. 눈 영역의 그레이스케일 이미지를 입력하면, 해당하는 눈 이미지에서 Eyelid, Iris edge와 Iris center, Eyeball Center가 출력됩니다.
 3. 입력크기는 108\*180\*1 이며 출력크기는 36\*60\*18 입니다.
 4. HeatMap 형태로 출력되며, 각 영역에 해당하는 값중 최대치를 찾아 좌표로 변환하면 사용 할 수 있습니다.
 
 * 모델은 데스크탑에서 학습되었으며 충분히 학습된 모델을 안드로이드에서 사용 할 수 있도록 변환하였습니다.
 * 변환된 모델은 추가적인 학습이 불가능하며, [Tensorflow-lite](https://www.tensorflow.org/lite/) 라이브러리를 사용하여 동작시킬수 있습니다.
 * 학습에 사용된 데이터셋은 420만개 가량입니다.

#### Face Detection
 1. [CameraX](https://developer.android.com/training/camerax) 라이브러리와 [Google Mlkit](https://developers.google.com/ml-kit/vision/face-detection) 라이브러리를 통해 얼굴 및 얼굴의 특징점을 찾습니다.
 2. Face Detection에 입력되는 이미지의 크기는 640 x 480이어야 하며, 출력되는 이미지 또한 640 x 480 입니다. 이는 원본이미지의 크기 및 비율과는 독립적입니다.
    * CameraX 라이브러리를 사용하면 별다른 설정을 할 필요가 없습니다. 
 3. 얼굴이 20프레임 이상 검출되지 않는 경우 자리이탈로 판단하여 서버에 로그를 전송합니다.
 4. 서버에서 이미지 요청이 오는경우 얼굴이 검출되지 않는 경우에도 전면 카메라 영상을 전송합니다.

#### Eye Gaze Estimation
 1. 위의 Face Detection으로 얻어낸 얼굴 특징점을 사용합니다.
 2. 얼굴 특징점 중 왼쪽 및 오른쪽 눈의 위치를 찾고, 이를 이미지로 추출합니다.
 3. [Tensorflow-lite](https://www.tensorflow.org/lite/) 라이브러리로 Eye LandMark Detection 모델을 작동시켜 2의 이미지에서 Eye Landmark를 얻어냅니다. 
 4. 3에서 얻은 Eye Landmark를 기반으로 시선 방향 값을 구합니다.
 5. 시선 방향 값을 인증서버로 전송합니다.
 6. 서버에서 이미지 전송요청이 오는경우, 눈동자의 테두리와 시선방향이 그려진 이미지를 생성하여 서버로 전송합니다.
 
 * 검출 속도 향상을 위해 GPU 가속을 사용합니다.
 * 기기에 따라 GPU 가속을 사용할 수 없는 경우도 있습니다. 이 경우 관련 옵션을 제거해 주시기 바랍니다.
 
#### QR Code Scan
 1. [Google Mlkit](https://developers.google.com/ml-kit/vision/barcode-scanning) 및 [CameraX](https://developer.android.com/training/camerax) 라이브러리를 사용하여
 PC 클라이언트에서 보여주는 QR Code를 인식합니다.
 2. QR코드를 첫번째로 인식한 경우 5초간 대기 한 뒤에 서버로 전송합니다. 
 3. 2의 대기시간 중 휴대폰의 흔들림이 감지되면 핸드폰을 사용한 것으로 간주하여 다시 5초간 대기합니다.
 4. QR Code가 인증서버에서 확인된 경우에만 안드로이드 어플리케이션 및 PC 클라이언트를 정상적으로 사용 할 수 있습니다.
    * 사실상의 로그인 기능입니다.
 5. 이후 시험을 응시하는 동안에도 주기적으로 변경되는 QR Code를 인식하여 서버로 전송합니다.
 * QR Code에서 일부 데이터를 추출하여 이미지 서버와의 연결을 성립시킬때도 사용합니다.
 
#### Processing Server Connection
 1. 보안을 위해 SSLSocket과 프로토콜 TLSv1.2를 사용해서 연결했습니다.
 2. 원활한 통신을 위해 [JSONOBJECT](https://developer.android.com/reference/org/json/JSONObject)을 사용해서 데이터를 주고 받습니다.

#### Image Server Connection
 1. [Socket.IO](https://socket.io/blog/native-socket-io-and-android/)를 사용해서 연결했습니다.
 2. 이미지의 생성과 전송의 비용이 크기 때문에 서버로부터 요청이 들어오는 경우에만 이미지를 전송합니다.
 3. 이미지 서버에서 사용하기 편하도록 적절하게 리사이징후 base64 이미지로 변환합니다.
 4. stop명령이 오기 전까지 매 프레임마다 변환된 이미지를 계속해서 보냅니다.
 
#### Pages
 1. 각각의 화면들은 [wakelock](https://developer.android.com/training/scheduling/wakelock)을 이용해서 실행 중에 꺼지지 않습니다.
 2. 사용자의 편의성을 위해 처음 카메라 조정을 제외하면 특별한 조작이 필요없습니다.
 3. 시험 도중엔 UI가 사라집니다.

## Requirements
 * Recommended Device : Samsung Galaxy S10 series or later
 * Android : Oreo or later (API 22+)
 * ABI : armeabi-v7a or arm64-v8a

 #### Dependencies
 
   * [kotlinx-coroutines](https://developer.android.com/kotlin/coroutines)
   * [Google MLkit barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning)
   * [Google MLkit Face detection](https://developers.google.com/ml-kit/vision/face-detection)
   * [Android CameraX](https://developer.android.com/training/camerax)
   * [Socket.io](https://github.com/socketio/socket.io-client-java)
   * [OpenCV](https://github.com/quickbirdstudios/opencv-android)
   * [Tensorflow-lite](https://www.tensorflow.org/lite/)

### Reference
> @inproceedings{Park2018ETRA,
	author = {Park, Seonwook and Zhang, Xucong and Bulling, Andreas and Hilliges, Otmar},
	title = {Learning to Find Eye Region Landmarks for Remote Gaze Estimation in Unconstrained Settings},
	booktitle = {ACM Symposium on Eye Tracking Research and Applications (ETRA)},
	series = {ETRA '18},
	year = {2018},
	location = {Warsaw, Poland},
	publisher = {ACM},
	address = {New York, NY, USA},
}

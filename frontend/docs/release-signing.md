# 안드로이드 릴리즈 서명 설정

지금은 `key.properties`가 없으면 release 빌드도 debug 키로 서명되도록 되어 있다(`flutter run --release`가 바로 되게 하기 위한 임시 상태). 실제 배포 전에 아래를 한 번만 하면 된다.

## 1. 키스토어 생성 (한 번만, 팀 공용으로 안전하게 보관)

```bash
keytool -genkey -v -keystore coltrip-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias coltrip-release
```

비밀번호 입력하라고 뜨면 정하고, **이 `.jks` 파일과 비밀번호는 절대 잃어버리면 안 됨** — 스토어에 한 번 배포한 뒤 이 키를 잃어버리면 같은 앱으로 업데이트를 올릴 방법이 없다. 팀 내 안전한 곳(개인 볼트, 회사 비밀번호 관리자 등)에 백업.

## 2. `android/key.properties` 작성 (git에 올리지 않음, 이미 .gitignore 처리됨)

`android/key.properties.example` 참고해서 같은 위치에 `key.properties`로 복사 후 실제 값 채우기:

```
storePassword=위에서 정한 키스토어 비밀번호
keyPassword=위에서 정한 키 비밀번호
keyAlias=coltrip-release
storeFile=/절대/경로/coltrip-release.jks
```

## 3. Google OAuth 안드로이드 클라이언트에 release SHA-1 등록

```bash
keytool -list -v -keystore coltrip-release.jks -alias coltrip-release
```
출력의 `SHA1:` 값을 Google Cloud Console → OAuth 클라이언트(`coltrip-android`)에 추가 등록. 이거 안 하면 release 빌드에서 구글 로그인이 막힌다(debug SHA-1만 등록돼 있던 것과 별개).

## 4. 빌드 확인

```bash
flutter build apk --release
```
`key.properties`가 있으면 자동으로 release 키로 서명됨. `build.gradle.kts`가 파일 존재 여부로 자동 분기하므로 이후엔 별도 설정 없이 그대로 빌드하면 된다.

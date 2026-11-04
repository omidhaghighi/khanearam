name: ساخت APK خانه آرام
# این تنها فایل ورک‌فلوی پروژه است.
# اجرای دستی (Run workflow) دو حالت دارد:
#   build    = ساخت APK
#   make-key = ساخت کلید امضا (فقط یک بار در عمر پروژه)
on:
  workflow_dispatch:
    inputs:
      action:
        description: 'build = ساخت APK  |  make-key = ساخت کلید امضا (فقط یک بار)'
        type: choice
        default: build
        options:
          - build
          - make-key
  push:
    branches: [main]

jobs:
  key:
    if: ${{ github.event_name == 'workflow_dispatch' && inputs.action == 'make-key' }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - name: ساخت کلید
        run: |
          PASS=$(openssl rand -hex 16)
          keytool -genkeypair -keystore release.jks -alias khane -keyalg RSA -keysize 2048 -validity 10000 -storepass "$PASS" -keypass "$PASS" -dname "CN=Khane Aram, O=Khane Aram, C=IR"
          {
            echo "KEYSTORE_PASSWORD=$PASS"
            echo "KEY_ALIAS=khane"
            echo "KEY_PASSWORD=$PASS"
            echo "KEYSTORE_BASE64=$(base64 -w0 release.jks)"
          } > secrets.txt
      - uses: actions/upload-artifact@v4
        with:
          name: secrets-delete-after-use
          path: secrets.txt
          retention-days: 1

  build:
    if: ${{ !(github.event_name == 'workflow_dispatch' && inputs.action == 'make-key') }}
    runs-on: ubuntu-latest
    steps:
      - name: بررسی Secretها
        id: chk
        env:
          EVENT: ${{ github.event_name }}
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          SITE_URL: ${{ secrets.SITE_URL }}
          APP_KEY: ${{ secrets.APP_KEY }}
        run: |
          missing=""
          for n in KEYSTORE_BASE64 KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD SITE_URL APP_KEY; do
            if [ -z "${!n}" ]; then missing="$missing $n"; fi
          done
          if [ -n "$missing" ]; then
            if [ "$EVENT" = "push" ]; then
              echo "::warning::این Secretها هنوز ساخته نشده‌اند و ساخت APK رد شد:$missing"
              echo "ready=false" >> "$GITHUB_OUTPUT"
              exit 0
            fi
            echo "::error::این Secretها ساخته نشده‌اند:$missing"
            exit 1
          fi
          echo "ready=true" >> "$GITHUB_OUTPUT"
      - uses: actions/checkout@v4
        if: steps.chk.outputs.ready == 'true'
      - uses: actions/setup-java@v4
        if: steps.chk.outputs.ready == 'true'
        with:
          distribution: temurin
          java-version: '17'
      - name: تأیید لایسنس‌های Android SDK (SDK خود ران‌ر آماده است)
        if: steps.chk.outputs.ready == 'true'
        run: yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null || true
      - uses: gradle/actions/setup-gradle@v4
        if: steps.chk.outputs.ready == 'true'
        with:
          gradle-version: '8.9'
      - name: فایل فایربیس (برای اعلان گوشی)
        if: steps.chk.outputs.ready == 'true'
        env:
          GSJ: ${{ secrets.GOOGLE_SERVICES_JSON }}
        run: |
          if [ -n "$GSJ" ]; then
            echo "$GSJ" > app/google-services.json
          else
            cat > app/google-services.json <<'JSON'
          {"project_info":{"project_number":"000000000000","project_id":"dummy-project","storage_bucket":"dummy-project.appspot.com"},"client":[{"client_info":{"mobilesdk_app_id":"1:000000000000:android:0000000000000000","android_client_info":{"package_name":"com.khanearam.shop"}},"oauth_client":[],"api_key":[{"current_key":"dummy"}],"services":{"appinvite_service":{"other_platform_oauth_client":[]}}}],"configuration_version":"1"}
          JSON
          fi
      - name: آماده‌سازی کلید امضا
        if: steps.chk.outputs.ready == 'true'
        env:
          KEYSTORE_B64: ${{ secrets.KEYSTORE_BASE64 }}
        run: echo "$KEYSTORE_B64" | base64 -d > "$RUNNER_TEMP/release.jks"
      - name: ساخت APK
        if: steps.chk.outputs.ready == 'true'
        env:
          SITE_URL: ${{ secrets.SITE_URL }}
          APP_KEY: ${{ secrets.APP_KEY }}
          KEYSTORE_FILE: ${{ runner.temp }}/release.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          gradle assembleRelease --no-daemon
          cp app/build/outputs/apk/release/*.apk khane-aram.apk
      - uses: actions/upload-artifact@v4
        if: steps.chk.outputs.ready == 'true'
        with:
          name: khane-aram-apk
          path: khane-aram.apk

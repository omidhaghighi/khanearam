name: ساخت فایل APK
on:
  workflow_dispatch:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: android-actions/setup-android@v3
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.9'
      - name: فایل فایربیس (برای اعلان گوشی)
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
        env:
          KEYSTORE_B64: ${{ secrets.KEYSTORE_BASE64 }}
        run: echo "$KEYSTORE_B64" | base64 -d > "$RUNNER_TEMP/release.jks"
      - name: ساخت APK
        env:
          SITE_URL: ${{ secrets.SITE_URL }}
          APP_KEY: ${{ secrets.APP_KEY }}
          KEYSTORE_FILE: ${{ runner.temp }}/release.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: gradle assembleRelease --no-daemon
      - uses: actions/upload-artifact@v4
        with:
          name: khane-aram-apk
          path: app/build/outputs/apk/release/*.apk

# Cloudy - AI Assistant for Android

**كلودي** - A personal AI assistant floating over your Android 15 device.

## 🎯 Features

✅ **Floating Cloud Interface** - ☁️ Tap to expand, drag to move
✅ **Arabic-First Design** - Full Arabic UI and conversations
✅ **Voice Recognition** - "تحدث بالعربية" (Speak in Arabic)
✅ **Text-to-Speech** - Hear responses read aloud
✅ **Chat History** - Persistent message storage
✅ **Foreground Service** - Runs in background with notification
✅ **Smart Responses** - Context-aware AI responses
✅ **Full App & Overlay Modes** - Use in main app or floating widget

## 🏗️ Project Structure

```
app/src/main/
├── java/com/cloudy/ai/
│   ├── MainActivity.kt              # Main chat activity
│   ├── FloatingCloudService.kt      # Floating overlay service
│   ├── AIService.kt                 # AI response generation
│   ├── ChatAdapter.kt               # RecyclerView adapter
│   └── ChatRepository.kt            # Message persistence
├── res/
│   ├── layout/
│   │   ├── activity_main.xml        # Main chat UI
│   │   ├── floating_cloud.xml       # Overlay layout
│   │   └── message_item.xml         # Chat message item
│   ├── drawable/
│   │   └── cloud_*.xml              # Cloud UI shapes
│   └── values/
│       ├── strings.xml              # Arabic strings
│       ├── colors.xml               # Color palette
│       └── themes.xml               # App theme
└── AndroidManifest.xml
```

## 📋 Permissions

- `INTERNET` - API communication
- `RECORD_AUDIO` - Voice recognition
- `SYSTEM_ALERT_WINDOW` - Floating overlay

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest)
- JDK 17+
- Android SDK 35

### Setup

1. **Clone/Open the repository**
   ```bash
   cd lawykhfajy56-rgb/-ai
   ```

2. **Sync Gradle**
   - Open in Android Studio
   - File → Sync Now

3. **Run the App**
   - Select an emulator or device
   - Click Run (Shift+F10)
   - Allow microphone & overlay permissions

## 💬 Usage

### Main App
- Type or speak Arabic messages in the input field
- Press "إرسال" (Send) to submit
- Click "🎙️" (Voice) to speak aloud
- Click "☁️ تفعيل" (Activate Cloud) to launch floating widget
- View chat history in RecyclerView

### Floating Cloud
- **Tap** the cloud ☁️ to expand/collapse
- **Drag** to reposition on screen
- Type messages or use voice
- Responses are read aloud automatically
- Tap "إغلاق" (Close) to dismiss

## 🤖 AI Responses

The current implementation includes smart pattern matching:

- "السلام عليكم" → Greeting response
- "ما اسمك؟" → Introduction
- "الوقت" → Current time
- "كيف..." → Help message
- "شكرا" → Appreciation
- Any other input → Contextual response

## 🔌 API Integration (Future)

To connect to a real AI backend:

1. Replace `simulateAIResponse()` in `AIService.kt`
2. Add HTTP client (Retrofit/OkHttp)
3. Integrate OpenAI/Gemini API
4. Add API key management

### Example integration point:
```kotlin
private suspend fun callAIAPI(userMessage: String): String {
    // Replace with your API call
    val response = apiClient.chat(userMessage)
    return response.text
}
```

## 🎨 Customization

### Change Colors
Edit `app/src/main/res/values/colors.xml`

### Change App Name
Edit `app/src/main/res/values/strings.xml`

### Change Cloud Size
Edit `floating_cloud.xml` layout dimensions

## 📦 Build & Release

### Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## ⚙️ Build Configuration

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Language**: Kotlin
- **Build Tool**: Gradle (Kotlin DSL)

## 🔐 Permissions Handling

The app requests permissions dynamically:
- Microphone: On first voice input
- Overlay: Before activating floating cloud
- All requests use modern Activity Result APIs

## 🐛 Troubleshooting

**"Permission denied" error**
- Go to Settings → Apps → Cloudy → Permissions
- Enable Microphone and Draw over other apps

**Overlay not showing**
- Settings → Display → Special app access → Draw over other apps
- Toggle Cloudy ON

**Voice recognition not working**
- Check microphone permission
- Ensure language is set to Arabic (ar-SA)
- Test with device microphone

## 📄 License

MIT - Feel free to use and modify.

## 👤 Author

Created for Android 15 as a personal AI assistant with floating UI.

---

**Next Steps:**
1. Test on Android 15 device/emulator
2. Connect to real AI API (OpenAI/Gemini)
3. Add features: wake words, persistent login, rich media
4. Sign and release on Google Play Store

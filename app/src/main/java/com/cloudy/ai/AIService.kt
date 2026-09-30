package com.cloudy.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

class AIService(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    fun generateResponse(
        userMessage: String,
        onResponse: (String) -> Unit
    ) {
        scope.launch {
            val response = withContext(Dispatchers.Default) {
                simulateAIResponse(userMessage)
            }
            onResponse(response)
        }
    }

    private fun simulateAIResponse(userMessage: String): String {
        return when {
            userMessage.contains("السلام") || userMessage.contains("مرحبا") ->
                "وعليكم السلام ورحمة الله وبركاته! أنا كلودي، مساعدك الذكي. كيف أستطيع مساعدتك اليوم؟"
            userMessage.contains("ما اسمك") ->
                "أنا كلودي، مساعد ذكاء اصطناعي شخصي لأندرويد 15. أظهر كسحابة عائمة فوق تطبيقاتك."
            userMessage.contains("الوقت") ->
                "الوقت الآن ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(System.currentTimeMillis())}"
            userMessage.contains("كيف") ->
                "أنا هنا لمساعدتك! يمكنك التحدث معي بالعربية أو كتابة طلباتك. جرب قول 'اقرأ الساعة' أو 'من أنت'."
            userMessage.contains("شكرا") || userMessage.contains("شكراً") ->
                "على الرحب والسعة! أنا دائماً هنا للمساعدة."
            else ->
                "فهمت: '$userMessage'. هذا مثير للاهتمام! هل هناك شيء محدد تريد مني أن أفعله؟"
        }
    }

    fun destroy() {
        scope.cancel()
    }
}

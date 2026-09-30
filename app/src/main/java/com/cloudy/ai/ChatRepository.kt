package com.cloudy.ai

import android.content.Context

data class Message(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatRepository(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("cloudy_chat", Context.MODE_PRIVATE)
    private val messageList = mutableListOf<Message>()

    fun addMessage(message: Message) {
        messageList.add(message)
    }

    fun getMessages(): List<Message> = messageList.toList()

    fun clearMessages() {
        messageList.clear()
    }

    fun saveMessages() {
        val json = messageList.joinToString("|") { "${it.text}|${it.isFromUser}" }
        sharedPrefs.edit().putString("messages", json).apply()
    }

    fun loadMessages() {
        val json = sharedPrefs.getString("messages", "") ?: ""
        messageList.clear()
        json.split("|").chunked(2).forEach {
            if (it.size == 2) {
                messageList.add(Message(text = it[0], isFromUser = it[1].toBoolean()))
            }
        }
    }
}

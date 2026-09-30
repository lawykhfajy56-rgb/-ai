package com.cloudy.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cloudy.ai.databinding.MessageItemBinding

class ChatAdapter : RecyclerView.Adapter<ChatViewHolder>() {
    private val messages = mutableListOf<Message>()

    fun submitList(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = MessageItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size
}

class ChatViewHolder(private val binding: MessageItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        with(binding) {
            messageText.text = message.text
            if (message.isFromUser) {
                messageText.setBackgroundColor(0xFF2563EB.toInt())
                messageText.setTextColor(0xFFFFFFFF.toInt())
                root.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            } else {
                messageText.setBackgroundColor(0xFFE2E8F0.toInt())
                messageText.setTextColor(0xFF0F172A.toInt())
                root.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
            }
        }
    }
}

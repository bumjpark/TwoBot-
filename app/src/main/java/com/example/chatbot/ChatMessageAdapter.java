package com.example.chatbot;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    private List<ChatMessage> messageList;

    public ChatMessageAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        holder.messageTextView.setText(message.getMessage());

        // 발신자에 따라 정렬과 배경 변경
        if (message.getSender().equalsIgnoreCase("User")) {
            // 사용자 메시지: 오른쪽 정렬, 사용자 배경
            ((LinearLayout) holder.messageContainer).setGravity(Gravity.END);
            holder.messageTextView.setBackgroundResource(R.drawable.user_message_background);
        } else {
            // 챗봇 메시지: 왼쪽 정렬, 챗봇 배경
            ((LinearLayout) holder.messageContainer).setGravity(Gravity.START);
            holder.messageTextView.setBackgroundResource(R.drawable.bot_message_background);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageContainer;
        TextView messageTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}

package com.example.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ResponseAdapter extends RecyclerView.Adapter<ResponseAdapter.ResponseViewHolder> {

    private List<ResponseItem> responseList;

    public ResponseAdapter(List<ResponseItem> responseList) {
        this.responseList = responseList;
    }

    @NonNull
    @Override
    public ResponseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ResponseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResponseViewHolder holder, int position) {
        ResponseItem item = responseList.get(position);
        holder.botName.setText(item.getBotName());
        holder.responseText.setText(item.getResponseText());
    }

    @Override
    public int getItemCount() {
        return responseList.size();
    }

    public static class ResponseViewHolder extends RecyclerView.ViewHolder {
        TextView botName, responseText;

        public ResponseViewHolder(@NonNull View itemView) {
            super(itemView);
            botName = itemView.findViewById(android.R.id.text1);
            responseText = itemView.findViewById(android.R.id.text2);
        }
    }
}

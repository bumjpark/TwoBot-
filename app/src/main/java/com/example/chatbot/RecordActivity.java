package com.example.chatbot;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecordActivity extends AppCompatActivity {

    private RecyclerView recordRecyclerView;
    private ChatMessageAdapter recordAdapter;
    private List<ChatMessage> chatHistory;
    private ImageButton closeButton;
    private ImageButton deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record); // 위에서 수정한 레이아웃

        recordRecyclerView = findViewById(R.id.recordRecyclerView);
        closeButton = findViewById(R.id.closeButton);
        deleteButton = findViewById(R.id.deleteButton); // 하단 오른쪽 끝에 있는 삭제 버튼

        // Intent에서 대화 기록 JSON을 받아 List<ChatMessage>로 변환
        String chatHistoryJson = getIntent().getStringExtra("chat_history_json");
        chatHistory = ChatHistoryUtils.fromJson(chatHistoryJson);

        recordRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new ChatMessageAdapter(chatHistory);
        recordRecyclerView.setAdapter(recordAdapter);

        // 닫기 버튼 클릭 시 Activity 종료
        closeButton.setOnClickListener(v -> finish());

        // 삭제 버튼 클릭 이벤트
        deleteButton.setOnClickListener(v -> {
            // 대화 기록 삭제
            chatHistory.clear();
            recordAdapter.notifyDataSetChanged();

            // 필요하다면 SharedPreferences에서도 삭제
            // getSharedPreferences("chat_pref", MODE_PRIVATE).edit().putString("chat_history_json", "[]").apply();

            Toast.makeText(this, "대화 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
        });
    }
}

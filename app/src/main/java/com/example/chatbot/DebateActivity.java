package com.example.chatbot;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DebateActivity extends AppCompatActivity {

    private RecyclerView debateRecyclerView;
    private ChatMessageAdapter adapter;
    private List<ChatMessage> debateHistory;
    private Button startDebateButton, stopDebateButton;
    private EditText debateTopicInput;
    private Handler handler;
    private boolean isDebating;
    private int debateCount;
    private String debateTopic;
    private RequestQueue requestQueue;
    private static final String SERVER_URL = "http://10.0.2.2:5000/debate"; // Flask 서버 주소
    private DrawerLayout drawerLayout;

    // menuButton 멤버 변수로 유지
    private ImageButton menuButton;
    private NavigationView navigationView;
    private androidx.appcompat.widget.Toolbar topAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debate);

        // View 초기화
        debateRecyclerView = findViewById(R.id.debateRecyclerView);
        startDebateButton = findViewById(R.id.startDebateButton);
        stopDebateButton = findViewById(R.id.stopDebateButton);
        debateTopicInput = findViewById(R.id.debateTopic);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        // menuButton 초기화 (중복 선언 없이)
        menuButton = findViewById(R.id.menuButton);
        topAppBar = findViewById(R.id.topAppBar);

        // Drawer 열기 이벤트: menuButton을 이용하거나 topAppBar 이용
        // 여기서는 topAppBar의 navigationIcon을 사용해 드로어를 엽니다.
        // navigationIcon이 동작하도록 리스너 추가
        topAppBar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // 만약 menuButton도 유지하고 싶다면 아래 코드 사용 (topAppBar navIcon 제거 필요)
        // menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // RecyclerView 설정
        debateHistory = new ArrayList<>();
        adapter = new ChatMessageAdapter(debateHistory);
        debateRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        debateRecyclerView.setAdapter(adapter);

        handler = new Handler();

        // RequestQueue 초기화
        requestQueue = Volley.newRequestQueue(this);

        // 시작 버튼 이벤트
        startDebateButton.setOnClickListener(v -> startDebate());

        // 중지 버튼 이벤트
        stopDebateButton.setOnClickListener(v -> stopDebate());

        // 메뉴 항목 선택 이벤트 처리
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_chatbots) {
                // MainActivity로 이동
                Intent intent = new Intent(DebateActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_debate) {
                // 현재 화면 유지
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            return false;
        });
    }

    private void startDebate() {
        debateTopic = debateTopicInput.getText().toString().trim();
        if (debateTopic.isEmpty()) {
            Toast.makeText(this, "주제를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        isDebating = true;
        debateCount = 0;
        debateHistory.clear();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "토론을 시작합니다: " + debateTopic, Toast.LENGTH_SHORT).show();

        // ChatGPT부터 시작
        simulateDebate(new JSONArray(), true); // true이면 ChatGPT부터
    }

    private void stopDebate() {
        isDebating = false;
        Toast.makeText(this, "토론을 중지합니다.", Toast.LENGTH_SHORT).show();
    }

    private void simulateDebate(JSONArray history, boolean isChatGptFirst) {
        if (!isDebating || debateCount >= 10) {  // 총 10번 진행
            Toast.makeText(this, "토론이 종료되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("topic", debateTopic);
            requestBody.put("history", history);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    SERVER_URL,
                    requestBody,
                    response -> {
                        try {
                            String chatGptResponse = response.getString("chatgpt_response");
                            String geminiResponse = response.getString("gemini_response");
                            JSONArray updatedHistory = response.getJSONArray("history");

                            if (isChatGptFirst) {
                                // ChatGPT 응답 추가
                                debateHistory.add(new ChatMessage("ChatGPT", "ChatGPT: " + chatGptResponse));
                                adapter.notifyDataSetChanged();
                                debateRecyclerView.scrollToPosition(debateHistory.size() - 1);

                                // 2초 후 Gemini 응답 추가
                                handler.postDelayed(() -> {
                                    debateHistory.add(new ChatMessage("Gemini", "Gemini: " + geminiResponse));
                                    adapter.notifyDataSetChanged();
                                    debateRecyclerView.scrollToPosition(debateHistory.size() - 1);

                                    debateCount++;
                                    simulateDebate(updatedHistory, false);
                                }, 2000);
                            } else {
                                // Gemini 응답 추가
                                debateHistory.add(new ChatMessage("Gemini", "Gemini: " + geminiResponse));
                                adapter.notifyDataSetChanged();
                                debateRecyclerView.scrollToPosition(debateHistory.size() - 1);

                                // 2초 후 ChatGPT 응답 추가
                                handler.postDelayed(() -> {
                                    debateHistory.add(new ChatMessage("ChatGPT", "ChatGPT: " + chatGptResponse));

                                    adapter.notifyDataSetChanged();
                                    debateRecyclerView.scrollToPosition(debateHistory.size() - 1);

                                    debateCount++;
                                    simulateDebate(updatedHistory, true);
                                }, 2000);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "응답 처리 중 오류 발생", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        Toast.makeText(this, "서버 요청 실패: " + error.toString(), Toast.LENGTH_SHORT).show();
                        error.printStackTrace();
                    }
            );

            // 타임아웃 설정
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    60000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(jsonObjectRequest);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

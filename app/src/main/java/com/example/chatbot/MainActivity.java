package com.example.chatbot;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
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
import java.util.Locale;

import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private TextView geminiResponse, chatGptResponse;
    private ImageButton geminiExpandButton, chatGptExpandButton;
    private EditText inputField;
    private ImageView selectedImageView;
    private RequestQueue requestQueue;
    private View initialMessage;

    private static final int CAMERA_PERMISSION_CODE = 100;

    private SpeechRecognizerHelper speechRecognizerHelper;
    private PhotoPickerHelper photoPickerHelper;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private Dialog dialog;
    private List<ChatMessage> chatHistory;
    private RecyclerView chatRecyclerView;

    private List<ChatMessage> geminiChatHistory;
    private List<ChatMessage> chatGptChatHistory;
    private ChatMessageAdapter geminiAdapter;
    private ChatMessageAdapter chatGptAdapter;
    private RecyclerView geminiRecyclerView;
    private RecyclerView chatGptRecyclerView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private static final String PREF_NAME = "chat_pref";
    private static final String KEY_CHAT_HISTORY = "chat_history_json";

    private List<List<ChatMessage>> recordSessions;
    private int recordCount = 1; // 시작은 기록1번부터

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        // View 초기화
        ImageButton photoButton = findViewById(R.id.photoButton);
        selectedImageView = findViewById(R.id.selectedImageView);
        geminiExpandButton = findViewById(R.id.geminiExpandButton);
        chatGptExpandButton = findViewById(R.id.chatGptExpandButton);
        inputField = findViewById(R.id.inputField);
        initialMessage = findViewById(R.id.initialMessage);
        ImageButton sendButton = findViewById(R.id.sendButton);
        ImageButton microphoneButton = findViewById(R.id.microphoneButton);

        geminiChatHistory = new ArrayList<>();
        chatGptChatHistory = new ArrayList<>();
        chatHistory = new ArrayList<>();
        recordSessions = new ArrayList<>();

        loadChatHistory();

        geminiRecyclerView = findViewById(R.id.geminiRecyclerView);
        chatGptRecyclerView = findViewById(R.id.chatGptRecyclerView);

        geminiRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatGptRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        geminiAdapter = new ChatMessageAdapter(geminiChatHistory);
        chatGptAdapter = new ChatMessageAdapter(chatGptChatHistory);

        geminiRecyclerView.setAdapter(geminiAdapter);
        chatGptRecyclerView.setAdapter(chatGptAdapter);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        ImageButton menuButton = findViewById(R.id.menuButton);

        requestQueue = Volley.newRequestQueue(this);

        speechRecognizerHelper = new SpeechRecognizerHelper(this);

        ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        photoPickerHelper.handleCameraResult(result.getData());
                    }
                }
        );
        photoPickerHelper = new PhotoPickerHelper(this, cameraLauncher, selectedImageView, detectedText -> {
            inputField.setText(detectedText);
            sendChatRequest();
        });
        photoPickerHelper = new PhotoPickerHelper(this, cameraLauncher, selectedImageView, detectedText -> {
            inputField.setText(detectedText);
            sendChatRequest();
        });

        sendButton.setOnClickListener(v -> sendChatRequest());
        microphoneButton.setOnClickListener(v -> speechRecognizerHelper.startSpeechToText());
        photoButton.setOnClickListener(v -> photoPickerHelper.openCamera());

        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_chatbots) {
                Toast.makeText(this, "Two Bot 선택", Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_debate) {
                startActivity(new Intent(MainActivity.this, DebateActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_history) {
                // 현재 chatHistory를 기록 화면으로
                String chatHistoryJson = ChatHistoryUtils.toJson(chatHistory);
                Intent intent = new Intent(MainActivity.this, RecordActivity.class);
                intent.putExtra("chat_history_json", chatHistoryJson);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
            return false;
        });

        geminiExpandButton.setOnClickListener(v -> showFullResponseDialog("Gemini 응답", true));
        chatGptExpandButton.setOnClickListener(v -> showFullResponseDialog("ChatGPT 응답", false));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // main_menu.xml에서 새로운페이지 아이템이 있다고 가정
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // 새로운 페이지 버튼 클릭 시
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new_page) {
            createNewPage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createNewPage() {
        // 현재 페이지의 chatHistory를 기록 목록에 저장 (기록1)
        if (!chatHistory.isEmpty()) {
            List<ChatMessage> oldHistory = new ArrayList<>(chatHistory);
            recordSessions.add(oldHistory);
        }

        // 기록 번호 증가
        recordCount++;

        // 히스토리 초기화
        chatHistory.clear();
        geminiChatHistory.clear();
        chatGptChatHistory.clear();

        saveChatHistory();

        geminiAdapter.notifyDataSetChanged();
        chatGptAdapter.notifyDataSetChanged();
        initialMessage.setVisibility(View.VISIBLE);
        findViewById(R.id.geminiCard).setVisibility(View.GONE);
        findViewById(R.id.chatGptCard).setVisibility(View.GONE);

        Toast.makeText(this, "새로운 페이지를 생성했습니다! 현재 페이지는 기록 " + recordCount + "번 입니다.", Toast.LENGTH_SHORT).show();
    }

    private void loadChatHistory() {
        String json = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(KEY_CHAT_HISTORY, "[]");
        chatHistory = ChatHistoryUtils.fromJson(json);
    }

    private void saveChatHistory() {
        String json = ChatHistoryUtils.toJson(chatHistory);
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().putString(KEY_CHAT_HISTORY, json).apply();
    }

    private void sendChatRequest() {
        String prompt = inputField.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, "질문을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        geminiChatHistory.add(new ChatMessage("User", prompt));
        chatGptChatHistory.add(new ChatMessage("User", prompt));
        chatHistory.add(new ChatMessage("User", prompt));
        saveChatHistory();

        String url = "http://10.0.2.2:5000/chat";

        JSONArray historyArray = new JSONArray();
        try {
            for (ChatMessage message : chatGptChatHistory) {
                JSONObject messageJson = new JSONObject();
                String role = message.getSender().equalsIgnoreCase("User") ? "user" : "assistant";
                messageJson.put("role", role);
                messageJson.put("content", message.getMessage());
                historyArray.put(messageJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("prompt", prompt);
            requestBody.put("history", historyArray);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    try {
                        String chatGpt = response.getString("chatgpt_response");
                        String gemini = response.getString("gemini_response");

                        geminiChatHistory.add(new ChatMessage("Gemini", gemini));
                        chatGptChatHistory.add(new ChatMessage("ChatGPT", chatGpt));
                        chatHistory.add(new ChatMessage("Gemini", gemini));
                        chatHistory.add(new ChatMessage("ChatGPT", chatGpt));
                        saveChatHistory();

                        updateChatViews();

                        initialMessage.setVisibility(View.GONE);
                        findViewById(R.id.geminiCard).setVisibility(View.VISIBLE);
                        findViewById(R.id.chatGptCard).setVisibility(View.VISIBLE);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "응답 처리 중 오류 발생", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(MainActivity.this, "서버 연결 실패: " + error.toString(), Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }
        );

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(jsonObjectRequest);
        inputField.setText("");
    }

    private void updateChatViews() {
        geminiAdapter.notifyDataSetChanged();
        chatGptAdapter.notifyDataSetChanged();
        geminiRecyclerView.scrollToPosition(geminiChatHistory.size() - 1);
        chatGptRecyclerView.scrollToPosition(chatGptChatHistory.size() - 1);
    }

    private void showFullResponseDialog(String title, boolean isGemini) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_full_response);

        TextView titleTextView = dialog.findViewById(R.id.dialogTitle);
        RecyclerView dialogRecyclerView = dialog.findViewById(R.id.dialogRecyclerView);
        EditText dialogInputField = dialog.findViewById(R.id.dialogInputField);
        ImageButton closeButton = dialog.findViewById(R.id.closeButton);
        ImageButton sendButton = dialog.findViewById(R.id.dialogSendButton);
        ImageButton dialogMicrophoneButton = dialog.findViewById(R.id.dialogMicrophoneButton);
        ImageButton dialogPhotoButton = dialog.findViewById(R.id.dialogPhotoButton);

        titleTextView.setText(title);

        List<ChatMessage> currentHist = isGemini ? geminiChatHistory : chatGptChatHistory;
        ChatMessageAdapter adapter = new ChatMessageAdapter(currentHist);
        dialogRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dialogRecyclerView.setAdapter(adapter);

        adapter.notifyDataSetChanged();
        dialogRecyclerView.scrollToPosition(currentHist.size() - 1);

        closeButton.setOnClickListener(v -> dialog.dismiss());

        sendButton.setOnClickListener(v -> {
            String userInput = dialogInputField.getText().toString().trim();
            if (!userInput.isEmpty()) {
                currentHist.add(new ChatMessage("User", userInput));
                chatHistory.add(new ChatMessage("User", userInput));
                adapter.notifyDataSetChanged();
                dialogRecyclerView.scrollToPosition(currentHist.size() - 1);
                sendFullDialogRequest(userInput, isGemini, adapter);
                dialogInputField.setText("");
                saveChatHistory();
            } else {
                Toast.makeText(this, "질문을 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });

        dialogMicrophoneButton.setOnClickListener(v -> speechRecognizerHelper.startSpeechToText());
        dialogPhotoButton.setOnClickListener(v -> photoPickerHelper.hashCode());

        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.9)
        );

        dialog.show();
    }

    private void sendFullDialogRequest(String input, boolean isGemini, ChatMessageAdapter adapter) {
        String url = "http://10.0.2.2:5000/chat";

        JSONArray historyArray = new JSONArray();
        List<ChatMessage> currentHist = isGemini ? geminiChatHistory : chatGptChatHistory;

        try {
            for (ChatMessage message : currentHist) {
                JSONObject messageJson = new JSONObject();
                messageJson.put("role", message.getSender().equals("User") ? "user" : "assistant");
                messageJson.put("content", message.getMessage());
                historyArray.put(messageJson);
            }

            JSONObject userInputJson = new JSONObject();
            userInputJson.put("role", "user");
            userInputJson.put("content", input);
            historyArray.put(userInputJson);

        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("history", historyArray);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    try {
                        String responseText = isGemini ?
                                response.getString("gemini_response") :
                                response.getString("chatgpt_response");

                        currentHist.add(new ChatMessage(isGemini ? "Gemini" : "ChatGPT", responseText));
                        chatHistory.add(new ChatMessage(isGemini ? "Gemini" : "ChatGPT", responseText));
                        saveChatHistory();
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "응답 처리 중 오류 발생", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(MainActivity.this, "서버 연결 실패: " + error.toString(), Toast.LENGTH_LONG).show();
                    error.printStackTrace();
                }
        );

        requestQueue.add(jsonObjectRequest);
    }

    // showChatHistoryDialog()는 필요시 기록 보기용 다이얼로그를 띄울 수 있음.
}

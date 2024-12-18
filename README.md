# Twobot
![Twobot사진](https://github.com/user-attachments/assets/469c5af8-5662-49b4-aabe-3bf669b793db)


챗봇 2개를 사용하는 어플

#프로젝트 소개
Twobot은  두 개의 AI 챗봇인 Gemini와 ChatGPT를 동시에 활용하여 사용자가 두 가지의 챗봇에서 답장을 확인할 수 있는 안드로이드 애플리케이션입니다.
이 앱은 1:1 채팅과 토론 기능을 제공하며 사용자가 기능을 더 잘 사용 할 수 있게 만들었습니다.

+교수님께서 자소서를 적으라고 하셔서 다른 강의를 참고하면서 적고 있었는데 강사님이 자신은 자기소개서를 만들때 챗지피티와 제미나이를 둘 다 사용한다 하셔서 두개의 챗봇을 합치면 어떨까 해서 개발하게 되었습니다.

+챗봇 토론같은경우 챗봇에게 하나에 대해서 물어본다면 그거에대한 한측면정도 말해주는데 더 많은 대답을 듣기 위해서 챗봇끼리 토론을 시켜본다면 어떨까해서 토론도 개발해보게 되었습니다.

#개발 환경

![flask](https://github.com/user-attachments/assets/39f6f75e-db1d-4c09-8a51-cb341f001456)
![python](https://github.com/user-attachments/assets/18870185-7a88-4a3e-8c9b-bf1a0d085c28)
![mlkit](https://github.com/user-attachments/assets/c890c77b-3297-43ac-a973-f3791c339560)
![챗지피티](https://github.com/user-attachments/assets/74f9a609-921c-416e-9e2b-d2c8c2af0e7f)
![제미나이](https://github.com/user-attachments/assets/57b876a7-8ca9-4edb-8732-47a3583fe812)




#UserFlow


![image](https://github.com/user-attachments/assets/58faf73a-040e-4fdb-bdd8-ab2654437b3a)




#ERD
![image](https://github.com/user-attachments/assets/8ec9e99f-13ec-4248-9ce0-05a5353ec17a)

#기능
1.챗지피티와 제미나이 동시에 입력받기

1-1.음성인식으로 텍스트 전환하기

1-2.카메라로 사진을찍어서 MLKIT로 텍스트로 전환하기

2.챗지피티또는 제미나이와 1:1로 통신하기

3.챗지피티와 제미나이 토론하기


#flask 서버
```
from flask import Flask, request, jsonify
import openai
import google.generativeai as genai
import re
from flask_cors import CORS

app = Flask(__name__)
CORS(app)  # CORS 설정
app.config['MAX_CONTENT_LENGTH'] = 32 * 1024 * 1024  # 요청 크기를 32MB로 설정

# ChatGPT API 키 설정
openai.api_key = ""

# Gemini 설정
genai.configure(api_key="")

def remove_markdown_formatting(text):
    """** 표시 및 기타 Markdown 포맷 제거"""
    if not text:
        return text
    text = re.sub(r"\*\*", "", text)
    text = re.sub(r"\*", "", text)
    return text

@app.route('/chat', methods=['POST'])
def chat():
    data = request.json
    history = data.get("history", [])

    if not history:
        return jsonify({"error": "History is empty"}), 400

    # ChatGPT 응답 생성
    try:
        chatgpt_response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=history,
            max_tokens=150,
            temperature=0.7
        )['choices'][0]['message']['content']
        chatgpt_response = remove_markdown_formatting(chatgpt_response)
    except Exception as e:
        chatgpt_response = f"ChatGPT Error: {str(e)}"

    # Gemini 응답 생성
    try:
        gemini_model = genai.GenerativeModel("gemini-pro")
        gemini_prompt = "\n".join([f"{msg['role']}: {msg['content']}" for msg in history])
        gemini_response = gemini_model.generate_content(gemini_prompt).text.strip()
        gemini_response = remove_markdown_formatting(gemini_response)
    except Exception as e:
        gemini_response = f"Gemini Error: {str(e)}"

    return jsonify({
        "chatgpt_response": chatgpt_response,
        "gemini_response": gemini_response
    })

def truncate_text(text, max_length=50):
    """응답을 최대 max_length 길이로 자르기"""
    return text[:max_length] + ("..." if len(text) > max_length else "")

# Debate 엔드포인트

def remove_markdown_formatting(text):
    """Markdown 포맷 제거"""
    if not text:
        return text
    text = re.sub(r"\*\*", "", text)
    text = re.sub(r"\*", "", text)
    return text

@app.route('/debate', methods=['POST'])
def debate():
    data = request.json
    topic = data.get("topic")
    history = data.get("history", [])

    if not topic:
        return jsonify({"error": "Topic is empty"}), 400

    # 현재 진행된 라운드 수 계산
    round_count = len(history) // 2

    if round_count >= 5:  # 각 봇이 5번씩 총 10번의 발언을 마치면 종료
        return jsonify({
            "gemini_response": "토론이 종료되었습니다.",
            "chatgpt_response": "토론이 종료되었습니다.",
            "history": history
        })

    # 현재 차례 결정 (ChatGPT 또는 Gemini)
    current_speaker = "ChatGPT" if len(history) % 2 == 0 else "Gemini"

    gemini_response = ""
    chatgpt_response = ""

    # ChatGPT 차례
    if current_speaker == "ChatGPT":
        try:
            chatgpt_prompt = [
                {"role": "system", "content": f"You are participating in a debate on the topic '{topic}' and should respond concisely."}
            ]
            for msg in history:
                role = "assistant" if msg['role'] == "ChatGPT" else "user"
                chatgpt_prompt.append({"role": role, "content": msg['content']})

            response = openai.ChatCompletion.create(
                model="gpt-3.5-turbo",
                messages=chatgpt_prompt,
                max_tokens=200,
                temperature=0.7
            )
            chatgpt_response = response['choices'][0]['message']['content'].strip()
            chatgpt_response = remove_markdown_formatting(chatgpt_response)
            history.append({"role": "ChatGPT", "content": chatgpt_response})

        except Exception as e:
            chatgpt_response = f"ChatGPT Error: {str(e)}"
            history.append({"role": "ChatGPT", "content": chatgpt_response})

    # Gemini 차례
    else:
        try:
            history_str = "\n".join([f"{msg['role']}: {msg['content']}" for msg in history])
            gemini_prompt = f"{history_str}\nGemini: {topic}에 대해 이전 논점을 반박하거나 새로운 관점으로 의견을 제시해 주세요."
            gemini_model = genai.GenerativeModel("gemini-pro")
            gemini_response = gemini_model.generate_content(gemini_prompt).text.strip()
            gemini_response = remove_markdown_formatting(gemini_response)
            history.append({"role": "Gemini", "content": gemini_response})

        except Exception as e:
            gemini_response = f"Gemini Error: {str(e)}"
            history.append({"role": "Gemini", "content": gemini_response})

    return jsonify({
        "gemini_response": gemini_response,
        "chatgpt_response": chatgpt_response,
        "history": history
    })

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)

```

#-----------------------------------------------------------------서버 코드설명--------------------------------------------------------------------------
```
flask_cors import CORS: CORS(Cross-Origin Resource Sharing)
```
CORS를 사용하는 이유:웹 브라우저에서 보안 정책상 적용되는 동일 출처 정책때문에 발생하는 요청 제한을 완화하여, 다른 도메인(출처)에서 오는 요청을 허용하기 위함입니다.
```@app.route('/chat', methods=['POST'])``` 사용자가 메인화면에서 입력을하였을때 응답을 서버로부터 받기 위한 코드
```@app.route('/debate', methods=['POST'])``` 토론화면에서 제미나이와 챗지피티가 서로 응답을 주고받기 위한 코드

```
# ChatGPT 차례
    if current_speaker == "ChatGPT":
        try:
            chatgpt_prompt = [
                {"role": "system", "content": f"You are participating in a debate on the topic '{topic}' and should respond concisely."}
            ]
            for msg in history:
                role = "assistant" if msg['role'] == "ChatGPT" else "user"
                chatgpt_prompt.append({"role": role, "content": msg['content']})

            response = openai.ChatCompletion.create(
                model="gpt-3.5-turbo",
                messages=chatgpt_prompt,
                max_tokens=200,
                temperature=0.7
            )
            chatgpt_response = response['choices'][0]['message']['content'].strip()
            chatgpt_response = remove_markdown_formatting(chatgpt_response)
            history.append({"role": "ChatGPT", "content": chatgpt_response})

        except Exception as e:
            chatgpt_response = f"ChatGPT Error: {str(e)}"
            history.append({"role": "ChatGPT", "content": chatgpt_response})
```
ChatGPT 차례이면 ChatGPT에게 system 메시지(토론 규칙)와 지금까지의 history를 assistant/user 역할로 변환해 전달.
응답 받으면 history에 추가.
```
    # Gemini 차례
    else:
        try:
            history_str = "\n".join([f"{msg['role']}: {msg['content']}" for msg in history])
            gemini_prompt = f"{history_str}\nGemini: {topic}에 대해 이전 논점을 반박하거나 새로운 관점으로 의견을 제시해 주세요."
            gemini_model = genai.GenerativeModel("gemini-pro")
            gemini_response = gemini_model.generate_content(gemini_prompt).text.strip()
            gemini_response = remove_markdown_formatting(gemini_response)
            history.append({"role": "Gemini", "content": gemini_response})

        except Exception as e:
            gemini_response = f"Gemini Error: {str(e)}"
            history.append({"role": "Gemini", "content": gemini_response})

```


#------------------------------------------------------------------------Android Studio에서 주요 코드--------------------------------------------------------------------

```
private void sendChatRequest() {
    String prompt = inputField.getText().toString().trim();
    if (prompt.isEmpty()) {
        Toast.makeText(this, "질문을 입력하세요.", Toast.LENGTH_SHORT).show();
        return;
    }

    // 사용자 메시지를 히스토리에 추가
    chatHistory.add(new ChatMessage("User", prompt));
    geminiChatHistory.add(new ChatMessage("User", prompt));
    chatGptChatHistory.add(new ChatMessage("User", prompt));

    // 서버로 보낼 JSON 구성
    JSONArray historyArray = new JSONArray();
    try {
        for (ChatMessage message : chatGptChatHistory) {
            JSONObject msgObj = new JSONObject();
            String role = message.getSender().equals("User") ? "user" : "assistant";
            msgObj.put("role", role);
            msgObj.put("content", message.getMessage());
            historyArray.put(msgObj);
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

    // Volley를 통한 서버 요청
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.POST,
            "http://10.0.2.2:5000/chat",
            requestBody,
            response -> {
                // 응답 처리 콜백 (2번 설명 부분)
            },
            error -> {
                Toast.makeText(MainActivity.this, "서버 연결 실패: " + error.toString(), Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
    );

    // 요청 큐에 추가
    requestQueue.add(jsonObjectRequest);

    // 입력 필드 초기화
    inputField.setText("");
}

```
사용자가 EditText에 입력한 텍스트를 가져와 유효성 검사(비어있는지 확인) 후, 대화 기록 리스트(chatHistory, geminiChatHistory, chatGptChatHistory)에 사용자 메시지를 추가합니다.

chatGptChatHistory를 바탕으로 JSON 형태의 historyArray를 생성하고, requestBody에 prompt와 history를 담아 Flask 서버(/chat 엔드포인트)에 POST 요청을 보냅니다.

Volley 라이브러리 사용: 서버 응답이 비동기로 도착하면 response 콜백에서 응답 처리(아래 2번) 수행.

입력 필드 초기화로 다음 메시지 입력 준비.

```
JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
    Request.Method.POST,
    "http://10.0.2.2:5000/chat",
    requestBody,
    response -> {
        try {
            String chatGpt = response.getString("chatgpt_response");
            String gemini = response.getString("gemini_response");

            // 서버 응답을 히스토리에 추가
            geminiChatHistory.add(new ChatMessage("Gemini", gemini));
            chatGptChatHistory.add(new ChatMessage("ChatGPT", chatGpt));
            chatHistory.add(new ChatMessage("Gemini", gemini));
            chatHistory.add(new ChatMessage("ChatGPT", chatGpt));

            // UI 업데이트
            updateChatViews();

            // 초기 메시지 숨기기 및 카드 보이기
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

```
서버가 반환한 JSON 응답에서 chatgpt_response와 gemini_response를 추출합니다.

이 응답들을 geminiChatHistory, chatGptChatHistory, chatHistory에 추가해 기록을 갱신.

updateChatViews()를 호출하여 RecyclerView에 새로운 메시지를 반영하고, 스크롤을 최신 메시지 위치로 이동.

초기 소개 메시지를 숨기고, Gemini/ChatGPT 카드뷰를 표시해 대화 상태를 시각적으로 반영.

```
private void updateChatViews() {
    geminiAdapter.notifyDataSetChanged();
    chatGptAdapter.notifyDataSetChanged();

    geminiRecyclerView.scrollToPosition(geminiChatHistory.size() - 1);
    chatGptRecyclerView.scrollToPosition(chatGptChatHistory.size() - 1);
}
```
어댑터에 데이터 변경 사항을 알려 UI를 갱신하고, RecyclerView를 마지막 메시지로 스크롤하여 사용자가 항상 최신 응답을 볼 수 있도록 합니다.

#실행화면


![image](https://github.com/user-attachments/assets/b3aa440d-9429-4955-8299-78a0271c23ee)
![image](https://github.com/user-attachments/assets/5946c0b8-2330-4033-8560-719a52659a31)
![image](https://github.com/user-attachments/assets/58db15d5-03d8-40f5-a04f-f4513f3c678c)
![image](https://github.com/user-attachments/assets/36c18912-9039-44be-88ca-12c07f974f9c)
![image](https://github.com/user-attachments/assets/f8f2a6ea-89fb-437f-8c45-d4ab7e455714)
![image](https://github.com/user-attachments/assets/97e925d6-0747-414e-91e5-282a7cb78f1e)










#실행영상
[https://drive.google.com/file/d/1t1_depPQAgZlDBDmDrvYjaBr7oKDDVRo](https://drive.google.com/file/d/1t1_depPQAgZlDBDmDrvYjaBr7oKDDVRo)




#아쉬웠던점,추후에 하고싶은것들

기록부분에서 메세지형태로 똑같은 형식으로 기록하기

기록을 여러개로 나눠서 따로 구분하기

챗지피티,제미나이 말고도 다른 api들 사용하여 코드를 줬을때 서로 다른 챗봇들이 토론을 할 수 있도록 만들어보는것

ui를조금 더 사용자 측면에서 보기 좋게 만드는것.

새로운 패이지 버튼을 추가하여 페이지를 초기화하고 이전 페이지는 기록하는것.


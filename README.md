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

#코드설명
```
flask_cors import CORS: CORS(Cross-Origin Resource Sharing)
```
 허용을 위해 사용.
```app.config['MAX_CONTENT_LENGTH'] = 32 * 1024 * 1024```  # 요청 최대 크기: 32MB


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



import os
import sys

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

from agents.gemini import ask_gemini
from services.stt import transcribe_audio_file, AUDIO_MIME_MAP

print("--- Medical Agent Console (Type your message or enter /voice <audio_file>) ---")

while True:
    try:
        message = input("You: ").strip()
    except (KeyboardInterrupt, EOFError):
        break

    if not message:
        continue

    if message.lower() == 'exit':
        break

    # If an audio file or /voice command is passed
    clean_msg = message.strip('"').strip("'")
    ext = os.path.splitext(clean_msg)[1].lower()
    if message.startswith("/voice ") or ext in AUDIO_MIME_MAP:
        audio_path = message.replace("/voice ", "").strip().strip('"').strip("'")
        if os.path.exists(audio_path):
            print(f"🎙️ [Voice Input Detected] Transcribing '{audio_path}'...")
            transcript = transcribe_audio_file(audio_path)
            print(f"📝 Heard: \"{transcript}\"")
            message = transcript
        else:
            print(f"❌ Audio file not found: {audio_path}")
            continue

    response = ask_gemini(message)
    print("AI :", response)
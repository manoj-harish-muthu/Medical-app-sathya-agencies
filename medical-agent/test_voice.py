"""
Interactive Voice & Text Terminal Runner for Medical Agent (Doxy).
Allows testing both typed messages and voice note audio files (.ogg, .wav, .mp3, .m4a).
Completely decoupled: uses services/stt.py for audio and agents/gemini.py for agent reasoning.
"""
import os
import sys

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

from agents.gemini import ask_gemini
from agents.order import Order
from services.stt import transcribe_audio_file, AUDIO_MIME_MAP


def print_order_status():
    order = Order.load("current_customer")
    details = order.get_order()
    print("\n--- Current Order State ---")
    print(f"Customer Name : {details.get('customer_name')}")
    print(f"Phone         : {details.get('phone')}")
    print(f"Address       : {details.get('address')}")
    print(f"Items         : {details.get('items')}")
    print(f"Confirmed     : {details.get('confirmed')}")
    print("---------------------------\n")


def process_audio(file_path: str):
    clean_path = file_path.strip().strip('"').strip("'")
    if not os.path.exists(clean_path):
        print(f"[Error] Audio file not found at '{clean_path}'")
        return

    print(f"\n[Voice Input]: Processing '{clean_path}'...")
    print("Transcribing audio with Gemini Audio STT...")
    try:
        transcript = transcribe_audio_file(clean_path)
    except Exception as e:
        print(f"[Error] Transcription failed: {e}")
        return

    if not transcript:
        print("[Notice] No audible speech detected in the audio file.")
        return

    print(f"[Transcribed Text]: \"{transcript}\"\n")

    # Pass transcribed speech to the medical agent
    print("[Agent]: Processing with Medical Agent (Doxy)...")
    agent_message = f"[Customer Voice Message]: {transcript}"
    response = ask_gemini(agent_message)
    print(f"\nAI: {response}\n")

    print_order_status()


def main():
    print("=" * 60)
    print("Medical Agent (Doxy) - Voice & Text Terminal Tester")
    print("=" * 60)
    print("How to use:")
    print("  • Type a normal text message (e.g. 'I need 2 Dolo 650')")
    print("  • Or test audio by typing: /voice <path_to_audio_file>")
    print("    Example: /voice sample_voice_order.wav")
    print("  • Type 'status' to see the current order JSON")
    print("  • Type 'exit' to quit")
    print("=" * 60)

    # If an argument is provided directly via CLI (e.g. python test_voice.py sample_voice_order.wav)
    if len(sys.argv) > 1:
        target = sys.argv[1]
        process_audio(target)
        if len(sys.argv) > 2 and sys.argv[2] == "--once":
            return

    while True:
        try:
            user_input = input("\nYou: ").strip()
        except (KeyboardInterrupt, EOFError):
            print("\nExiting.")
            break

        if not user_input:
            continue

        if user_input.lower() == "exit":
            print("Goodbye!")
            break

        if user_input.lower() == "status":
            print_order_status()
            continue

        # Check if user entered /voice or dragged an audio file into terminal
        ext = os.path.splitext(user_input.strip('"').strip("'"))[1].lower()
        if user_input.startswith("/voice ") or ext in AUDIO_MIME_MAP:
            audio_path = user_input.replace("/voice ", "").strip()
            process_audio(audio_path)
            continue

        # Normal text message processing
        response = ask_gemini(user_input)
        print(f"\nAI: {response}\n")


if __name__ == "__main__":
    main()

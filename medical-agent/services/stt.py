import os
import logging
from typing import Optional
from dotenv import load_dotenv
from google import genai
from google.genai import types

# Suppress internal SDK AFC warnings for pure generation tasks
logging.getLogger("google.genai").setLevel(logging.ERROR)
logging.getLogger("google_genai").setLevel(logging.ERROR)

load_dotenv()

# Supported audio format mapping
AUDIO_MIME_MAP = {
    ".ogg": "audio/ogg",
    ".opus": "audio/ogg",
    ".wav": "audio/wav",
    ".mp3": "audio/mp3",
    ".m4a": "audio/aac",
    ".aac": "audio/aac",
    ".flac": "audio/flac",
}

DEFAULT_STT_MODEL = "gemini-2.5-flash"

TRANSCRIPTION_PROMPT = """
You are an accurate medical speech-to-text transcription system for WhatsApp voice messages.
Transcribe the user's spoken audio into exact text.

GUIDELINES:
1. Carefully transcribe all medicine brand names, drug names, strengths, quantities, and units (e.g., Dolo 650, Cetirizine, Metexel, strips, tablets, boxes, bottles, pieces).
2. Accurately transcribe customer details such as names, phone numbers, and delivery locations (e.g., Anna Nagar, T. Nagar, Gandhipuram).
3. Accurately capture Indian English accents, colloquial phrasing, and mixed Tanglish (Tamil + English phonetics like 'venum', 'kudunga', 'strip kudunga').
4. Output ONLY the exact transcribed text. Do NOT include any conversation, markdown codeblocks, notes, or explanations.
5. If the audio is completely silent or has only inaudible noise, output: [NO_SPEECH].
"""


def _get_genai_client() -> genai.Client:
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise ValueError("GEMINI_API_KEY environment variable is missing in .env")
    return genai.Client(api_key=api_key)


def get_mime_type(file_path: str) -> str:
    """Detect audio MIME type from file extension."""
    ext = os.path.splitext(file_path)[1].lower()
    return AUDIO_MIME_MAP.get(ext, "audio/ogg")


def transcribe_audio_bytes(
    audio_bytes: bytes,
    mime_type: str = "audio/ogg",
    model: str = DEFAULT_STT_MODEL,
) -> str:
    """
    Transcribe raw audio bytes into text using Gemini Multimodal Audio.
    Independent and decoupled from the agent logic.
    """
    if not audio_bytes:
        return ""

    client = _get_genai_client()
    audio_part = types.Part.from_bytes(data=audio_bytes, mime_type=mime_type)

    config = types.GenerateContentConfig(
        temperature=0.0,
    )

    response = client.models.generate_content(
        model=model,
        contents=[audio_part, TRANSCRIPTION_PROMPT],
        config=config,
    )

    # Extract clean text without thought parts
    text = ""
    if response.candidates and response.candidates[0].content and response.candidates[0].content.parts:
        parts = [
            p.text for p in response.candidates[0].content.parts
            if getattr(p, "text", None) and not getattr(p, "thought", False)
        ]
        text = "".join(parts).strip()
    elif getattr(response, "text", None):
        text = response.text.strip()

    if text == "[NO_SPEECH]":
        return ""

    return text


def transcribe_audio_file(
    file_path: str,
    mime_type: Optional[str] = None,
    model: str = DEFAULT_STT_MODEL,
) -> str:
    """
    Transcribe a local audio file (.ogg, .wav, .mp3, etc.) into text.
    """
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Audio file not found: {file_path}")

    if not mime_type:
        mime_type = get_mime_type(file_path)

    with open(file_path, "rb") as f:
        audio_bytes = f.read()

    return transcribe_audio_bytes(audio_bytes=audio_bytes, mime_type=mime_type, model=model)

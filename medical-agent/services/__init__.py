"""
Services package for external integrations (STT, Webhooks, Meta WhatsApp, etc.)
Kept completely decoupled from the core agents domain.
"""
from services.stt import transcribe_audio_file, transcribe_audio_bytes
from services.whatsapp import (
    send_whatsapp_message,
    send_whatsapp_template,
    send_order_confirmation_template,
    download_media_bytes,
)

__all__ = [
    "transcribe_audio_file",
    "transcribe_audio_bytes",
    "send_whatsapp_message",
    "send_whatsapp_template",
    "send_order_confirmation_template",
    "download_media_bytes",
]

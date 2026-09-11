import os
import requests
from typing import Optional
from dotenv import load_dotenv

load_dotenv()

GRAPH_API_VERSION = "v25.0"
GRAPH_API_URL = f"https://graph.facebook.com/{GRAPH_API_VERSION}"


def get_whatsapp_config():
    """Retrieve WhatsApp Cloud API configuration dynamically from environment."""
    load_dotenv(override=True)
    token = os.getenv("WHATSAPP_TOKEN", "").strip()
    phone_number_id = os.getenv("WHATSAPP_PHONE_NUMBER_ID", "").strip()
    verify_token = os.getenv("WHATSAPP_VERIFY_TOKEN", "medical_agent_token").strip()
    return {
        "token": token,
        "phone_number_id": phone_number_id,
        "verify_token": verify_token,
    }


def send_whatsapp_message(
    to_number: str,
    text: str,
    phone_number_id: Optional[str] = None,
) -> bool:
    """
    Send a plain text message to a user on WhatsApp via Meta Cloud API.
    """
    cfg = get_whatsapp_config()
    token = cfg["token"]
    pid = phone_number_id or cfg["phone_number_id"]

    if not token or not pid:
        print("[WhatsApp Warning] Missing WHATSAPP_TOKEN or WHATSAPP_PHONE_NUMBER_ID in .env. Message cannot be sent.")
        return False

    url = f"{GRAPH_API_URL}/{pid}/messages"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }
    payload = {
        "messaging_product": "whatsapp",
        "recipient_type": "individual",
        "to": to_number,
        "type": "text",
        "text": {
            "preview_url": False,
            "body": text,
        },
    }

    try:
        response = requests.post(url, headers=headers, json=payload, timeout=15)
        if response.status_code in [200, 201]:
            print(f"[WhatsApp] Successfully sent text message to {to_number}")
            return True
        else:
            print(f"[WhatsApp Error] Failed to send text message (HTTP {response.status_code}): {response.text}")
            return False
    except Exception as e:
        print(f"[WhatsApp Exception] Error sending message to {to_number}: {e}")
        return False


def send_whatsapp_template(
    to_number: str,
    template_name: str,
    language_code: str = "en_US",
    components: Optional[list] = None,
    phone_number_id: Optional[str] = None,
) -> bool:
    """
    Send a pre-approved WhatsApp template message (e.g. order confirmations).
    """
    cfg = get_whatsapp_config()
    token = cfg["token"]
    pid = phone_number_id or cfg["phone_number_id"]

    if not token or not pid:
        print("[WhatsApp Warning] Missing WHATSAPP_TOKEN or WHATSAPP_PHONE_NUMBER_ID in .env.")
        return False

    url = f"{GRAPH_API_URL}/{pid}/messages"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }
    payload = {
        "messaging_product": "whatsapp",
        "to": to_number,
        "type": "template",
        "template": {
            "name": template_name,
            "language": {
                "code": language_code
            },
            "components": components or []
        }
    }

    try:
        response = requests.post(url, headers=headers, json=payload, timeout=15)
        if response.status_code in [200, 201]:
            print(f"[WhatsApp] Successfully sent template '{template_name}' to {to_number}")
            return True
        else:
            print(f"[WhatsApp Error] Failed to send template (HTTP {response.status_code}): {response.text}")
            return False
    except Exception as e:
        print(f"[WhatsApp Exception] Error sending template to {to_number}: {e}")
        return False


def send_order_confirmation_template(
    to_number: str,
    customer_name: str = "Customer",
    order_id: str = "123456",
    order_date: Optional[str] = None,
    phone_number_id: Optional[str] = None,
) -> bool:
    """
    Send the Jaspers Market order confirmation template as configured in Meta WhatsApp.
    Template: jaspers_market_order_confirmation_v1
    Parameters: [Name, Order ID, Date]
    """
    from datetime import datetime
    date_str = order_date or datetime.now().strftime("%b %d, %Y")
    components = [
        {
            "type": "body",
            "parameters": [
                {"type": "text", "text": customer_name},
                {"type": "text", "text": str(order_id)},
                {"type": "text", "text": date_str},
            ]
        }
    ]
    return send_whatsapp_template(
        to_number=to_number,
        template_name="jaspers_market_order_confirmation_v1",
        language_code="en_US",
        components=components,
        phone_number_id=phone_number_id,
    )


def download_media_bytes(media_id: str) -> Optional[bytes]:
    """
    Download audio/media binary bytes from Meta Cloud API using the media_id.
    """
    cfg = get_whatsapp_config()
    token = cfg["token"]

    if not token:
        print("[WhatsApp Warning] Missing WHATSAPP_TOKEN in .env. Cannot download media.")
        return None

    headers = {
        "Authorization": f"Bearer {token}",
    }

    try:
        # Step 1: Retrieve temporary media download URL
        meta_url = f"{GRAPH_API_URL}/{media_id}"
        meta_res = requests.get(meta_url, headers=headers, timeout=15)
        if meta_res.status_code != 200:
            print(f"[WhatsApp Error] Failed to get media URL for {media_id}: {meta_res.text}")
            return None

        media_info = meta_res.json()
        download_url = media_info.get("url")
        if not download_url:
            print(f"[WhatsApp Error] No download URL returned for media {media_id}")
            return None

        # Step 2: Download raw binary media bytes
        audio_res = requests.get(download_url, headers=headers, timeout=30)
        if audio_res.status_code == 200:
            print(f"[WhatsApp] Successfully downloaded media {media_id} ({len(audio_res.content)} bytes)")
            return audio_res.content
        else:
            print(f"[WhatsApp Error] Failed to download media content: {audio_res.text}")
            return None
    except Exception as e:
        print(f"[WhatsApp Exception] Error downloading media {media_id}: {e}")
        return None

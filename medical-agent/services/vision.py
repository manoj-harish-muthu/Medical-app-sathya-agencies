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

IMAGE_MIME_MAP = {
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".webp": "image/webp",
    ".gif": "image/gif",
    ".bmp": "image/bmp",
    ".heic": "image/heic",
}

DEFAULT_VISION_MODEL = "gemini-2.5-flash"

PRESCRIPTION_VISION_PROMPT = """
You are an expert clinical pharmacist and medical OCR assistant analyzing an image sent by a customer over WhatsApp for Sathya Agencies pharmacy.

The image may be:
1. A doctor's prescription (handwritten or printed on clinic letterhead)
2. A photo of medicine packaging (strip, blister pack, box, or syrup/tonic bottle)
3. A customer's handwritten or typed shopping list of medicines

YOUR TASK:
Carefully analyze the image and extract all relevant order, medicine, and patient information with high clinical accuracy.

EXTRACTION GUIDELINES:
1. MEDICINES IDENTIFIED:
   For every medicine found:
   - Medicine Brand / Product Name (e.g., Dolo 650, Azithral 500, Pan 40, Benadryl Cough Syrup, Calpol).
   - Strength / Dosage (e.g., 650mg, 500mg, 10mg, 100ml).
   - Quantity & Unit:
     * For tablets/capsules: determine number of strips, tablets, or boxes (e.g., 1 strip, 10 tablets, 2 boxes). If the doctor wrote a dosage regimen like "1-0-1 x 5 days" (BD for 5 days = 10 tablets / 1 strip), indicate the count or estimated strips.
     * For Tonics / Syrups / Liquids: unit MUST be "tonic". Specify bottle volume in ml (e.g., 60ml, 100ml, 200ml) if visible.
   - Any specific instructions or notes from the doctor (e.g., after food, bedtime).

2. PATIENT / CUSTOMER DETAILS (if printed or written on prescription):
   - Patient Name
   - Phone Number
   - Delivery Address / City / Location / Pincode
   - Date of Prescription

3. UNREADABLE / ILLEGIBLE ITEMS:
   - If any medicine name or dosage is blurry, cut off, or illegible doctor's handwriting, explicitly list it under "Illegible / Unclear Medicines" so the order agent can ask the customer to clarify.

4. OVERALL SUMMARY:
   Provide a concise structured summary for the AI order agent.

FORMAT YOUR OUTPUT LIKE THIS:
Prescription Analysis:
- Doctor / Clinic: [Doctor name or Clinic name, if visible, else 'Not specified']
- Patient Name: [Patient name, if visible, else 'Not specified']
- Patient Phone/Address: [Phone/Address, if visible, else 'Not specified']
- Date: [Date on prescription, if visible, else 'Not specified']

Medicines Detected:
1. [Medicine Name] - Quantity: [Quantity] [Unit: strip/tablet/tonic/box], Strength: [Strength], Volume: [Volume ml if tonic], Notes: [Notes]
2. ...

Unclear / Ambiguous Items:
- [List any item that needs clarification from the customer, or 'None']

Additional Observations:
- [Any other relevant detail, e.g. packaging condition, tonic bottle size, customer handwritten text]
"""


def _get_genai_client() -> genai.Client:
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise ValueError("GEMINI_API_KEY environment variable is missing in .env")
    return genai.Client(api_key=api_key)


def get_image_mime_type(file_path: str) -> str:
    """Detect image MIME type from file extension."""
    ext = os.path.splitext(file_path)[1].lower()
    return IMAGE_MIME_MAP.get(ext, "image/jpeg")


def analyze_image_bytes(
    image_bytes: bytes,
    mime_type: str = "image/jpeg",
    caption: str = "",
    model: str = DEFAULT_VISION_MODEL,
) -> str:
    """
    Analyze image bytes (doctor prescription, medicine pack, syrup bottle) using Gemini Vision.
    Returns structured clinical extraction text.
    """
    if not image_bytes:
        return ""

    client = _get_genai_client()
    clean_mime = mime_type.split(";")[0].strip() or "image/jpeg"
    image_part = types.Part.from_bytes(data=image_bytes, mime_type=clean_mime)

    prompt = PRESCRIPTION_VISION_PROMPT
    if caption and caption.strip():
        prompt += f"\n\nCustomer Caption / Accompanying Message:\n\"{caption.strip()}\""

    config = types.GenerateContentConfig(
        temperature=0.1,
    )

    response = client.models.generate_content(
        model=model,
        contents=[image_part, prompt],
        config=config,
    )

    # Extract clean text without internal thought parts
    text = ""
    if response.candidates and response.candidates[0].content and response.candidates[0].content.parts:
        parts = [
            p.text for p in response.candidates[0].content.parts
            if getattr(p, "text", None) and not getattr(p, "thought", False)
        ]
        text = "".join(parts).strip()
    elif getattr(response, "text", None):
        text = response.text.strip()

    return text


def analyze_image_file(
    file_path: str,
    caption: str = "",
    model: str = DEFAULT_VISION_MODEL,
) -> str:
    """
    Analyze an image file from local path using Gemini Vision.
    """
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Image file not found: {file_path}")

    mime_type = get_image_mime_type(file_path)
    with open(file_path, "rb") as f:
        image_bytes = f.read()

    return analyze_image_bytes(image_bytes=image_bytes, mime_type=mime_type, caption=caption, model=model)

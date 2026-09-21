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
You are an expert clinical pharmacist and medical OCR specialist analyzing an image sent by a customer over WhatsApp for Sathya Agencies pharmacy.

The image may be:
1. A doctor's prescription (handwritten or printed on clinic letterhead)
2. A photo of medicine packaging (strip, blister pack, box, or syrup/tonic bottle)
3. A customer's handwritten or typed shopping list of medicines

YOUR TASK:
Carefully analyze the image and extract all relevant order, medicine, and patient information with high clinical accuracy.

CRITICAL CLINICAL EXTRACTION GUIDELINES:

1. STRIKETHROUGHS & CROSSED-OUT MEDICINES (CRITICAL):
   - Scrutinize EVERY single line of medicine on the prescription. Look closely to see if a horizontal, diagonal, wavy, or cross pen line has been drawn through the medicine name, strength, dosage, or quantity circle (for example: a line drawn across 'T. Febura 20mg (10)').
   - Any medicine with a strikethrough / strikeout line means the doctor CANCELLED or WITHDREW that medicine.
   - You MUST categorize it strictly under "CANCELLED / STRIKED-THROUGH MEDICINES (DO NOT ORDER)" and NEVER include it in active prescribed medicines!

2. DOCTOR'S DISPENSE COUNTS (CIRCLED NUMBERS) (CRITICAL):
   - Doctors frequently write a circled number like '(10)', '(14)', '(15)', '(20)', '(30)' immediately next to or after each medicine name.
   - In clinical prescriptions, this circled number represents the EXACT COUNT OF TABLETS OR CAPSULES TO DISPENSE (e.g. '(10)' means exactly 10 tablets or 10 capsules).
   - Prefix 'T.' or 'Tab' = Tablet. Prefix 'C.' or 'Cap' = Capsule. Prefix 'Syp' or 'Susp' = Syrup / Tonic.
   - You MUST extract the exact quantity as number of tablets/capsules (e.g., Quantity: 10, Unit: tablet; or Quantity: 10, Unit: capsule).
   - DO NOT default to '1 strip' when an explicit tablet/capsule count is written!
   - If no circled number or explicit count is written, look at the dosage regimen (e.g., 1 tablet twice daily for 5 days = 10 tablets), or if only strip count is specified (e.g., '1 strip').

3. DOSAGE TABLE COLUMNS (TAMIL / ENGLISH):
   - Prescriptions often feature dosage timing columns such as:
     * காலை / Morning
     * மதியம் / Afternoon
     * மாலை / Evening
     * இரவு / Night
   - Tamil medical terms:
     * "சாப்பாடு" or "சாண்" (short for சாப்பாடு) = After food.
     * "சாப்பாட்டிற்கு முன்" = Before food.
     * "1 சாப்பாடு" = 1 tablet after food.

4. TONICS / SYRUPS / LIQUIDS:
   - For all syrups, suspensions, and tonics, unit MUST be "tonic".
   - Extract the bottle volume in ml (e.g. 60ml, 100ml, 200ml) if visible or written.

5. PATIENT & CLINIC DETAILS:
   - Patient Name, Age, Sex, Phone, Address, Vitals (BP, Pulse, SpO2), Date.
   - Clinic / Hospital Name, Doctor Name, Registration Number, Contact Number.

6. UNREADABLE / AMBIGUOUS ITEMS:
   - If any handwriting is unclear or doubtful, clearly flag it under "Ambiguous / Unclear Items".

FORMAT YOUR OUTPUT EXACTLY AS FOLLOWS:

Prescription Analysis:
- Doctor / Clinic: [Doctor name or Clinic name, if visible, else 'Not specified']
- Patient Name: [Patient name, if visible, else 'Not specified']
- Patient Phone/Address: [Phone/Address, if visible, else 'Not specified']
- Date: [Date on prescription, if visible, else 'Not specified']

Active Prescribed Medicines to Dispense:
1. [Medicine Name with Strength] - Quantity: [Exact number from circled count or regimen] [Unit: tablet/capsule/tonic/strip], Dosage Timing: [e.g. 1 morning after food], Notes: [Doctor instructions]
2. ...

CANCELLED / STRIKED-THROUGH MEDICINES (DO NOT ORDER):
- [Medicine Name with Strength] - Reason: [e.g. 'Striked through with pen line by doctor - CANCELLED / EXCLUDED']
(If no striked-through medicines are found, write 'None')

Ambiguous / Unclear Items:
- [List any item that needs clarification from the customer, or 'None']

Additional Observations:
- [Patient vitals, Tamil dosage notations, packaging condition, or customer handwritten text]
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

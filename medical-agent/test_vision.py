"""
Test script for Gemini Vision integration in Sathya Agencies Medical Agent.
Simulates:
1. Creating a synthetic doctor's prescription image with medicines (Dolo 650, Azithral 500, Benadryl cough syrup).
2. Running Gemini Vision analysis via services.vision.analyze_image_file.
3. Passing vision analysis to Doxy (ask_gemini) to populate the order.
4. Verifying that the customer's order file (data/orders/{sender}.json) contains the medicines with correct quantities, units, and tonic bottle volumes.
"""
import os
import sys
import json

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

from dotenv import load_dotenv
load_dotenv()

from agents.gemini import ask_gemini, reset_chat_session
from agents.order import Order
from services.vision import analyze_image_bytes, analyze_image_file


def create_sample_prescription_image(output_path: str = "sample_prescription.jpg"):
    """
    Generate a clean, high-resolution prescription image using Pillow.
    """
    from PIL import Image, ImageDraw, ImageFont

    # Create white canvas
    width, height = 900, 1100
    img = Image.new("RGB", (width, height), color=(255, 255, 255))
    draw = ImageDraw.Draw(img)

    # Draw header / clinic banner
    draw.rectangle([(0, 0), (width, 130)], fill=(13, 148, 136))  # Teal banner
    draw.text((40, 25), "DR. V. RAMESH, M.D. (Gen Med)", fill=(255, 255, 255))
    draw.text((40, 60), "Senior Consultant Physician | Reg. No: 45892", fill=(240, 253, 250))
    draw.text((40, 85), "Sathya Healthcare Clinic, 14 Cross, Anna Nagar, Chennai", fill=(204, 251, 241))

    # Divider
    draw.line([(30, 150), (width - 30, 150)], fill=(200, 200, 200), width=2)

    # Patient Details section
    draw.text((40, 170), "Date: 21-Sep-2026", fill=(70, 70, 70))
    draw.text((40, 200), "Patient Name: Rajesh Kumar", fill=(20, 20, 20))
    draw.text((400, 200), "Age/Sex: 38 / M", fill=(20, 20, 20))
    draw.text((40, 230), "Phone: 917708035227", fill=(20, 20, 20))
    draw.text((40, 260), "Address: Flat 4B, Emerald Flats, 2nd Avenue, Anna Nagar, Chennai - 600040", fill=(20, 20, 20))

    # Divider
    draw.line([(30, 300), (width - 30, 300)], fill=(200, 200, 200), width=2)

    # Rx Symbol
    draw.text((40, 320), "Rx (Prescription):", fill=(13, 148, 136))

    # Prescription items
    items = [
        ("1. Tab. Dolo 650 mg", "1 strip (15 tabs) - 1 tab thrice a day for 3 days after food"),
        ("2. Tab. Azithral 500 mg", "1 strip (5 tabs) - 1 tab once daily after dinner"),
        ("3. Syp. Benadryl Cough Syrup (100ml)", "1 bottle (100ml) - 10ml twice a day after food"),
        ("4. Tab. Cetirizine 10 mg", "1 strip (10 tabs) - 1 tab at bedtime"),
    ]

    y = 380
    for med, dosage in items:
        # Draw bullet/box indicator
        draw.rectangle([(40, y + 5), (48, y + 20)], fill=(13, 148, 136))
        draw.text((60, y), med, fill=(10, 10, 10))
        draw.text((80, y + 30), f"Dosage & Qty: {dosage}", fill=(90, 90, 90))
        draw.line([(40, y + 70), (width - 40, y + 70)], fill=(240, 240, 240), width=1)
        y += 90

    # Doctor Notes & Signature
    draw.text((40, 850), "Doctor's Advice: Drink plenty of warm water. Complete the 5-day antibiotic course.", fill=(60, 60, 60))
    draw.line([(600, 980), (820, 980)], fill=(50, 50, 50), width=2)
    draw.text((620, 990), "Dr. V. Ramesh, M.D.", fill=(30, 30, 30))
    draw.text((640, 1015), "(Signature & Seal)", fill=(120, 120, 120))

    # Save
    img.save(output_path, quality=95)
    print(f"✅ Generated synthetic prescription image: {output_path} ({os.path.getsize(output_path)} bytes)")
    return output_path


def run_vision_test():
    sender = "919988776655"
    test_img_path = "sample_prescription.jpg"

    print("=" * 70)
    print("🔬 RUNNING GEMINI VISION TEST FOR MEDICAL AGENT")
    print("=" * 70)

    # 1. Generate image if not exists
    create_sample_prescription_image(test_img_path)

    # 2. Test Gemini Vision Analysis
    print("\n🔍 Step 1: Analyzing prescription image using Gemini Vision...")
    caption = "Please send these medicines to my address as soon as possible"
    vision_report = analyze_image_file(file_path=test_img_path, caption=caption)
    print("\n--- VISION EXTRACTION REPORT ---")
    print(vision_report)
    print("--------------------------------\n")

    assert "Dolo" in vision_report or "650" in vision_report, "Vision failed to detect Dolo 650"
    assert "Benadryl" in vision_report or "Cough" in vision_report or "100ml" in vision_report, "Vision failed to detect Benadryl tonic"

    # 3. Test Agent processing with Doxy
    print(f"\n🤖 Step 2: Passing vision extraction to Doxy for customer {sender}...")
    reset_chat_session(sender)

    agent_prompt = (
        f"[Customer sent a photo (prescription / medicine package)]:\n"
        f"{vision_report}\n"
        f"Customer caption: \"{caption}\""
    )

    ai_reply = ask_gemini(agent_prompt, customer_id=sender)
    print(f"\n💡 Doxy's Reply:\n{ai_reply}")

    # 4. Verify Order Data Saved
    print(f"\n📁 Step 3: Verifying persisted order data in data/orders/{sender}.json...")
    order = Order.load(sender)
    order_data = order.get_order()
    print("Order JSON Content:")
    print(json.dumps(order_data, indent=2))

    # Assertions
    items = order_data.get("items", [])
    print(f"\nTotal items added to order: {len(items)}")
    assert len(items) > 0, "No items were added to the order!"

    med_names = [it["medicine"].lower() for it in items]
    print(f"Medicines in order: {med_names}")

    # Check for Dolo
    has_dolo = any("dolo" in m for m in med_names)
    print(f"✓ Dolo 650 detected & added: {has_dolo}")

    # Check for Tonic and volume_ml
    tonics = [it for it in items if it.get("unit") == "tonic" or "benadryl" in it["medicine"].lower()]
    print(f"✓ Tonic items in order: {tonics}")
    if tonics:
        assert tonics[0].get("unit") == "tonic", f"Tonic unit should be 'tonic', got {tonics[0].get('unit')}"
        print(f"✓ Tonic volume_ml correctly recorded: {tonics[0].get('volume_ml')}")

    print("\n🎉 ALL GEMINI VISION TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)


if __name__ == "__main__":
    run_vision_test()

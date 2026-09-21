"""
Flask Backend Server for Medical Agent (Doxy) with Meta WhatsApp Integration.
Supports text messages and voice notes (.ogg/opus), transcribing them via Gemini Audio STT,
and persisting orders as data/orders/{sender_number}.json.
"""
import os
import sys
from collections import deque
from flask import Flask, request, jsonify
from dotenv import load_dotenv

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

load_dotenv()

from agents.gemini import ask_gemini
from agents.order import Order
from services.stt import transcribe_audio_bytes
from services.vision import analyze_image_bytes
from services.whatsapp import (
    send_whatsapp_message,
    send_whatsapp_template,
    send_order_confirmation_template,
    download_media_bytes,
    get_whatsapp_config,
)

app = Flask(__name__)

# Keep a cache of the last 1000 processed message IDs to avoid duplicate processing from Meta retries
_processed_msg_ids = deque(maxlen=1000)


@app.route("/", methods=["GET"])
@app.route("/webhook", methods=["GET"])
def verify_webhook():
    """
    Verification endpoint called by Meta when you configure the Webhook in Meta Developer Portal.
    Handles verification on both / and /webhook paths.
    """
    mode = request.args.get("hub.mode")
    token = request.args.get("hub.verify_token")
    challenge = request.args.get("hub.challenge")

    # If it's just a normal browser visit to / without hub query params
    if not mode and not token and not challenge:
        return jsonify({
            "service": "Sathya Agencies Medical Agent Backend",
            "status": "online",
            "webhook_url": "/webhook",
        }), 200

    cfg = get_whatsapp_config()
    verify_token = cfg["verify_token"]

    if mode == "subscribe" and token == verify_token:
        print(f"\n✅ [Webhook Verified] Meta webhook verified successfully! Challenge: {challenge}")
        return challenge, 200
    else:
        print(f"\n❌ [Webhook Verification Failed] Expected '{verify_token}', got '{token}'")
        return "Forbidden: Token mismatch", 403


@app.route("/", methods=["POST"])
@app.route("/webhook", methods=["POST"])
def webhook():
    """
    Webhook handler for incoming WhatsApp messages (text and voice notes).
    Accepts on both / and /webhook.
    """
    data = request.get_json(silent=True) or {}
    print(f"\n📩 [Webhook Event Received on {request.path}]")

    # Confirm event comes from WhatsApp
    if data.get("object") != "whatsapp_business_account":
        print(f"[Notice] Non-WhatsApp event or empty payload received: {data}")
        return jsonify({"status": "ignored"}), 200

    try:
        entries = data.get("entry", [])
        for entry in entries:
            changes = entry.get("changes", [])
            for change in changes:
                value = change.get("value", {})
                messages = value.get("messages", [])
                statuses = value.get("statuses", [])
                metadata = value.get("metadata", {})
                phone_number_id = metadata.get("phone_number_id")

                # Log any status updates (sent, delivered, read)
                for st in statuses:
                    print(f"📊 [Delivery Status] Message {st.get('id')} is '{st.get('status')}' for {st.get('recipient_id')}")

                # Process each incoming message
                for msg in messages:
                    msg_id = msg.get("id")
                    if msg_id and msg_id in _processed_msg_ids:
                        print(f"⏩ [Webhook] Skipping duplicate message: {msg_id}")
                        continue

                    if msg_id:
                        _processed_msg_ids.append(msg_id)

                    sender = msg.get("from")  # e.g. "917708035227"
                    msg_type = msg.get("type")

                    if not sender:
                        continue

                    # Ensure customer order file exists immediately
                    order = Order.load(sender)
                    order.save()

                    print(f"\n💬 [Incoming Message] Sender: {sender} | Type: {msg_type}")

                    was_confirmed_before = order.confirmed

                    # Handle Text Message
                    if msg_type == "text":
                        text_body = msg.get("text", {}).get("body", "").strip()
                        print(f"📝 Customer Text: \"{text_body}\"")

                        # Pass to medical agent with customer_id=sender
                        print("🤖 Processing with Doxy...")
                        ai_reply = ask_gemini(text_body, customer_id=sender)
                        print(f"💡 AI Reply: {ai_reply}")

                        # Send reply back to customer on WhatsApp
                        send_whatsapp_message(to_number=sender, text=ai_reply, phone_number_id=phone_number_id)

                        # If the order was newly confirmed in this turn, send the official order confirmation template card
                        curr_order = Order.load(sender)
                        if not was_confirmed_before and curr_order.confirmed:
                            print(f"🎉 [Order Confirmed] Sending official confirmation template to {sender}...")
                            send_order_confirmation_template(
                                to_number=sender,
                                customer_name=curr_order.customer_name or "Customer",
                                order_id=sender[-6:],
                                phone_number_id=phone_number_id,
                            )

                    # Handle Voice / Audio Message
                    elif msg_type == "audio":
                        audio_data = msg.get("audio", {})
                        media_id = audio_data.get("id")
                        mime_type = audio_data.get("mime_type", "audio/ogg; codecs=opus").split(";")[0].strip()

                        print(f"🎙️ Downloading voice note (media_id: {media_id}, mime: {mime_type})...")
                        audio_bytes = download_media_bytes(media_id)

                        if not audio_bytes:
                            send_whatsapp_message(
                                to_number=sender,
                                text="Sorry, I could not download the voice note. Please try again or type your message.",
                                phone_number_id=phone_number_id,
                            )
                            continue

                        print(f"⏳ Transcribing voice note with Gemini STT...")
                        transcript = transcribe_audio_bytes(audio_bytes=audio_bytes, mime_type=mime_type)
                        print(f"📝 Transcribed Text: \"{transcript}\"")

                        if not transcript:
                            send_whatsapp_message(
                                to_number=sender,
                                text="I couldn't detect any clear speech in your voice note. Please speak a little louder or send a text.",
                                phone_number_id=phone_number_id,
                            )
                            continue

                        # Pass transcript to medical agent
                        agent_prompt = f"[Customer Voice Note]: {transcript}"
                        ai_reply = ask_gemini(agent_prompt, customer_id=sender)

                        # Formulate final response with transparency
                        final_reply = f"🎙️ *I heard*: \"{transcript}\"\n\n{ai_reply}"
                        print(f"💡 Final AI Reply: {final_reply}")

                        # Send reply back to WhatsApp
                        send_whatsapp_message(to_number=sender, text=final_reply, phone_number_id=phone_number_id)

                        # If the order was newly confirmed in this turn, send the official order confirmation template card
                        curr_order = Order.load(sender)
                        if not was_confirmed_before and curr_order.confirmed:
                            print(f"🎉 [Order Confirmed] Sending official confirmation template to {sender}...")
                            send_order_confirmation_template(
                                to_number=sender,
                                customer_name=curr_order.customer_name or "Customer",
                                order_id=sender[-6:],
                                phone_number_id=phone_number_id,
                            )

                    # Handle Image / Photo Message (Prescriptions, medicine strips/boxes, syrup bottles)
                    elif msg_type == "image" or (msg_type == "document" and msg.get("document", {}).get("mime_type", "").startswith("image/")):
                        media_data = msg.get("image") if msg_type == "image" else msg.get("document", {})
                        media_id = media_data.get("id")
                        mime_type = media_data.get("mime_type", "image/jpeg").split(";")[0].strip()
                        caption = media_data.get("caption", "").strip()

                        print(f"📷 Downloading image (media_id: {media_id}, mime: {mime_type}, caption: '{caption}')...")
                        image_bytes = download_media_bytes(media_id)

                        if not image_bytes:
                            send_whatsapp_message(
                                to_number=sender,
                                text="Sorry, I could not download the image. Please try sending the photo again.",
                                phone_number_id=phone_number_id,
                            )
                            continue

                        # Save local copy of prescription/medicine photo
                        try:
                            import datetime
                            os.makedirs("data/prescriptions", exist_ok=True)
                            ext = ".jpg" if ("jpeg" in mime_type or "jpg" in mime_type) else (".png" if "png" in mime_type else ".jpg")
                            ts = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
                            local_img_path = f"data/prescriptions/{sender}_{ts}{ext}"
                            with open(local_img_path, "wb") as f_img:
                                f_img.write(image_bytes)
                            print(f"💾 Saved prescription image to {local_img_path}")
                        except Exception as save_err:
                            print(f"⚠️ Warning: Could not save local prescription copy: {save_err}")

                        print(f"⏳ Analyzing photo with Gemini Vision...")
                        vision_analysis = analyze_image_bytes(image_bytes=image_bytes, mime_type=mime_type, caption=caption)
                        print(f"📋 Vision Analysis Result:\n{vision_analysis}")

                        if not vision_analysis:
                            send_whatsapp_message(
                                to_number=sender,
                                text="I received your photo, but could not detect readable medicines or prescription text. Please take a clearer photo or type your medicine names.",
                                phone_number_id=phone_number_id,
                            )
                            continue

                        # Pass vision analysis to Doxy
                        agent_prompt = (
                            f"[Customer sent a photo (prescription / medicine package)]:\n"
                            f"{vision_analysis}\n"
                        )
                        if caption:
                            agent_prompt += f"\nCustomer caption: \"{caption}\""

                        print("🤖 Processing with Doxy...")
                        ai_reply = ask_gemini(agent_prompt, customer_id=sender)
                        print(f"💡 AI Reply: {ai_reply}")

                        # Send reply back to WhatsApp
                        send_whatsapp_message(to_number=sender, text=ai_reply, phone_number_id=phone_number_id)

                        # If the order was newly confirmed in this turn, send the official order confirmation template card
                        curr_order = Order.load(sender)
                        if not was_confirmed_before and curr_order.confirmed:
                            print(f"🎉 [Order Confirmed] Sending official confirmation template to {sender}...")
                            send_order_confirmation_template(
                                to_number=sender,
                                customer_name=curr_order.customer_name or "Customer",
                                order_id=sender[-6:],
                                phone_number_id=phone_number_id,
                            )

                    else:
                        print(f"ℹ️ [Notice] Received media type '{msg_type}'. Currently text, audio voice notes, and images are supported.")

    except Exception as e:
        print(f"❌ [Webhook Error] Exception processing webhook: {e}")

    # Return 200 OK immediately to Meta so it doesn't retry
    return jsonify({"status": "received"}), 200


@app.route("/send_test_template", methods=["GET", "POST"])
def send_test_template_route():
    """
    Endpoint to test sending the official Jaspers Market order confirmation template.
    GET/POST /send_test_template?to=917708035227
    """
    to_number = request.args.get("to") or (request.get_json() or {}).get("to", "917708035227")
    success = send_order_confirmation_template(
        to_number=to_number,
        customer_name="John Doe",
        order_id="123456",
        order_date="Sep 9, 2026",
    )
    return jsonify({
        "recipient": to_number,
        "template": "jaspers_market_order_confirmation_v1",
        "success": success,
    }), (200 if success else 500)


@app.route("/simulate_message", methods=["POST"])
def simulate_message():
    """
    Test endpoint allowing you to simulate incoming WhatsApp messages directly
    without waiting for Meta webhooks.
    POST /simulate_message
    Body: {"from": "917708035227", "text": "I need 2 strips of Dolo 650"}
    """
    data = request.get_json() or {}
    sender = str(data.get("from", "917708035227")).strip()
    text = data.get("text", "").strip()

    if not text:
        return jsonify({"error": "text field is required"}), 400

    ai_reply = ask_gemini(text, customer_id=sender)
    order = Order.load(sender)

    return jsonify({
        "sender": sender,
        "input_text": text,
        "ai_reply": ai_reply,
        "saved_order_file": f"data/orders/{sender}.json",
        "current_order": order.get_order(),
    }), 200


@app.route("/simulate_image", methods=["POST"])
def simulate_image():
    """
    Test endpoint allowing you to simulate incoming WhatsApp image/prescription messages directly
    without waiting for Meta webhooks.
    POST /simulate_image
    Body JSON options:
      1) {"from": "917708035227", "image_path": "path/to/prescription.jpg", "caption": "optional caption"}
      2) {"from": "917708035227", "base64_image": "...", "mime_type": "image/jpeg", "caption": "..."}
    """
    import base64
    from services.vision import get_image_mime_type

    data = request.get_json() or {}
    sender = str(data.get("from", "917708035227")).strip()
    image_path = data.get("image_path", "").strip()
    caption = data.get("caption", "").strip()
    b64_data = data.get("base64_image", "").strip()

    image_bytes = None
    mime_type = "image/jpeg"

    if b64_data:
        image_bytes = base64.b64decode(b64_data)
        mime_type = data.get("mime_type", "image/jpeg")
    elif image_path:
        if not os.path.exists(image_path):
            return jsonify({"error": f"image_path '{image_path}' does not exist"}), 400
        mime_type = get_image_mime_type(image_path)
        with open(image_path, "rb") as f:
            image_bytes = f.read()
    else:
        return jsonify({"error": "Either 'image_path' or 'base64_image' must be provided"}), 400

    order = Order.load(sender)
    order.save()
    was_confirmed_before = order.confirmed

    # Save local copy in data/prescriptions/
    try:
        import datetime
        os.makedirs("data/prescriptions", exist_ok=True)
        ext = ".jpg" if ("jpeg" in mime_type or "jpg" in mime_type) else (".png" if "png" in mime_type else ".jpg")
        ts = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        local_img_path = f"data/prescriptions/{sender}_{ts}{ext}"
        with open(local_img_path, "wb") as f_img:
            f_img.write(image_bytes)
    except Exception as e:
        print(f"⚠️ Warning saving copy: {e}")

    print(f"⏳ Analyzing photo with Gemini Vision for customer {sender}...")
    vision_analysis = analyze_image_bytes(image_bytes=image_bytes, mime_type=mime_type, caption=caption)
    print(f"📋 Vision Analysis Result:\n{vision_analysis}")

    agent_prompt = f"[Customer sent a photo (prescription / medicine package)]:\n{vision_analysis}\n"
    if caption:
        agent_prompt += f"\nCustomer caption: \"{caption}\""

    print("🤖 Processing with Doxy...")
    ai_reply = ask_gemini(agent_prompt, customer_id=sender)
    curr_order = Order.load(sender)

    return jsonify({
        "sender": sender,
        "vision_analysis": vision_analysis,
        "caption": caption,
        "ai_reply": ai_reply,
        "saved_order_file": f"data/orders/{sender}.json",
        "current_order": curr_order.get_order(),
        "confirmed": curr_order.confirmed,
    }), 200


if __name__ == "__main__":
    port = int(os.getenv("PORT", 5000))
    print("=" * 60)
    print(f"🚀 Starting Medical Agent Backend on http://0.0.0.0:{port}")
    print(f"📡 Meta Webhook URL: http://localhost:{port}/webhook")
    print(f"🔗 Forward with ngrok: ngrok http {port}")
    print("=" * 60)
    app.run(host="0.0.0.0", port=port, debug=False)

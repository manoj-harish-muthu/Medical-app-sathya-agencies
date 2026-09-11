import os
import sys
import requests
from dotenv import load_dotenv

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

load_dotenv(override=True)

token = os.getenv("WHATSAPP_TOKEN", "").strip()
phone_id = os.getenv("WHATSAPP_PHONE_NUMBER_ID", "").strip()
webhook_url = os.getenv("WEBHOOK_URL", "").strip()

print("=" * 60)
print("[Meta WhatsApp Cloud API Diagnostic]")
print("=" * 60)

if not token or not phone_id:
    print("❌ Error: WHATSAPP_TOKEN or WHATSAPP_PHONE_NUMBER_ID missing in .env")
    exit(1)

# Step 1: Check token validity by fetching phone number info
print(f"1. Checking Phone Number ID: {phone_id}...")
headers = {"Authorization": f"Bearer {token}"}
res = requests.get(f"https://graph.facebook.com/v25.0/{phone_id}", headers=headers)

if res.status_code != 200:
    print(f"[ERROR] Token Error (HTTP {res.status_code}):")
    print(res.json())
    print("\n[EXPIRED] YOUR TEMPORARY ACCESS TOKEN HAS EXPIRED!")
    print("Please generate a new Temporary Access Token in Meta Developer Portal:")
    print("  1. Go to developers.facebook.com -> Your App -> WhatsApp -> API Setup")
    print("  2. Copy the 'Temporary access token'")
    print("  3. Paste it into .env (WHATSAPP_TOKEN=...) and re-run this script.")
    exit(1)

data = res.json()
print("[OK] Token is VALID!")
print(f"   Display Phone Number: {data.get('display_phone_number')}")
print(f"   Verified Name: {data.get('verified_name')}")
print(f"   Quality Rating: {data.get('quality_rating')}")

# Step 2: Fetch WhatsApp Business Account ID (WABA ID)
print("\n2. Fetching WhatsApp Business Account (WABA) ID...")
res_waba = requests.get(
    f"https://graph.facebook.com/v25.0/{phone_id}?fields=whatsapp_business_account",
    headers=headers,
)
waba_id = None
if res_waba.status_code == 200:
    waba_data = res_waba.json()
    waba_id = waba_data.get("whatsapp_business_account", {}).get("id")
    print(f"[OK] Found WABA ID: {waba_id}")
else:
    print(f"[Notice] Could not fetch WABA ID directly: {res_waba.text}")

# Step 3: Automatically subscribe the WABA to the app webhook
if waba_id:
    print(f"\n3. Subscribing WABA ({waba_id}) to webhook events (subscribed_apps)...")
    sub_res = requests.post(
        f"https://graph.facebook.com/v25.0/{waba_id}/subscribed_apps",
        headers=headers,
    )
    if sub_res.status_code in [200, 201] and sub_res.json().get("success"):
        print("[SUCCESS] WABA is now actively subscribed to forward incoming WhatsApp messages to your webhook!")
    else:
        print(f"[Notice] Subscribed apps response (HTTP {sub_res.status_code}): {sub_res.text}")

# Step 4: Verify Webhook URL connectivity
print("\n4. Checking ngrok webhook endpoint...")
if webhook_url:
    try:
        check_url = f"{webhook_url}?hub.mode=subscribe&hub.verify_token=medical_agent_token&hub.challenge=test_check"
        wh_res = requests.get(check_url, timeout=10)
        if wh_res.status_code == 200 and wh_res.text.strip() == "test_check":
            print(f"[OK] Webhook endpoint '{webhook_url}' is LIVE and verified!")
        else:
            print(f"[Notice] Webhook endpoint returned {wh_res.status_code}: {wh_res.text}")
    except Exception as e:
        print(f"[Error] Error connecting to webhook URL '{webhook_url}': {e}")
else:
    print("[Notice] WEBHOOK_URL not set in .env")

print("\n" + "=" * 60)
print("Diagnostic Complete.")
print("=" * 60)

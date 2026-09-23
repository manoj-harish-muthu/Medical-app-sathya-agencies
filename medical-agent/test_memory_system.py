"""
End-to-End Verification Test for 2-Tier Customer Memory System in Sathya Agencies Medical Agent.
Tests:
1. Phone normalization across formats (+91, 91, 0, local).
2. Tier 1 Relational Profile Memory (Address saved once, remembered across all sessions).
3. Dynamic Prompt Generation with Saved Address injection.
4. Tier 2 Semantic & Episodic Memory (pgvector vector search on Neon PostgreSQL).
"""

import os
import sys

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from services.memory_db import (
    normalize_phone,
    get_customer_profile,
    save_customer_profile,
    store_memory,
    search_memories,
    get_customer_preferences,
    init_memory_db,
)
from agents.order import Order
from agents.tools import set_current_customer, recall_customer_memory, save_customer_preference
from agents.gemini import build_system_instruction

def run_tests():
    print("=================================================================")
    print("🚀 SATHYA AGENCIES — 2-TIER CUSTOMER MEMORY VERIFICATION")
    print("=================================================================\n")

    # 1. Test Phone Normalization
    print("--- 1. Testing Phone Normalization ---")
    assert normalize_phone("+91 98765 43210") == "9876543210", "Failed on +91 format"
    assert normalize_phone("919876543210") == "9876543210", "Failed on WhatsApp 91 format"
    assert normalize_phone("09876543210") == "9876543210", "Failed on leading zero format"
    assert normalize_phone("9876543210") == "9876543210", "Failed on standard 10 digit"
    print("✅ Phone normalization passed for all phone formats.\n")

    # 2. Test Tier 1 Relational Customer Memory (Address saved once, remembered forever)
    print("--- 2. Testing Tier 1 Customer Profile & Address Memory ---")
    test_phone = "9789012345"
    test_whatsapp_id = f"91{test_phone}"
    test_name = "Suresh Raina"
    test_address = "Flat 3A, Green Park Apts, 2nd Main Road, Anna Nagar, Chennai 600040"

    # Simulate customer placing their first order and providing their address once
    set_current_customer(test_whatsapp_id)
    order1 = Order(test_whatsapp_id)
    order1.set_customer_details(name=test_name, phone=test_phone, address=test_address)
    order1.add_item("Dolo 650", quantity=2, unit="strip")
    order1.add_item("Benadryl", quantity="100ml", unit="tonic", volume_ml="100ml")
    confirmed = order1.confirm()
    print(f"Order 1 Confirmed: {confirmed}")
    assert confirmed, "Order 1 should be complete and confirmed"

    # Verify directly in PostgreSQL
    profile = get_customer_profile(test_phone)
    print(f"Profile fetched from DB: {profile}")
    assert profile is not None, "Profile must exist in PostgreSQL"
    assert profile["phone"] == test_phone, "Phone mismatch"
    assert profile["address"] == test_address, "Address mismatch"
    print("✅ Address successfully saved to PostgreSQL on phone number key.\n")

    # Simulate a brand new session later (e.g. from WhatsApp or Voice call)
    print("--- 3. Testing Address Auto-Recall on New Conversation ---")
    # Delete local file cache to prove it loads directly from PostgreSQL
    local_file = f"data/orders/{test_whatsapp_id}.json"
    if os.path.exists(local_file):
        os.remove(local_file)
    if os.path.exists(f"data/orders/{test_phone}.json"):
        os.remove(f"data/orders/{test_phone}.json")

    order2 = Order.load(test_whatsapp_id)
    print(f"Fresh Order loaded for {test_whatsapp_id}:")
    print(f"  - Customer Name: {order2.customer_name}")
    print(f"  - Phone: {order2.phone}")
    print(f"  - Saved Address: {order2.address}")

    assert order2.customer_name == test_name, "Customer name should be remembered from DB"
    assert order2.address == test_address, "Delivery address should be remembered from DB"
    print("✅ Returning customer address automatically restored from PostgreSQL memory without asking user!\n")

    # 4. Test Dynamic Prompt Generation for Gemini
    print("--- 4. Testing Gemini Instruction with Saved Address ---")
    instruction = build_system_instruction(test_whatsapp_id)
    assert test_address in instruction, "System instruction must contain the saved address"
    assert "DO NOT ask them for their address or pincode again" in instruction, "Rule must be present"
    print("✅ System prompt successfully instructs Doxy to reuse saved address and avoid re-asking.\n")

    # 5. Test Tier 2 Semantic & Episodic Memory (pgvector)
    print("--- 5. Testing Tier 2 Semantic Memory (pgvector) ---")
    # Save a medical preference
    pref_res = save_customer_preference("Patient is allergic to aspirin, prefers paracetamol based medicines")
    print(f"Save Preference Result: {pref_res}")

    # Recall past memories using semantic search
    recall_res = recall_customer_memory("What did I order last time?")
    print(f"Recall Memory for 'What did I order last time?':")
    print(f"  Found: {recall_res.get('found')}")
    if recall_res.get("memories"):
        for m in recall_res["memories"]:
            print(f"  - [{m['type']}] (Score: {m['relevance']}): {m['content']}")

    assert recall_res.get("found") is True, "Semantic memory should find the past confirmed order"
    print("✅ pgvector successfully recalled past order by semantic query!\n")

    print("=================================================================")
    print("🎉 ALL TESTS PASSED! 2-TIER HYBRID MEMORY FULLY FUNCTIONAL!")
    print("=================================================================")

if __name__ == "__main__":
    run_tests()

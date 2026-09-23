import contextvars
from typing import Optional, Union, List, Dict, Any
from agents.order import Order
from services.memory_db import (
    search_memories,
    store_memory,
    get_customer_profile,
    get_customer_preferences,
    normalize_phone,
)

# ContextVar to maintain active customer ID per request/thread
_current_customer_var = contextvars.ContextVar("current_customer_id", default="current_customer")


def set_current_customer(customer_id: str):
    """Set the active customer context (e.g. WhatsApp phone number)."""
    _current_customer_var.set(customer_id)


def get_current_customer() -> str:
    """Retrieve the active customer context."""
    return _current_customer_var.get()


def add_medicine(
    medicine: str,
    quantity: Union[int, str] = 1,
    unit: str = "strip",
    pieces_per_unit: Optional[int] = None,
    volume_ml: Optional[str] = None,
) -> dict:
    """
    Add a medicine and its quantity to the current customer order.

    IMPORTANT RULES:
    - For tonics / syrups / liquids (e.g. 'Irumal tonic', 'Kachal tonic', 'cough syrup'):
      * Always set unit="tonic" (NEVER 'strip').
      * Always pass volume_ml (e.g. '60ml', '100ml', '200ml'). If the customer hasn't specified ml, ask them.
    - For tablets / capsules:
      * unit is typically 'strip', 'tablet', or 'piece'.
    - If the customer's previous order was already confirmed and they are ordering another product,
      adding an item automatically archives the old confirmed order and starts a fresh new order.
    """
    order = Order.load(get_current_customer())

    order.add_item(
        medicine=medicine,
        quantity=quantity,
        unit=unit,
        pieces_per_unit=pieces_per_unit,
        volume_ml=volume_ml,
    )

    return order.get_order()


def update_medicine(
    medicine: str,
    quantity: Optional[Union[int, str]] = None,
    unit: Optional[str] = None,
    pieces_per_unit: Optional[int] = None,
    volume_ml: Optional[str] = None,
) -> dict:
    """
    Intelligently update an existing medicine in the current customer order.

    Use this when:
    - Customer explicitly asks to edit/modify their order (e.g. change quantity, unit, or ml size).
    - For tonics: set unit="tonic" and volume_ml="60ml"/"100ml"/etc.
    - If quantity is 0 or less, the medicine is removed from the order.
    """
    order = Order.load(get_current_customer())
    if order.is_packing_started():
        return {
            "success": False,
            "shipping_started": True,
            "message": "The shipping process is started so that you can't modify the order now.",
            "order": order.get_order(),
        }

    order.update_item(
        medicine=medicine,
        quantity=quantity,
        unit=unit,
        pieces_per_unit=pieces_per_unit,
        volume_ml=volume_ml,
    )
    return order.get_order()


def create_new_order() -> dict:
    """
    Start a fresh new order for the current customer when they want to order another product
    after a previous order was already confirmed.
    Safely archives the completed/confirmed order into data/orders/completed/ and retains the
    customer's name, phone, and delivery address so they don't have to re-enter them.
    """
    order = Order.load(get_current_customer())
    order.start_new_order()
    return {
        "success": True,
        "message": "Previous confirmed order safely archived. Ready to collect items for the new order.",
        "order": order.get_order(),
    }


def remove_medicine(
    medicine: str,
    unit: Optional[str] = None,
) -> dict:
    """
    Remove a specific medicine from the current customer order.
    """
    order = Order.load(get_current_customer())
    if order.is_packing_started():
        return {
            "success": False,
            "shipping_started": True,
            "message": "The shipping process is started so that you can't modify the order now.",
            "order": order.get_order(),
        }

    order.remove_item(
        medicine=medicine,
        unit=unit,
    )
    return order.get_order()


def cancel_order() -> dict:
    """
    Cancel the customer order and remove it from the portal.
    If the owner/admin has already checked the order and marked that packing/shipping
    has started, the order cannot be cancelled because the shipping process is started.
    """
    order = Order.load(get_current_customer())
    if order.is_packing_started():
        return {
            "success": False,
            "can_cancel": False,
            "shipping_started": True,
            "message": "The shipping process is started so that you can't cancel now.",
        }

    success = order.cancel_order()
    if success:
        return {
            "success": True,
            "can_cancel": True,
            "message": "Order has been cancelled and removed from the portal successfully.",
        }
    return {
        "success": False,
        "can_cancel": False,
        "message": "Unable to cancel the order or no active order found.",
    }


def update_customer_details(
    name: Optional[str] = None,
    phone: Optional[str] = None,
    address: Optional[str] = None,
) -> dict:
    """
    Update the customer information for the current order.

    IMPORTANT ADDRESS REQUIREMENTS:
    Sathya Agencies requires a complete, deliverable address containing:
    1. Door / Flat / House number and Street name
    2. Area / Locality
    3. City / Town
    4. 6-digit postal pincode (e.g. 600040)

    A single locality name (like 'Anna Nagar' or 'T Nagar') is incomplete because
    multiple cities share the same area names. If an address is incomplete or missing a pincode,
    the tool response will return 'address_status: incomplete'. You must continue asking the customer
    for their door number, street, city, and 6-digit pincode until the address is fully complete.
    """
    order = Order.load(get_current_customer())

    order.set_customer_details(
        name=name,
        phone=phone,
        address=address,
    )

    res = order.get_order()
    if not order.is_valid_address():
        res["address_status"] = "incomplete"
        res["address_warning"] = (
            "Address is incomplete. A complete delivery address with door/house number, street, city, "
            "and 6-digit postal PIN code is required. Please ask the customer to provide these missing details."
        )
    else:
        res["address_status"] = "complete"

    return res


def get_order() -> dict:
    """
    Get the current order details.
    """
    order = Order.load(get_current_customer())

    return order.get_order()


def get_missing_details() -> dict:
    """
    Get the required order information that is still missing.
    """
    order = Order.load(get_current_customer())

    return {
        "missing": order.get_missing_details(),
        "complete": order.is_complete(),
    }


def is_order_complete() -> dict:
    """
    Check whether all required order information has been completed.
    """
    order = Order.load(get_current_customer())
    return {
        "complete": order.is_complete(),
        "missing": order.get_missing_details(),
    }


def confirm_order() -> dict:
    """
    Confirm the current order.
    """
    order = Order.load(get_current_customer())

    success = order.confirm()

    if not success:
        return {
            "success": False,
            "message": "Order cannot be confirmed as required information is missing",
            "missing": order.get_missing_details(),
        }

    return {
        "success": True,
        "message": "Order confirmed successfully",
        "order": order.get_order(),
    }


def get_customer_order() -> dict:
    """
    Get the current customer order details.
    """
    return get_order()


def recall_customer_memory(query: str) -> dict:
    """
    Search semantic memory (pgvector) for the current customer to recall:
    - Previous orders / past medicines they purchased ("What did I order last time?", "Send the usual")
    - Special medical instructions, preferences, or doctor notes
    - Specific medicine dosages or brands used previously
    """
    cust = get_current_customer()
    order = Order.load(cust)
    phone = order.phone or cust

    matches = search_memories(phone_or_customer_id=phone, query=query, limit=4, threshold=0.40)
    if matches:
        return {
            "found": True,
            "count": len(matches),
            "memories": [
                {
                    "content": m["content"],
                    "type": m["memory_type"],
                    "date": m["created_at"],
                    "relevance": m["similarity"],
                }
                for m in matches
            ],
        }
    return {
        "found": False,
        "message": f"No past records or memories found matching '{query}' for this customer.",
    }


def save_customer_preference(preference: str, category: str = "preference") -> dict:
    """
    Save an explicit customer preference, special delivery instruction, or medical note
    into persistent memory (pgvector).
    Examples:
    - "Customer is diabetic, only dispense sugar-free syrups"
    - "Customer prefers morning delivery between 9 AM and 11 AM"
    - "Customer allergic to sulfa drugs"
    - "Call before arriving at the gate"
    """
    cust = get_current_customer()
    order = Order.load(cust)
    phone = order.phone or cust

    mem_id = store_memory(
        phone_or_customer_id=phone,
        content=preference.strip(),
        memory_type=category,
        customer_id=cust,
    )
    return {
        "success": bool(mem_id),
        "message": "Customer preference saved to persistent memory.",
        "preference": preference.strip(),
    }

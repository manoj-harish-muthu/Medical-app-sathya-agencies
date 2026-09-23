import os
import json
from services.memory_db import (
    normalize_phone,
    get_customer_profile,
    save_customer_profile,
    store_memory,
)

class Order:
    def __init__(self, customer_id = "current_customer"):
        self.customer_id = customer_id
        self.customer_name = None
        # Normalize phone number from customer_id (e.g. 917708035227 -> 7708035227)
        self.phone = normalize_phone(customer_id) or None
        self.address = None
        self.items = []
        self.confirmed = False
        self.is_checked_by_admin = False
        self.admin_status = "pending"

        # Check PostgreSQL memory for returning customer profile (Tier 1 Memory)
        try:
            profile = get_customer_profile(self.phone or self.customer_id)
            if profile:
                if profile.get("name") and not self.customer_name:
                    self.customer_name = profile["name"]
                if profile.get("address") and not self.address:
                    self.address = profile["address"]
                if profile.get("phone") and not self.phone:
                    self.phone = profile["phone"]
        except Exception:
            pass

    def is_valid_address(self) -> bool:
        """
        Check if the address is a complete, deliverable address.
        Requires:
        - Non-empty address string
        - Contains a valid 6-digit Indian postal pincode (e.g. 600040)
        - Contains sufficient street/area/city descriptive text (at least 10 non-pincode chars)
        """
        if not self.address or not isinstance(self.address, str):
            return False
        clean = self.address.strip()
        import re
        pincode_match = re.search(r'\b[1-9][0-9]{5}\b', clean)
        if not pincode_match:
            return False
        # Ensure there is descriptive address content beyond just the pincode
        text_without_pincode = re.sub(r'\b[1-9][0-9]{5}\b', '', clean).strip()
        return len(text_without_pincode) >= 10

    def set_customer_details(self, name=None, phone=None, address=None):
        if name:
            self.customer_name = name

        if phone:
            self.phone = normalize_phone(phone) or phone

        if address:
            import re
            new_addr = address.strip()
            # If an address already exists and customer provides supplementary details (e.g. just city/pincode)
            if self.address:
                existing_lower = self.address.strip().lower()
                new_lower = new_addr.lower()
                # If new address is already comprehensive, use it
                if existing_lower in new_lower:
                    self.address = new_addr
                elif new_lower in existing_lower:
                    pass  # existing address already contains this info
                elif len(new_addr) < 25 or re.match(r'^(pincode\s*[:=]?\s*)?[1-9][0-9]{5}$', new_lower):
                    # Supplementary addition (e.g. pincode or city added to earlier area)
                    self.address = f"{self.address.strip()}, {new_addr}"
                else:
                    self.address = new_addr
            else:
                self.address = new_addr

        # Persist to local JSON cache
        self.save()

        # Persist customer profile to PostgreSQL (Tier 1 Memory)
        try:
            save_customer_profile(
                phone_or_customer_id=self.phone or self.customer_id,
                name=self.customer_name,
                address=self.address,
                customer_id=self.customer_id,
            )
        except Exception:
            pass


    def archive_confirmed_order(self):
        """
        If current order is confirmed, archive it into data/orders/completed/
        so it is preserved and not overwritten.
        """
        if not self.confirmed or not self.items:
            return None
        import datetime
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        os.makedirs("data/orders/completed", exist_ok=True)
        archive_path = f"data/orders/completed/{self.customer_id}_{timestamp}.json"
        order_data = self.get_order()
        order_data["completed_at"] = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        with open(archive_path, "w", encoding="utf-8") as f:
            json.dump(order_data, f, indent=4, ensure_ascii=False)
        return archive_path

    def start_new_order(self):
        """
        Archive previous confirmed order and start a fresh new order.
        Preserves customer name, phone, and address.
        """
        if self.confirmed and self.items:
            self.archive_confirmed_order()
        self.items = []
        self.confirmed = False
        self.is_checked_by_admin = False
        self.admin_status = "pending"
        self.save()
        return self.get_order()

    def add_item(
        self,
        medicine: str,
        quantity: int | str = 1,
        unit: str = "strip",
        pieces_per_unit: int = None,
        volume_ml: str = None,
    ):
        # Prevent 0 or negative quantities
        if quantity is not None:
            if isinstance(quantity, (int, float)) and quantity <= 0:
                return
            if isinstance(quantity, str):
                import re
                nums = re.findall(r'\d+', quantity)
                if nums and int(nums[0]) <= 0:
                    return

        # If previous order was already confirmed, start a new order so old order is not modified
        if self.confirmed:
            self.archive_confirmed_order()
            self.items = []
            self.confirmed = False
            self.is_checked_by_admin = False
            self.admin_status = "pending"

        import re
        med_lower = medicine.strip().lower()
        is_tonic = any(
            k in med_lower
            for k in [
                "tonic", "syrup", "டானிக்", "சிரப்", "suspension", "liquid",
                "benadryl", "ascoril", "zedex", "grilinctus", "calpol", "t-98", "corex",
                "crocin", "paracip", "maxtra", "sinarest"
            ]
        ) or (unit and str(unit).strip().lower() in ["tonic", "tonics", "syrup", "syrups", "bottle", "bottles", "suspension", "liquid"]) or bool(re.search(r'\b\d+\s*ml\b', med_lower)) or bool(volume_ml)

        norm_unit = unit.strip().lower() if unit else ("tonic" if is_tonic else "strip")
        if norm_unit in ["tonic", "tonics", "syrup", "syrups", "bottle", "bottles", "suspension", "liquid"]:
            norm_unit = "tonic"
        elif norm_unit in ["tablet", "tablets", "tab", "tabs", "pill", "pills"]:
            norm_unit = "tablet"
        elif norm_unit in ["strip", "strips"]:
            norm_unit = "strip"
        elif norm_unit in ["piece", "pieces", "pc", "pcs"]:
            norm_unit = "piece"
        elif norm_unit in ["capsule", "capsules", "cap", "caps"]:
            norm_unit = "capsule"
        elif norm_unit in ["box", "boxes", "pack", "packs"]:
            norm_unit = "box"

        # If it's a tonic medicine, ensure unit is tonic, never strip or tablet
        if is_tonic:
            norm_unit = "tonic"

        # Extract and normalize volume_ml (e.g. 60, "100", "100ml" -> "100ml")
        norm_volume = None
        if volume_ml:
            v_str = str(volume_ml).strip().lower()
            norm_volume = f"{v_str}ml" if v_str.isdigit() else v_str
        else:
            m = re.search(r'\b(\d{2,4})\s*ml\b', med_lower)
            if m:
                norm_volume = f"{m.group(1)}ml"
            elif quantity and isinstance(quantity, str):
                mq = re.search(r'\b(\d{2,4})\s*ml\b', quantity.lower())
                if mq:
                    norm_volume = f"{mq.group(1)}ml"
            elif is_tonic and isinstance(quantity, (int, str)) and str(quantity).strip() in ["50", "60", "100", "120", "150", "200", "250", "450", "500"]:
                norm_volume = f"{str(quantity).strip()}ml"

        # Format quantity representation for tonics
        disp_quantity = quantity
        if norm_unit == "tonic" and norm_volume:
            q_str = str(quantity).strip() if quantity is not None else "1"
            if q_str in ["1", "None", "", norm_volume]:
                disp_quantity = norm_volume
            elif q_str.isdigit() and int(q_str) > 1:
                disp_quantity = f"{q_str} x {norm_volume}"
            elif norm_volume not in q_str:
                disp_quantity = f"{q_str} ({norm_volume})"

        # Check if item with same medicine already exists
        for item in self.items:
            if item["medicine"].strip().lower() == med_lower:
                # If same unit, update quantity
                if item.get("unit", "").strip().lower() == norm_unit:
                    item["quantity"] = disp_quantity
                    if pieces_per_unit is not None:
                        item["pieces_per_unit"] = pieces_per_unit
                    if norm_volume:
                        item["volume_ml"] = norm_volume
                else:
                    item["unit"] = norm_unit
                    item["quantity"] = disp_quantity
                    item["pieces_per_unit"] = pieces_per_unit if norm_unit not in ["tablet", "piece", "capsule", "pill", "tonic"] else None
                    if norm_volume:
                        item["volume_ml"] = norm_volume
                self.confirmed = False
                self.save()
                return

        item = {
            "medicine": medicine,
            "quantity": disp_quantity,
            "unit": norm_unit,
            "volume_ml": norm_volume,
            "pieces_per_unit": pieces_per_unit if norm_unit not in ["tablet", "piece", "capsule", "pill", "tonic"] else None
        }

        self.items.append(item)
        self.confirmed = False
        self.save()

    def update_item(
        self,
        medicine: str,
        quantity: int | str = None,
        unit: str = None,
        pieces_per_unit: int = None,
        volume_ml: str = None,
    ):
        """
        Intelligently update an existing medicine in the order:
        - If quantity <= 0: removes the medicine from the order.
        - Normalizes units (e.g. 'tablets' -> 'tablet', 'pieces' -> 'piece', 'syrup' -> 'tonic').
        - Ensures tonics always have unit='tonic' and updates volume_ml if specified.
        - If quantity is None: keeps the existing quantity and only updates unit / volume_ml / pieces_per_unit.
        """
        if quantity is not None:
            if isinstance(quantity, (int, float)) and quantity <= 0:
                return self.remove_item(medicine, unit)
            if isinstance(quantity, str):
                import re
                nums = re.findall(r'\d+', quantity)
                if nums and int(nums[0]) <= 0:
                    return self.remove_item(medicine, unit)

        import re
        med_lower = medicine.strip().lower()
        is_tonic = any(
            k in med_lower
            for k in [
                "tonic", "syrup", "டானிக்", "சிரப்", "suspension", "liquid",
                "benadryl", "ascoril", "zedex", "grilinctus", "calpol", "t-98", "corex",
                "crocin", "paracip", "maxtra", "sinarest"
            ]
        ) or (unit and str(unit).strip().lower() in ["tonic", "tonics", "syrup", "syrups", "bottle", "bottles", "suspension", "liquid"]) or bool(re.search(r'\b\d+\s*ml\b', med_lower)) or bool(volume_ml)

        # Normalize unit if provided
        norm_unit = None
        if unit:
            u = unit.strip().lower()
            if u in ["strip", "strips"]:
                norm_unit = "strip"
            elif u in ["tablet", "tablets", "tab", "tabs", "pill", "pills"]:
                norm_unit = "tablet"
            elif u in ["piece", "pieces", "pc", "pcs"]:
                norm_unit = "piece"
            elif u in ["capsule", "capsules", "cap", "caps"]:
                norm_unit = "capsule"
            elif u in ["bottle", "bottles", "tonic", "tonics", "syrup", "syrups"]:
                norm_unit = "tonic"
            elif u in ["box", "boxes", "pack", "packs"]:
                norm_unit = "box"
            else:
                norm_unit = u

        if is_tonic:
            norm_unit = "tonic"

        norm_volume = None
        if volume_ml:
            v_str = str(volume_ml).strip().lower()
            norm_volume = f"{v_str}ml" if v_str.isdigit() else v_str
        else:
            m = re.search(r'\b(\d{2,4})\s*ml\b', med_lower)
            if m:
                norm_volume = f"{m.group(1)}ml"
            elif quantity and isinstance(quantity, str):
                mq = re.search(r'\b(\d{2,4})\s*ml\b', quantity.lower())
                if mq:
                    norm_volume = f"{mq.group(1)}ml"
            elif is_tonic and isinstance(quantity, (int, str)) and str(quantity).strip() in ["50", "60", "100", "120", "150", "200", "250", "450", "500"]:
                norm_volume = f"{str(quantity).strip()}ml"

        matching_indices = [
            i for i, item in enumerate(self.items)
            if item["medicine"].strip().lower() == med_lower
        ]

        if matching_indices:
            target_idx = matching_indices[0]
            item = self.items[target_idx]

            if norm_unit is not None:
                item["unit"] = norm_unit

            effective_unit = item.get("unit", norm_unit)
            if is_tonic or effective_unit == "tonic":
                item["unit"] = "tonic"
                effective_unit = "tonic"

            if norm_volume:
                item["volume_ml"] = norm_volume

            eff_vol = item.get("volume_ml") or norm_volume

            if quantity is not None:
                if effective_unit == "tonic" and eff_vol:
                    q_str = str(quantity).strip()
                    if q_str in ["1", "None", "", eff_vol]:
                        item["quantity"] = eff_vol
                    elif q_str.isdigit() and int(q_str) > 1:
                        item["quantity"] = f"{q_str} x {eff_vol}"
                    elif eff_vol not in q_str:
                        item["quantity"] = f"{q_str} ({eff_vol})"
                    else:
                        item["quantity"] = quantity
                else:
                    item["quantity"] = quantity
            elif norm_volume and effective_unit == "tonic":
                # Quantity was not provided, but volume was updated
                curr_q = str(item.get("quantity", "1")).strip()
                if curr_q in ["1", "None", "", norm_volume]:
                    item["quantity"] = norm_volume
                elif curr_q.isdigit() and int(curr_q) > 1:
                    item["quantity"] = f"{curr_q} x {norm_volume}"
                else:
                    item["quantity"] = norm_volume

            is_single_unit = effective_unit in ["tablet", "piece", "capsule", "pill", "tonic"]
            if is_single_unit and pieces_per_unit is None:
                item["pieces_per_unit"] = None
            elif pieces_per_unit is not None:
                item["pieces_per_unit"] = pieces_per_unit

            # Clean duplicate entries
            if len(matching_indices) > 1:
                self.items = [
                    it for i, it in enumerate(self.items)
                    if i == target_idx or i not in matching_indices
                ]

            self.confirmed = False
            self.save()
            return True

        # If not found, add as a new item
        qty = quantity if quantity is not None else 1
        u = norm_unit or ("tonic" if is_tonic else "strip")
        ppu = pieces_per_unit if not (u in ["tablet", "piece", "capsule", "pill", "tonic"]) else None
        return self.add_item(medicine, qty, u, ppu, norm_volume)

    def remove_item(self, medicine: str, unit: str = None):
        """Remove a medicine from the order."""
        med_lower = medicine.strip().lower()
        if unit:
            unit_lower = unit.strip().lower()
            self.items = [
                item for item in self.items
                if not (item["medicine"].strip().lower() == med_lower and item.get("unit", "").strip().lower() == unit_lower)
            ]
        else:
            self.items = [
                item for item in self.items
                if item["medicine"].strip().lower() != med_lower
            ]
        self.confirmed = False
        self.save()
        return True

    def is_packing_started(self) -> bool:
        """
        Check whether the owner/admin has verified the order and started the packing/shipping process.
        Returns True if:
        - admin_status is 'packing_started', 'shipping_started', or 'shipped'
        - packing_started is True
        - is_checked_by_admin is True
        """
        if getattr(self, "packing_started", False):
            return True
        status = str(getattr(self, "admin_status", "")).strip().lower()
        if status in ["packing_started", "packing started", "shipping_started", "shipping started", "shipped", "packed"]:
            return True
        if getattr(self, "is_checked_by_admin", False):
            return True
        return False

    def cancel_order(self):
        """Cancel and delete the order file, resetting state if packing has not started."""
        if self.is_packing_started():
            return False

        file_path = f"data/orders/{self.customer_id}.json"
        if os.path.exists(file_path):
            os.remove(file_path)

        if self.phone:
            phone_file = f"data/orders/{self.phone}.json"
            if os.path.exists(phone_file):
                os.remove(phone_file)

        self.customer_name = None
        self.phone = None
        self.address = None
        self.items = []
        self.confirmed = False
        self.is_checked_by_admin = False
        self.admin_status = "pending"
        return True


    def get_missing_details(self):
        missing = []

        if not self.customer_name:
            missing.append("customer_name")

        if not self.phone:
            missing.append("phone")

        if not self.address:
            missing.append("address")
        elif not self.is_valid_address():
            missing.append("full_address_with_pincode")

        if not self.items:
            missing.append("items")
        else:
            for item in self.items:
                if item.get("unit") == "tonic" and not item.get("volume_ml"):
                    missing.append(f"volume_ml_for_{item.get('medicine')}")

        return missing
        
    def is_complete(self):
        return len(self.get_missing_details())==0
    
    def confirm(self):
        if not self.is_complete():
            return False
        
        self.confirmed=True
        self.save()

        # Tier 1: Ensure customer profile (name, phone, address) is synced to PostgreSQL customers table
        try:
            save_customer_profile(
                phone_or_customer_id=self.phone or self.customer_id,
                name=self.customer_name,
                address=self.address,
                customer_id=self.customer_id,
            )
        except Exception:
            pass

        # Tier 2: Save confirmed order memory with pgvector embedding
        try:
            desc_list = []
            for it in self.items:
                v = f", {it['volume_ml']}" if it.get("volume_ml") else ""
                desc_list.append(f"{it.get('medicine')} ({it.get('quantity')} {it.get('unit', '')}{v})".strip())
            items_summary = ", ".join(desc_list)
            mem_text = f"Confirmed Order: {items_summary}. Delivery Address: {self.address}."
            store_memory(
                phone_or_customer_id=self.phone or self.customer_id,
                content=mem_text,
                memory_type="order_history",
                metadata={"items": self.items, "address": self.address, "customer_name": self.customer_name},
                customer_id=self.customer_id,
            )
        except Exception:
            pass

        return True

    def get_order(self):
        return {
            "customer_id": self.customer_id,
            "customer_name": self.customer_name,
            "phone": self.phone,
            "address": self.address,
            "items": self.items,
            "confirmed": self.confirmed,
            "is_checked_by_admin": self.is_checked_by_admin,
            "admin_status": self.admin_status,
        }

    def change_customer_id(self,new_customer_id):
        old_file =f"data/orders/{self.customer_id}.json"

        self.customer_id=new_customer_id

        if os.path.exists(old_file):
            os.remove(old_file)

        self.save()

    
    def save(self):
        os.makedirs("data/orders",exist_ok=True)
        file_path = f"data/orders/{self.customer_id}.json"
        with open(file_path,"w",encoding="utf-8")as file:
            json.dump(
                self.get_order(),
                file,
                indent=4,
                ensure_ascii=False
            )
    
    @classmethod

    def load(cls,customer_id):
        file_path =f"data/orders/{customer_id}.json"

        if not os.path.exists(file_path):
            return cls(customer_id)

        with open(file_path,"r",encoding="utf-8")as file:
            data = json.load(file)
        
        order = cls(customer_id)

        order.customer_name = data.get("customer_name") or order.customer_name
        # Load saved phone or fallback to pre-seeded 10-digit WhatsApp phone
        order.phone = data.get("phone") or order.phone
        order.address = data.get("address") or order.address
        order.items = data.get("items", [])
        order.confirmed = data.get("confirmed", False)
        order.is_checked_by_admin = data.get("is_checked_by_admin", False)
        order.admin_status = data.get("admin_status", "pending")
        order.packing_started = data.get("packing_started", False)

        # Fallback to PostgreSQL profile if address or name missing
        if not order.address or not order.customer_name:
            try:
                prof = get_customer_profile(order.phone or customer_id)
                if prof:
                    if not order.customer_name and prof.get("name"):
                        order.customer_name = prof["name"]
                    if not order.address and prof.get("address"):
                        order.address = prof["address"]
            except Exception:
                pass

        return order


    
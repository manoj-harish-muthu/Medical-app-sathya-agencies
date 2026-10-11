"""
Catalog Service for Sathya Agencies Medical Agent.
Extracts and manages clean medicine names and inventory data from REPORT_1.csv.
Provides in-memory caching and formatted catalog blocks for agent system prompts.
"""

import os
import csv
import re
from pathlib import Path
from collections import defaultdict
from typing import List, Dict, Optional

# Cache variables
_MEDICINES: Optional[List[str]] = None
_INVENTORY: Optional[Dict[str, dict]] = None
_VOICE_CATALOG_PROMPT: Optional[str] = None


def find_csv_path() -> Path:
    """Locate REPORT_1.csv relative to this file or current working directory."""
    candidates = [
        # Medical-app-sathya-agencies/REPORT_1.csv (from services/ directory)
        Path(__file__).resolve().parent.parent.parent / "REPORT_1.csv",
        # medical-agent/REPORT_1.csv
        Path(__file__).resolve().parent.parent / "REPORT_1.csv",
        # Current working directory / REPORT_1.csv
        Path.cwd() / "REPORT_1.csv",
        Path.cwd().parent / "REPORT_1.csv",
    ]
    for p in candidates:
        if p.exists() and p.is_file():
            return p
    raise FileNotFoundError("Could not locate REPORT_1.csv in workspace.")


def clean_medicine_name(raw_desc: str) -> str:
    """
    Cleans raw ERP description into a clean medicine name.
    Example:
        'ACILOC 300MG TAB              20\\'S' -> 'ACILOC 300MG TAB'
        'ACECBRONIC 100MG CAP          10\\'S' -> 'ACECBRONIC 100MG CAP'
        'ALEX+ SYP 100ML               1\\'S'  -> 'ALEX+ SYP 100ML'
    """
    # Normalize internal whitespace
    clean = re.sub(r"\s+", " ", raw_desc.strip())
    # Remove trailing pack quantity indicator (e.g. 10'S, 1'S, 10S, 4S, 15S, 30'S, 1S)
    clean = re.sub(r"\s+\d+[\']?[Ss]$", "", clean)
    clean = re.sub(r"\s+1S$", "", clean, flags=re.IGNORECASE)
    return clean.strip()


def load_catalog(force_reload: bool = False):
    """Parses REPORT_1.csv and caches medicine names and inventory items."""
    global _MEDICINES, _INVENTORY, _VOICE_CATALOG_PROMPT

    if _MEDICINES is not None and not force_reload:
        return

    csv_path = find_csv_path()
    medicines_set = set()
    inventory_map = {}

    with open(csv_path, "r", encoding="utf-8", errors="ignore") as f:
        reader = csv.reader(f)
        for row in reader:
            # Valid item rows have a numeric S.No. in first column
            if len(row) >= 2 and row[0].strip().isdigit():
                raw_desc = row[1].strip()
                clean_name = clean_medicine_name(raw_desc)
                if not clean_name:
                    continue

                stock = row[2].strip() if len(row) > 2 else "0"
                unit = row[3].strip() if len(row) > 3 else ""

                medicines_set.add(clean_name)
                # Store by normalized lower-case key for case-insensitive lookup
                inventory_map[clean_name.lower()] = {
                    "name": clean_name,
                    "raw": raw_desc,
                    "stock": stock,
                    "unit": unit,
                }

    _MEDICINES = sorted(medicines_set)
    _INVENTORY = inventory_map

    # Build compact alphabetized catalog text for Voice Prompt ASR priming
    grouped = defaultdict(list)
    for med in _MEDICINES:
        letter = med[0].upper() if med[0].isalpha() else "#"
        grouped[letter].append(med)

    lines = []
    for letter in sorted(grouped.keys()):
        lines.append(f"[{letter}]: " + ", ".join(grouped[letter]))

    _VOICE_CATALOG_PROMPT = "\n".join(lines)


def get_medicine_names() -> List[str]:
    """Returns sorted list of all unique cleaned medicine names."""
    if _MEDICINES is None:
        load_catalog()
    return _MEDICINES or []


def get_voice_prompt_catalog_text() -> str:
    """Returns alphabetized compact medicine catalog text to inject into Gemini Live system prompt."""
    if _VOICE_CATALOG_PROMPT is None:
        load_catalog()
    return _VOICE_CATALOG_PROMPT or ""


def get_medicine_stock(query: str) -> Optional[dict]:
    """
    Looks up stock details for a medicine query.
    Returns dict with name, stock count, and unit, or None if not found.
    """
    if _INVENTORY is None:
        load_catalog()

    q_lower = query.strip().lower()
    # Exact match
    if q_lower in _INVENTORY:
        return _INVENTORY[q_lower]

    # Partial / substring match
    for key, item in _INVENTORY.items():
        if q_lower in key or key in q_lower:
            return item

    return None


def search_medicines(query: str, limit: int = 5) -> List[dict]:
    """Search for medicines matching query string."""
    if _INVENTORY is None:
        load_catalog()

    q_lower = query.strip().lower()
    matches = []
    for key, item in _INVENTORY.items():
        if q_lower in key:
            matches.append(item)
            if len(matches) >= limit:
                break
    return matches


if __name__ == "__main__":
    load_catalog()
    names = get_medicine_names()
    print(f"Loaded {len(names)} unique medicines from REPORT_1.csv")
    print(f"Sample 5: {names[:5]}")
    sample_stock = get_medicine_stock("ACILOC 300MG TAB")
    print(f"Stock test: {sample_stock}")

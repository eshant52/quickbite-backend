#!/usr/bin/env python3
"""
QuickBite Complete Demo Data Setup Script
=========================================
Seeds a local or deployed QuickBite backend with complete, realistic demo data
100% via QuickBite's official REST APIs (idempotent — safe to re-run):

  1. Super Admin authentication (`super.admin@quickbite.com`).
  2. 6 Delivery Agents (from `scripts/agents.json`):
     - Registration -> Personal KYC docs (`AADHAR`, `DRIVING_LICENSE`)
     - Vehicle onboarding (`VIN`, `Plate`, `RC`, `INSURANCE`)
     - Submission -> Admin allotment claim & approval
     - Marked `available = true` with live GPS coordinates in Nagpur.
  3. 6 Cuisines & 6 Restaurants (with 24x7 operating hours & 4 menu items each):
     - Haldiram's Planet Food - Dharampeth      (North Indian & Sweets)
     - La Pino'z Woodfired Pizzeria - Sitabuldi (Italian & Pizza)
     - Burger Barn & Grill - Sadar              (Burgers & American)
     - Brewberrys Artisanal Cafe - Civil Lines  (Cafe & Bakery)
     - Sip & Chill Beverage Lounge - Bajaj Nagar(Beverages & Shakes)
     - Dakshin Spice & Biryani House - Pratap N.(South Indian & Biryani)
  4. Demo Customer (`eshantsonune52@gmail.com`) with default Dharampeth address.
  5. Writes all resolved UUIDs to `scripts/demo_ids.json` for instant copy-paste testing.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional

import requests

# ── ANSI Styling ──────────────────────────────────────────────────────────────
RESET = "\033[0m"
BOLD = "\033[1m"
DIM = "\033[2m"
GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
CYAN = "\033[96m"
MAGENTA = "\033[95m"

USER_AGENT = "QuickBite-DemoSeeder/1.0 (Android 14; Mobile)"
DEFAULT_PASSWORD = "Password@123"

ALL_DAYS = [
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY",
    "SATURDAY",
    "SUNDAY",
]

RESTAURANT_DOCS = ["FSSAI_LICENSE", "GST", "PAN", "BANK_PROOF"]
AGENT_PERSONAL_DOCS = ["AADHAR", "DRIVING_LICENSE"]
VEHICLE_DOCS = ["RC", "INSURANCE"]


# ── Demo Catalog Definitions (6 Restaurants across 6 Cuisines) ────────────────

DEMO_RESTAURANTS: List[Dict[str, Any]] = [
    {
        "owner": {
            "name": "Rakesh Agarwal",
            "email": "rakesh.agarwal@haldiramsnagpur.com",
            "phoneNumber": "+919820000001",
            "password": DEFAULT_PASSWORD,
        },
        "cuisine": "North Indian & Sweets",
        "restaurant": {
            "name": "Haldiram's Planet Food - Dharampeth",
            "description": "Authentic North Indian thalis, signature street chaat, and traditional Indian sweets.",
            "houseNumber": "Plot 12",
            "buildingName": "Haldiram Tower",
            "street": "West High Court Road, Dharampeth",
            "landmark": "Near Law College Square",
            "city": "Nagpur",
            "state": "Maharashtra",
            "country": "India",
            "postalCode": "440010",
            "latitude": 21.1425,
            "longitude": 79.0615,
            "imageUrl": "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=800",
        },
        "menuItems": [
            {
                "name": "Shahi Gulab Jamun (2 Pcs)",
                "description": "Warm khoya dumplings soaked in saffron and green cardamom rose syrup.",
                "price": 75.00,
                "category": "Desserts & Sweets",
            },
            {
                "name": "Kaju Katli (250g)",
                "description": "Classic diamond-cut cashew fudge topped with edible silver leaf.",
                "price": 260.00,
                "category": "Desserts & Sweets",
            },
            {
                "name": "Royal Raj Kachori",
                "description": "Crispy giant kachori filled with sprouts, dahi, chutneys, and sev.",
                "price": 140.00,
                "category": "Chaat",
            },
            {
                "name": "Pindi Chole Bhature",
                "description": "Spiced chickpea curry served with two fluffy leavened bhaturas and pickle.",
                "price": 165.00,
                "category": "Main Course",
            },
        ],
    },
    {
        "owner": {
            "name": "Marco Rossi",
            "email": "marco.rossi@lapinoznagpur.com",
            "phoneNumber": "+919820000002",
            "password": DEFAULT_PASSWORD,
        },
        "cuisine": "Italian & Pizza",
        "restaurant": {
            "name": "La Pino'z Woodfired Pizzeria - Sitabuldi",
            "description": "Hand-stretched artisan woodfired pizzas, garlic breadsticks, and molten desserts.",
            "houseNumber": "Shop 4",
            "buildingName": "Eternity Mall Plaza",
            "street": "Variety Square, Sitabuldi",
            "landmark": "Opposite Sitabuldi Metro Station",
            "city": "Nagpur",
            "state": "Maharashtra",
            "country": "India",
            "postalCode": "440012",
            "latitude": 21.1432,
            "longitude": 79.0805,
            "imageUrl": "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800",
        },
        "menuItems": [
            {
                "name": "Margherita Woodfired Pizza (Medium)",
                "description": "San Marzano tomato sauce, fresh buffalo mozzarella, and sweet basil leaves.",
                "price": 249.00,
                "category": "Pizzas",
            },
            {
                "name": "Farmhouse Veggie Supreme Pizza",
                "description": "Loaded with bell peppers, olives, sweet corn, mushrooms, and jalapenos.",
                "price": 349.00,
                "category": "Pizzas",
            },
            {
                "name": "Cheesy Garlic Breadsticks",
                "description": "Freshly baked herb-butter breadsticks stuffed with molten mozzarella.",
                "price": 139.00,
                "category": "Sides",
            },
            {
                "name": "Warm Belgian Choco Lava Cake",
                "description": "Rich dark chocolate cake with a gooey molten chocolate center.",
                "price": 109.00,
                "category": "Desserts",
            },
        ],
    },
    {
        "owner": {
            "name": "Rohan Kapoor",
            "email": "rohan.kapoor@burgerbarn.com",
            "phoneNumber": "+919820000003",
            "password": DEFAULT_PASSWORD,
        },
        "cuisine": "Burgers & American",
        "restaurant": {
            "name": "Burger Barn & Grill - Sadar",
            "description": "Gourmet flame-grilled smash burgers, loaded peri-peri fries, and crispy sides.",
            "houseNumber": "22B",
            "buildingName": "Mount Road Arcade",
            "street": "Residency Road, Sadar",
            "landmark": "Near VCA Stadium",
            "city": "Nagpur",
            "state": "Maharashtra",
            "country": "India",
            "postalCode": "440001",
            "latitude": 21.1585,
            "longitude": 79.0760,
            "imageUrl": "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=800",
        },
        "menuItems": [
            {
                "name": "Classic Crispy Veggie Burger",
                "description": "Golden herb-potato patty with cheddar slice, iceberg lettuce, and signature burger sauce.",
                "price": 149.00,
                "category": "Burgers",
            },
            {
                "name": "Double Peri-Peri Paneer Smash Burger",
                "description": "Char-grilled spicy paneer steak, caramelized onions, jalapenos, and chipotle mayo.",
                "price": 219.00,
                "category": "Burgers",
            },
            {
                "name": "Loaded Cheese & Jalapeno Crinkle Fries",
                "description": "Crispy crinkle-cut fries smothered in warm nacho cheese sauce and pickled jalapenos.",
                "price": 129.00,
                "category": "Fries & Sides",
            },
            {
                "name": "Beer-Battered Crunchy Onion Rings (8 Pcs)",
                "description": "Thick-cut white onion rings fried golden and served with smoky BBQ dip.",
                "price": 119.00,
                "category": "Fries & Sides",
            },
        ],
    },
    {
        "owner": {
            "name": "Ananya Deshmukh",
            "email": "ananya.deshmukh@brewberryscafe.com",
            "phoneNumber": "+919820000004",
            "password": DEFAULT_PASSWORD,
        },
        "cuisine": "Cafe & Bakery",
        "restaurant": {
            "name": "Brewberrys Artisanal Cafe - Civil Lines",
            "description": "Specialty single-origin coffees, sourdough sandwiches, and freshly baked Belgian waffles.",
            "houseNumber": "Bungalow 7",
            "buildingName": "Palm Grove Lane",
            "street": "Walkers Street, Civil Lines",
            "landmark": "Beside Chitnavis Centre",
            "city": "Nagpur",
            "state": "Maharashtra",
            "country": "India",
            "postalCode": "440001",
            "latitude": 21.1510,
            "longitude": 79.0670,
            "imageUrl": "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=800",
        },
        "menuItems": [
            {
                "name": "Hazelnut Cold Coffee Frappe",
                "description": "Double-shot Arabica espresso blended with roasted hazelnut syrup, milk, and vanilla ice cream.",
                "price": 175.00,
                "category": "Cold Coffee",
            },
            {
                "name": "Artisanal Classic Cappuccino",
                "description": "Rich espresso topped with velvety micro-foamed milk and cocoa dusting.",
                "price": 140.00,
                "category": "Hot Coffee",
            },
            {
                "name": "Grilled Spinach & Corn Sourdough Sandwich",
                "description": "Toasted multigrain sourdough stuffed with creamy spinach, sweet corn, and smoked gouda.",
                "price": 195.00,
                "category": "Sandwiches",
            },
            {
                "name": "Belgian Dark Chocolate Overload Waffle",
                "description": "Crispy Brussels waffle drizzled with 70% dark chocolate ganache and roasted almonds.",
                "price": 210.00,
                "category": "Waffles & Bakery",
            },
        ],
    },
    {
        "owner": {
            "name": "Kabir Mehta",
            "email": "kabir.mehta@sipandchill.com",
            "phoneNumber": "+919820000005",
            "password": DEFAULT_PASSWORD,
        },
        "cuisine": "Beverages & Shakes",
        "restaurant": {
            "name": "Sip & Chill Beverage Lounge - Bajaj Nagar",
            "description": "Thick artisanal milkshakes, refreshing fruit mocktails, bubble teas, and cold-pressed juices.",
            "houseNumber": "Shop 9",
            "buildingName": "Laxmi Sparkle Arcade",
            "street": "Central Bazaar Road, Bajaj Nagar",
            "landmark": "Near VNIT Main Gate",
            "city": "Nagpur",
            "state": "Maharashtra",
            "country": "India",
            "postalCode": "440010",
            "latitude": 21.1285,
            "longitude": 79.0545,
            "imageUrl": "https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=800",
        },
        "menuItems": [
            {
                "name": "Alphonso Mango Thick Shake",
                "description": "Ratnagiri Alphonso mango pulp blended thick with full-cream milk and mango chunks.",
                "price": 160.00,
                "category": "Thick Shakes",
            },
            {
                "name": "Oreo Belgian Fudge Freakshake",
                "description": "Crunchy Oreo cookies blended with Belgian chocolate ice cream and whipped cream.",
                "price": 185.00,
                "category": "Thick Shakes",
            },
            {
                "name": "Virgin Mint Mojito Sparkler",
                "description": "Muddled fresh mint leaves, lime wedges, organic cane sugar, and chilled club soda.",
                "price": 130.00,
                "category": "Mocktails & Coolers",
            },
            {
                "name": "Watermelon Basil Cold-Pressed Juice",
                "description": "100% pure cold-pressed watermelon infused with sweet basil and black salt (no added sugar).",
                "price": 145.00,
                "category": "Fresh Juices",
            },
        ],
    },
    {
        "owner": {
            "name": "Venkat Iyer",
            "email": "venkat.iyer@dakshinbiryani.com",
            "phoneNumber": "+919820000006",
            "password": DEFAULT_PASSWORD,
        },
        "cuisine": "South Indian & Biryani",
        "restaurant": {
            "name": "Dakshin Spice & Biryani House - Pratap Nagar",
            "description": "Crispy ghee roast dosas, fluffy thatte idlis, filter kaapi, and Hyderabadi dum biryanis.",
            "houseNumber": "Plot 44",
            "buildingName": "Sri Krishna Chambers",
            "street": "Ring Road, Pratap Nagar",
            "landmark": "Near Pratap Nagar Square",
            "city": "Nagpur",
            "state": "Maharashtra",
            "country": "India",
            "postalCode": "440022",
            "latitude": 21.1195,
            "longitude": 79.0480,
            "imageUrl": "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=800",
        },
        "menuItems": [
            {
                "name": "Mysore Ghee Masala Dosa",
                "description": "Crisp fermented crepe smeared with fiery Mysore garlic chutney, potato palya, and pure cow ghee.",
                "price": 135.00,
                "category": "South Indian",
            },
            {
                "name": "Hyderabadi Subz Dum Biryani",
                "description": "Long-grain aged basmati rice slow-cooked in a sealed handi with seasonal vegetables, saffron, and mirchi ka salan.",
                "price": 240.00,
                "category": "Biryani",
            },
            {
                "name": "Ghee Podi Thatte Idli (2 Pcs)",
                "description": "Steamed pillow-soft flat idlis generously coated in gunpowder podi and melted ghee.",
                "price": 110.00,
                "category": "South Indian",
            },
            {
                "name": "Traditional Kumbakonam Filter Kaapi",
                "description": "Strong chicory-blended South Indian decoction coffee frothed in a brass davarah-tumbler.",
                "price": 65.00,
                "category": "Beverages",
            },
        ],
    },
]

DEMO_CUSTOMER = {
    "name": "Eshant Sonune",
    "email": "eshantsonune52@gmail.com",
    "phoneNumber": "+919830000001",
    "password": DEFAULT_PASSWORD,
    "address": {
        "label": "Home - Dharampeth",
        "houseNumber": "Flat 302",
        "buildingName": "Shree Ganesh Residency",
        "street": "North Ambazari Road, Dharampeth",
        "landmark": "Near Shivaji Nagar Park",
        "city": "Nagpur",
        "state": "Maharashtra",
        "country": "India",
        "postalCode": "440010",
        "latitude": 21.1398,
        "longitude": 79.0635,
        "isDefault": True,
    },
}


# ── API Helper Client ─────────────────────────────────────────────────────────

class SeederClient:
    def __init__(self, base_url: str, timeout: float = 12.0):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.http = requests.Session()

    def _headers(self, token: Optional[str] = None) -> Dict[str, str]:
        h = {
            "X-Client-Type": "mobile_app",
            "User-Agent": USER_AGENT,
            "Content-Type": "application/json",
        }
        if token:
            h["Authorization"] = f"Bearer {token}"
        return h

    def request(
        self,
        method: str,
        path: str,
        token: Optional[str] = None,
        json_body: Optional[Any] = None,
    ) -> requests.Response:
        return self.http.request(
            method,
            f"{self.base_url}{path}",
            headers=self._headers(token),
            json=json_body,
            timeout=self.timeout,
        )

    def login(self, email: str, password: str) -> str:
        """Log in and return accessToken, evicting oldest session on 409."""
        resp = self.request("POST", "/api/v1/auth/login", json_body={"email": email, "password": password})
        if resp.status_code == 200:
            return resp.json()["accessToken"]

        if resp.status_code == 409:
            challenge_token = resp.json().get("challengeToken")
            if not challenge_token:
                raise RuntimeError(f"409 conflict without challengeToken for {email}: {resp.text}")
            sess_resp = self.request("GET", "/api/v1/auth/sessions", token=challenge_token)
            if sess_resp.status_code == 200:
                sessions = sess_resp.json() or []
                if sessions:
                    sessions.sort(key=lambda s: s.get("createdAt") or "")
                    oldest_id = sessions[0].get("id") or sessions[0].get("sessionId")
                    if oldest_id:
                        self.request("DELETE", f"/api/v1/auth/sessions/{oldest_id}", token=challenge_token)
            retry = self.request("POST", "/api/v1/auth/login", json_body={"email": email, "password": password})
            if retry.status_code == 200:
                return retry.json()["accessToken"]
            raise RuntimeError(f"Login retry failed for {email} ({retry.status_code}): {retry.text}")

        raise RuntimeError(f"Login failed for {email} (HTTP {resp.status_code}): {resp.text}")

    def register_or_login(self, role_path: str, name: str, email: str, phone: str, password: str) -> str:
        """Register user (customer / restaurant-owner / delivery-agent) if needed, then return accessToken."""
        reg_resp = self.request(
            "POST",
            f"/api/v1/auth/register/{role_path}",
            json_body={
                "name": name,
                "email": email,
                "phoneNumber": phone,
                "password": password,
            },
        )
        if reg_resp.status_code not in (200, 201, 409):
            raise RuntimeError(
                f"Registration failed for {email} on /{role_path} (HTTP {reg_resp.status_code}): {reg_resp.text}"
            )
        return self.login(email, password)

    def claim_pending_allotment_if_present(self, admin_token: str, reference_id: str) -> None:
        """Accept pending admin allotment for reference_id if one exists."""
        resp = self.request("GET", "/api/v1/admin/allotments/pending", token=admin_token)
        if resp.status_code != 200:
            return
        for allotment in resp.json() or []:
            if str(allotment.get("referenceId")) == str(reference_id):
                allotment_id = allotment.get("id")
                if allotment_id:
                    self.request("POST", f"/api/v1/admin/allotments/{allotment_id}/accept", token=admin_token)
                break


# ── Phase 2: Delivery Agent Onboarding ────────────────────────────────────────

def onboard_delivery_agents(
    client: SeederClient,
    admin_token: str,
    agents_file: Path,
) -> List[Dict[str, Any]]:
    raw_agents = json.loads(agents_file.read_text(encoding="utf-8"))
    seeded_agents: List[Dict[str, Any]] = []

    print(f"\n{BOLD}{CYAN}━━ Phase 2: Onboarding Delivery Agents ({len(raw_agents)} agents) ━━{RESET}")
    for idx, entry in enumerate(raw_agents, start=1):
        if not entry.get("enabled", True):
            continue
        email = entry["email"]
        password = entry.get("password", DEFAULT_PASSWORD)
        name = entry.get("name") or email.split("@")[0].replace(".", " ").title()
        phone = entry.get("phoneNumber") or f"+9198100000{idx:02d}"
        lat = float(entry.get("latitude", 21.1412 + idx * 0.002))
        lng = float(entry.get("longitude", 79.0625 + idx * 0.002))
        veh = entry.get("vehicle") or {
            "vinNumber": f"MA3EJKD1S001000{idx:02d}",
            "numberPlate": f"MH-31-AB-10{idx:02d}",
            "vehicleType": "BIKE",
            "brand": "Bajaj",
            "model": "Pulsar 150",
        }

        agent_token = client.register_or_login("delivery-agent", name, email, phone, password)

        # Check if already an approved delivery agent
        prof_resp = client.request("GET", "/api/v1/delivery-agent/profile", token=agent_token)
        if prof_resp.status_code == 200 and prof_resp.json().get("currentStatus") == "APPROVED":
            prof = prof_resp.json()
            print(f"  {GREEN}✓{RESET} [{idx}] {BOLD}{name}{RESET} ({email}) — already APPROVED")
        else:
            # Start or resume DeliveryAgentApplication
            start_resp = client.request("POST", "/api/v1/delivery-agent/applications/start", token=agent_token)
            if start_resp.status_code not in (200, 201):
                raise RuntimeError(f"Failed to start agent application for {email}: {start_resp.text}")
            app_data = start_resp.json()
            app_id = app_data["id"]
            app_status = app_data.get("status")

            if app_status == "REJECTED":
                reopen = client.request(
                    "POST", f"/api/v1/delivery-agent/applications/{app_id}/reopen", token=agent_token
                )
                if reopen.status_code == 200:
                    app_status = "DRAFT"

            if app_status == "DRAFT":
                # 1. Personal documents (AADHAR, DRIVING_LICENSE)
                for doc_type in AGENT_PERSONAL_DOCS:
                    doc_resp = client.request(
                        "POST",
                        f"/api/v1/delivery-agent/applications/{app_id}/documents",
                        token=agent_token,
                        json_body={
                            "type": doc_type,
                            "url": f"https://quickbite-demo.s3.ap-south-1.amazonaws.com/agents/{app_id}/{doc_type.lower()}.pdf",
                        },
                    )
                    if doc_resp.status_code not in (200, 201):
                        raise RuntimeError(f"Failed adding {doc_type} for {email}: {doc_resp.text}")

                # 2. Child Vehicle Application
                v_start = client.request(
                    "POST",
                    f"/api/v1/delivery-agent/applications/{app_id}/vehicles/start",
                    token=agent_token,
                )
                if v_start.status_code not in (200, 201):
                    raise RuntimeError(f"Failed starting vehicle app for {email}: {v_start.text}")
                v_app_id = v_start.json()["id"]

                # 3. Vehicle Details
                v_details = client.request(
                    "PATCH",
                    f"/api/v1/delivery-agent/vehicles/applications/{v_app_id}/details",
                    token=agent_token,
                    json_body={
                        "vinNumber": veh["vinNumber"],
                        "numberPlate": veh["numberPlate"],
                        "vehicleType": veh["vehicleType"],
                        "brand": veh["brand"],
                        "model": veh["model"],
                        "isOwnershipTransferred": True,
                    },
                )
                if v_details.status_code not in (200, 201):
                    raise RuntimeError(f"Failed saving vehicle details for {email}: {v_details.text}")

                # 4. Vehicle Documents (RC, INSURANCE)
                for v_doc_type in VEHICLE_DOCS:
                    vd_resp = client.request(
                        "POST",
                        f"/api/v1/delivery-agent/vehicles/applications/{v_app_id}/documents",
                        token=agent_token,
                        json_body={
                            "type": v_doc_type,
                            "url": f"https://quickbite-demo.s3.ap-south-1.amazonaws.com/vehicles/{v_app_id}/{v_doc_type.lower()}.pdf",
                        },
                    )
                    if vd_resp.status_code not in (200, 201):
                        raise RuntimeError(f"Failed saving vehicle doc {v_doc_type} for {email}: {vd_resp.text}")

                # 5. Submit onboarding application
                sub_resp = client.request(
                    "POST",
                    f"/api/v1/delivery-agent/applications/{app_id}/submit",
                    token=agent_token,
                )
                if sub_resp.status_code not in (200, 201):
                    raise RuntimeError(f"Failed submitting agent application for {email}: {sub_resp.text}")

            # 6. Admin claims allotment & approves application
            client.claim_pending_allotment_if_present(admin_token, app_id)
            app_approve = client.request(
                "POST",
                f"/api/v1/admin/delivery-agents/applications/{app_id}/approve",
                token=admin_token,
            )
            if app_approve.status_code not in (200, 204):
                raise RuntimeError(f"Admin approval failed for agent {email}: {app_approve.text}")

            prof_resp = client.request("GET", "/api/v1/delivery-agent/profile", token=agent_token)
            prof = prof_resp.json()
            print(f"  {GREEN}✓{RESET} [{idx}] {BOLD}{name}{RESET} ({email}) — onboarded & APPROVED")

        # Mark available = true and push GPS coordinates (for both PostGIS & Redis GEO)
        avail_resp = client.request(
            "PATCH",
            "/api/v1/delivery-agent/availability",
            token=agent_token,
            json_body={"available": True},
        )
        loc_resp = client.request(
            "PUT",
            "/api/v1/delivery-agent/location",
            token=agent_token,
            json_body={"latitude": lat, "longitude": lng},
        )
        if avail_resp.status_code == 200 and loc_resp.status_code == 200:
            print(
                f"      {DIM}↳ Duty: AVAILABLE | Vehicle: {veh['vehicleType']} ({veh['numberPlate']}) "
                f"| GPS: ({lat:.4f}, {lng:.4f}){RESET}"
            )

        seeded_agents.append(
            {
                "agentId": prof.get("id"),
                "userId": prof.get("userId"),
                "name": name,
                "email": email,
                "password": password,
                "vehicle": f"{veh['brand']} {veh['model']} ({veh['numberPlate']})",
                "latitude": lat,
                "longitude": lng,
            }
        )

    return seeded_agents


# ── Phase 3: Cuisines, Restaurants & Menu Items ───────────────────────────────

def ensure_cuisine_approved(
    client: SeederClient,
    owner_token: str,
    admin_token: str,
    cuisine_name: str,
) -> str:
    """Return the approved cuisine UUID for cuisine_name, creating & approving it if absent."""
    cat_resp = client.request("GET", "/api/v1/cuisines")
    if cat_resp.status_code == 200:
        for c in cat_resp.json() or []:
            if c.get("name", "").strip().lower() == cuisine_name.strip().lower():
                return c["id"]

    # Check if already pending in admin queue
    pend_resp = client.request("GET", "/api/v1/admin/cuisines?status=PENDING&size=100", token=admin_token)
    pending_id: Optional[str] = None
    if pend_resp.status_code == 200:
        items = pend_resp.json().get("items") or pend_resp.json().get("content") or []
        for c in items:
            if c.get("name", "").strip().lower() == cuisine_name.strip().lower():
                pending_id = c["id"]
                break

    if not pending_id:
        req_resp = client.request(
            "POST",
            "/api/v1/restaurant-owner/cuisines",
            token=owner_token,
            json_body={"name": cuisine_name},
        )
        if req_resp.status_code in (200, 201):
            pending_id = req_resp.json()["id"]
        else:
            # Re-check catalog in case of race
            cat_retry = client.request("GET", "/api/v1/cuisines")
            if cat_retry.status_code == 200:
                for c in cat_retry.json() or []:
                    if c.get("name", "").strip().lower() == cuisine_name.strip().lower():
                        return c["id"]
            raise RuntimeError(f"Failed requesting cuisine '{cuisine_name}': {req_resp.text}")

    client.claim_pending_allotment_if_present(admin_token, pending_id)
    app_resp = client.request("POST", f"/api/v1/admin/cuisines/{pending_id}/approve", token=admin_token)
    if app_resp.status_code in (200, 201):
        return app_resp.json()["id"]

    # Fallback lookup from public catalog
    cat_final = client.request("GET", "/api/v1/cuisines")
    for c in cat_final.json() or []:
        if c.get("name", "").strip().lower() == cuisine_name.strip().lower():
            return c["id"]

    raise RuntimeError(f"Could not approve cuisine '{cuisine_name}': {app_resp.text}")


def ensure_restaurant_onboarded(
    client: SeederClient,
    owner_token: str,
    admin_token: str,
    rest_cfg: Dict[str, Any],
) -> str:
    """Return approved restaurantId for the owner, running full onboarding if needed."""
    my_rests = client.request("GET", "/api/v1/restaurant-owner/restaurants?size=50", token=owner_token)
    if my_rests.status_code == 200:
        items = my_rests.json().get("items") or my_rests.json().get("content") or []
        for r in items:
            if r.get("name", "").strip().lower() == rest_cfg["name"].strip().lower():
                return r["id"]
        if items:
            return items[0]["id"]

    # Check if an active application already exists, else start one
    curr_resp = client.request("GET", "/api/v1/restaurant-owner/applications/current", token=owner_token)
    if curr_resp.status_code == 200:
        app_data = curr_resp.json()
    else:
        start_resp = client.request("POST", "/api/v1/restaurant-owner/applications/start", token=owner_token)
        if start_resp.status_code not in (200, 201):
            raise RuntimeError(f"Failed starting restaurant application for {rest_cfg['name']}: {start_resp.text}")
        app_data = start_resp.json()

    app_id = app_data["id"]
    app_status = app_data.get("status")

    if app_status == "DRAFT":
        # 1. Details
        det_resp = client.request(
            "PATCH",
            f"/api/v1/restaurant-owner/applications/{app_id}/details",
            token=owner_token,
            json_body={
                "name": rest_cfg["name"],
                "description": rest_cfg["description"],
            },
        )
        if det_resp.status_code != 200:
            raise RuntimeError(f"Failed saving restaurant details: {det_resp.text}")

        # 2. Address
        addr_resp = client.request(
            "PATCH",
            f"/api/v1/restaurant-owner/applications/{app_id}/address",
            token=owner_token,
            json_body={
                "houseNumber": rest_cfg["houseNumber"],
                "buildingName": rest_cfg["buildingName"],
                "street": rest_cfg["street"],
                "landmark": rest_cfg["landmark"],
                "city": rest_cfg["city"],
                "state": rest_cfg["state"],
                "country": rest_cfg["country"],
                "postalCode": rest_cfg["postalCode"],
                "latitude": rest_cfg["latitude"],
                "longitude": rest_cfg["longitude"],
            },
        )
        if addr_resp.status_code != 200:
            raise RuntimeError(f"Failed saving restaurant address: {addr_resp.text}")

        # 3. Operating Hours (All 7 days 00:00 -> 23:59 so demos work 24/7)
        hours_payload = {
            "hours": [
                {"dayOfWeek": day, "openTime": "00:00", "closeTime": "23:59"}
                for day in ALL_DAYS
            ]
        }
        hrs_resp = client.request(
            "POST",
            f"/api/v1/restaurant-owner/applications/{app_id}/hours",
            token=owner_token,
            json_body=hours_payload,
        )
        if hrs_resp.status_code not in (200, 201):
            raise RuntimeError(f"Failed saving restaurant hours: {hrs_resp.text}")

        # 4. Image
        img_resp = client.request(
            "POST",
            f"/api/v1/restaurant-owner/applications/{app_id}/images",
            token=owner_token,
            json_body={"imageUrl": rest_cfg["imageUrl"], "displayOrder": 0},
        )
        if img_resp.status_code not in (200, 201):
            raise RuntimeError(f"Failed adding restaurant image: {img_resp.text}")

        # 5. Mandatory Documents (FSSAI_LICENSE, GST, PAN, BANK_PROOF)
        for doc_type in RESTAURANT_DOCS:
            d_resp = client.request(
                "POST",
                f"/api/v1/restaurant-owner/applications/{app_id}/documents",
                token=owner_token,
                json_body={
                    "type": doc_type,
                    "url": f"https://quickbite-demo.s3.ap-south-1.amazonaws.com/restaurants/{app_id}/{doc_type.lower()}.pdf",
                },
            )
            if d_resp.status_code not in (200, 201):
                raise RuntimeError(f"Failed adding restaurant document {doc_type}: {d_resp.text}")

        # 6. Submit Application
        sub_resp = client.request(
            "POST",
            f"/api/v1/restaurant-owner/applications/{app_id}/submit",
            token=owner_token,
        )
        if sub_resp.status_code not in (200, 201):
            raise RuntimeError(f"Failed submitting restaurant application: {sub_resp.text}")

    # 7. Admin claims allotment & approves
    client.claim_pending_allotment_if_present(admin_token, app_id)
    approve_resp = client.request(
        "POST",
        f"/api/v1/admin/restaurants/applications/{app_id}/approve",
        token=admin_token,
    )
    if approve_resp.status_code not in (200, 204):
        raise RuntimeError(f"Admin approval failed for restaurant {rest_cfg['name']}: {approve_resp.text}")

    # Fetch newly created restaurantId
    my_rests_after = client.request("GET", "/api/v1/restaurant-owner/restaurants?size=50", token=owner_token)
    items_after = my_rests_after.json().get("items") or my_rests_after.json().get("content") or []
    for r in items_after:
        if r.get("name", "").strip().lower() == rest_cfg["name"].strip().lower():
            return r["id"]
    if items_after:
        return items_after[0]["id"]

    raise RuntimeError(f"Restaurant approved but could not be found for {rest_cfg['name']}")


def seed_restaurants_and_menus(
    client: SeederClient,
    admin_token: str,
) -> List[Dict[str, Any]]:
    print(f"\n{BOLD}{CYAN}━━ Phase 3: Onboarding 6 Restaurants, Cuisines & Menus ━━{RESET}")
    seeded_restaurants: List[Dict[str, Any]] = []

    for idx, entry in enumerate(DEMO_RESTAURANTS, start=1):
        owner = entry["owner"]
        cuisine_name = entry["cuisine"]
        rest_cfg = entry["restaurant"]

        owner_token = client.register_or_login(
            "restaurant-owner",
            owner["name"],
            owner["email"],
            owner["phoneNumber"],
            owner["password"],
        )

        cuisine_id = ensure_cuisine_approved(client, owner_token, admin_token, cuisine_name)
        restaurant_id = ensure_restaurant_onboarded(client, owner_token, admin_token, rest_cfg)

        # Fetch existing menu items so we don't duplicate on re-run
        existing_resp = client.request(
            "GET",
            f"/api/v1/restaurants/{restaurant_id}/menu-items?size=100&available=true",
        )
        existing_by_name: Dict[str, Dict[str, Any]] = {}
        if existing_resp.status_code == 200:
            items_list = existing_resp.json().get("items") or existing_resp.json().get("content") or []
            for mi in items_list:
                existing_by_name[mi.get("name", "").strip().lower()] = mi

        seeded_items: List[Dict[str, Any]] = []
        for item_cfg in entry["menuItems"]:
            key = item_cfg["name"].strip().lower()
            if key in existing_by_name:
                mi_data = existing_by_name[key]
            else:
                create_resp = client.request(
                    "POST",
                    f"/api/v1/restaurant-owner/restaurants/{restaurant_id}/menu-items",
                    token=owner_token,
                    json_body={
                        "name": item_cfg["name"],
                        "description": item_cfg["description"],
                        "cuisineId": cuisine_id,
                        "price": item_cfg["price"],
                        "category": item_cfg["category"],
                        "isAvailable": True,
                    },
                )
                if create_resp.status_code not in (200, 201):
                    raise RuntimeError(
                        f"Failed creating menu item '{item_cfg['name']}' for {rest_cfg['name']}: {create_resp.text}"
                    )
                mi_data = create_resp.json()

            seeded_items.append(
                {
                    "id": mi_data["id"],
                    "name": mi_data["name"],
                    "price": mi_data["price"],
                    "category": mi_data["category"],
                }
            )

        print(
            f"  {GREEN}✓{RESET} [{idx}] {BOLD}{rest_cfg['name']}{RESET} "
            f"{MAGENTA}[{cuisine_name}]{RESET} (id: {CYAN}{restaurant_id}{RESET})"
        )
        print(f"      {DIM}↳ Owner: {owner['email']} | Menu Items ({len(seeded_items)}):{RESET}")
        for si in seeded_items:
            print(f"        • {si['name']} — ₹{ float(si['price']):.2f} {DIM}[menuItemId: {si['id']}]{RESET}")

        seeded_restaurants.append(
            {
                "restaurantId": restaurant_id,
                "name": rest_cfg["name"],
                "cuisine": cuisine_name,
                "cuisineId": cuisine_id,
                "ownerEmail": owner["email"],
                "ownerPassword": owner["password"],
                "menuItems": seeded_items,
            }
        )

    return seeded_restaurants


# ── Phase 4: Customer & Default Delivery Address ──────────────────────────────

def seed_customer_and_address(client: SeederClient) -> Dict[str, Any]:
    print(f"\n{BOLD}{CYAN}━━ Phase 4: Setting Up Demo Customer & Delivery Address ━━{RESET}")
    cust = DEMO_CUSTOMER
    cust_token = client.register_or_login(
        "customer",
        cust["name"],
        cust["email"],
        cust["phoneNumber"],
        cust["password"],
    )

    addr_list_resp = client.request("GET", "/api/v1/customer/me/addresses", token=cust_token)
    address_id: Optional[str] = None
    if addr_list_resp.status_code == 200:
        addrs = addr_list_resp.json() or []
        if addrs:
            address_id = addrs[0]["id"]

    if not address_id:
        create_addr = client.request(
            "POST",
            "/api/v1/customer/me/addresses",
            token=cust_token,
            json_body=cust["address"],
        )
        if create_addr.status_code not in (200, 201):
            raise RuntimeError(f"Failed creating customer address: {create_addr.text}")
        address_id = create_addr.json()["id"]

    print(
        f"  {GREEN}✓{RESET} Customer: {BOLD}{cust['name']}{RESET} ({cust['email']}) "
        f"| Default addressId: {CYAN}{address_id}{RESET}"
    )
    return {
        "name": cust["name"],
        "email": cust["email"],
        "password": cust["password"],
        "addressId": address_id,
    }


# ── Main CLI Entrypoint ───────────────────────────────────────────────────────

def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    script_dir = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(
        description="Seed complete QuickBite demo data (6 agents, 6 restaurants, customer) via REST APIs."
    )
    parser.add_argument(
        "--base-url",
        default=os.environ.get("QUICKBITE_BASE_URL", "http://localhost:8080"),
        help="QuickBite backend base URL (default: http://localhost:8080)",
    )
    parser.add_argument(
        "--admin-email",
        default=os.environ.get("ADMIN_BOOTSTRAP_EMAIL", "super.admin@quickbite.com"),
        help="Super Admin email (default: super.admin@quickbite.com)",
    )
    parser.add_argument(
        "--admin-password",
        default=os.environ.get("ADMIN_BOOTSTRAP_PASSWORD", "Admin@123"),
        help="Super Admin password (default: Admin@123)",
    )
    parser.add_argument(
        "--agents-file",
        type=Path,
        default=script_dir / "agents.json",
        help="Path to agents.json (default: scripts/agents.json)",
    )
    parser.add_argument(
        "--output-file",
        type=Path,
        default=script_dir / "demo_ids.json",
        help="Path to write resolved demo UUIDs JSON (default: scripts/demo_ids.json)",
    )
    return parser.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    client = SeederClient(args.base_url)

    print(f"{BOLD}{CYAN}🚀 QuickBite Complete Demo Data Seeder (REST API){RESET}")
    print(f"{DIM}Target Backend: {args.base_url}{RESET}")

    print(f"\n{BOLD}{CYAN}━━ Phase 1: Authenticating Super Admin ━━{RESET}")
    try:
        admin_token = client.login(args.admin_email, args.admin_password)
        print(f"  {GREEN}✓{RESET} Logged in as Super Admin ({args.admin_email})")
    except Exception as exc:
        print(f"  {RED}✗ Failed Super Admin login ({args.admin_email}): {exc}{RESET}")
        return 1

    try:
        agents_summary = onboard_delivery_agents(client, admin_token, args.agents_file)
        restaurants_summary = seed_restaurants_and_menus(client, admin_token)
        customer_summary = seed_customer_and_address(client)
    except Exception as exc:
        print(f"\n{RED}{BOLD}✗ Demo setup aborted:{RESET} {exc}")
        return 1

    output_payload = {
        "baseUrl": args.base_url,
        "customer": customer_summary,
        "restaurants": restaurants_summary,
        "deliveryAgents": agents_summary,
    }
    args.output_file.write_text(json.dumps(output_payload, indent=2), encoding="utf-8")

    primary_rest = restaurants_summary[0]
    primary_item = primary_rest["menuItems"][0]

    print(f"\n{BOLD}{GREEN}{'━' * 88}{RESET}")
    print(f"{BOLD}{GREEN}🎉 DEMO DATA SETUP COMPLETE!{RESET}  {DIM}(Saved IDs to {args.output_file}){RESET}")
    print(f"{BOLD}{GREEN}{'━' * 88}{RESET}")
    print(f"  {BOLD}Customer Login     :{RESET} {customer_summary['email']} / {customer_summary['password']}")
    print(f"  {BOLD}Customer addressId :{RESET} {CYAN}{customer_summary['addressId']}{RESET}")
    print(f"  {BOLD}Primary Restaurant :{RESET} {primary_rest['name']} ({primary_rest['ownerEmail']})")
    print(f"  {BOLD}  ↳ restaurantId   :{RESET} {CYAN}{primary_rest['restaurantId']}{RESET}")
    print(f"  {BOLD}  ↳ menuItemId     :{RESET} {CYAN}{primary_item['id']}{RESET} ({primary_item['name']})")
    print(f"{BOLD}{GREEN}{'━' * 88}{RESET}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

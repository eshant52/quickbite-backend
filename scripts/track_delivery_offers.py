#!/usr/bin/env python3
"""
QuickBite Multi-Agent Delivery Offer Tracker
============================================
Interactive CLI tool for testing QuickBite's proactive delivery dispatch across
up to 10 delivery agents simultaneously — 100% via QuickBite REST APIs (works
against both local and deployed cloud environments without direct DB access).

Features:
  1. Logs in up to 10 delivery agents (auto-resolves 409 session_limit_exceeded
     by revoking only the single oldest session).
  2. Displays an agent status table and optionally marks offline agents as
     available (available = true) on startup.
  3. Maintains 60s Redis GEO heartbeats in the background so available agents
     remain eligible in RedisDeliveryCandidateSelector during long test sessions.
  4. Continuously polls `GET /api/v1/delivery-agent/offers/active` in a background
     thread for each logged-in agent.
  5. Queues incoming offers in a thread-safe FIFO queue while waiting for user
     input:
       [1] Accept offer  -> POST /api/v1/delivery-agent/orders/{orderId}/accept
       [2] Reject offer  -> POST /api/v1/delivery-agent/orders/{orderId}/reject
       [3] Skip offer    -> Leave untouched (let natural timeout/escalation run)
"""

from __future__ import annotations

import argparse
import dataclasses
import datetime as dt
import json
import os
import queue
import sys
import threading
import time
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional, Set

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
BLUE = "\033[94m"

MAX_AGENTS = 10
USER_AGENT = "QuickBite-OfferTracker/1.0 (Android 14; Mobile)"


# ── Data Structures ───────────────────────────────────────────────────────────

@dataclasses.dataclass
class AgentSession:
    email: str
    password: str
    access_token: str
    refresh_token: Optional[str]
    token_expires_at: float
    agent_id: str
    user_id: str
    name: str
    phone: str
    is_available: bool
    is_assigned: bool
    verification_status: str
    vehicle_type: Optional[str]
    vehicle_plate: Optional[str]
    latitude: Optional[float]
    longitude: Optional[float]
    lock: threading.Lock = dataclasses.field(default_factory=threading.Lock, repr=False)


@dataclasses.dataclass
class DiscoveredOffer:
    offer_id: str
    order_id: str
    agent_id: str
    round_number: int
    radius_km: float
    offered_at: str
    expires_at: str
    order_status: str
    total_amount: float
    subtotal: float
    customer_name: str
    customer_phone: str
    items: List[Dict[str, Any]]
    initial_summary: Optional[Dict[str, Any]] = None

    @classmethod
    def from_api_payload(cls, agent_id: str, data: Dict[str, Any]) -> "DiscoveredOffer":
        order_id = str(data.get("orderId") or "")
        offer_id = str(data.get("offerId") or f"{order_id}:{agent_id}")
        return cls(
            offer_id=offer_id,
            order_id=order_id,
            agent_id=agent_id,
            round_number=int(data.get("roundNumber") or 0),
            radius_km=float(data.get("radiusKm") or 0.0),
            offered_at=str(data.get("offeredAt") or ""),
            expires_at=str(data.get("offerExpiresAt") or ""),
            order_status=str(data.get("orderStatus") or "ACCEPTED"),
            total_amount=float(data.get("orderTotalAmount") or 0.0),
            subtotal=float(data.get("orderSubtotal") or 0.0),
            customer_name=str(data.get("customerName") or "Customer"),
            customer_phone=str(data.get("customerPhone") or "-"),
            items=list(data.get("items") or []),
            initial_summary=data,
        )


# ── API Client & Session Management ───────────────────────────────────────────

class QuickBiteClient:
    def __init__(self, base_url: str, timeout: float = 8.0):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.http = requests.Session()

    def _base_headers(self) -> Dict[str, str]:
        return {
            "X-Client-Type": "mobile_app",
            "User-Agent": USER_AGENT,
            "Content-Type": "application/json",
        }

    def login_raw(self, email: str, password: str) -> Dict[str, Any]:
        """Authenticate an agent, evicting ONLY the single oldest session on 409."""
        url = f"{self.base_url}/api/v1/auth/login"
        payload = {"email": email, "password": password}
        resp = self.http.post(url, json=payload, headers=self._base_headers(), timeout=self.timeout)

        if resp.status_code == 200:
            return resp.json()

        if resp.status_code == 409:
            err = resp.json()
            challenge_token = err.get("challengeToken")
            if not challenge_token:
                raise RuntimeError(f"409 conflict without challengeToken: {resp.text}")

            ch_headers = {
                **self._base_headers(),
                "Authorization": f"Bearer {challenge_token}",
            }
            sessions_resp = self.http.get(
                f"{self.base_url}/api/v1/auth/sessions",
                headers=ch_headers,
                timeout=self.timeout,
            )
            if sessions_resp.status_code == 200:
                sessions = sessions_resp.json() or []
                if sessions:
                    # Sort ascending by createdAt so we only evict the single oldest session
                    sessions.sort(key=lambda s: s.get("createdAt") or "")
                    oldest_id = sessions[0].get("id") or sessions[0].get("sessionId")
                    if oldest_id:
                        self.http.delete(
                            f"{self.base_url}/api/v1/auth/sessions/{oldest_id}",
                            headers=ch_headers,
                            timeout=self.timeout,
                        )

            retry = self.http.post(url, json=payload, headers=self._base_headers(), timeout=self.timeout)
            if retry.status_code == 200:
                return retry.json()
            raise RuntimeError(f"Login retry after session eviction failed ({retry.status_code}): {retry.text}")

        raise RuntimeError(f"HTTP {resp.status_code}: {resp.text}")

    def ensure_valid_token(self, agent: AgentSession, force: bool = False) -> str:
        """Return a valid access token, refreshing or re-logging in if near expiry."""
        with agent.lock:
            now = time.time()
            if not force and now < (agent.token_expires_at - 45):
                return agent.access_token

            # Try refresh token first
            if agent.refresh_token:
                try:
                    resp = self.http.post(
                        f"{self.base_url}/api/v1/auth/refresh-token",
                        json={"refreshToken": agent.refresh_token},
                        headers=self._base_headers(),
                        timeout=self.timeout,
                    )
                    if resp.status_code == 200:
                        data = resp.json()
                        agent.access_token = data["accessToken"]
                        agent.refresh_token = data.get("refreshToken") or agent.refresh_token
                        agent.token_expires_at = time.time() + int(data.get("expiresIn") or 900)
                        return agent.access_token
                except requests.RequestException:
                    pass

            # Fallback to full re-login
            data = self.login_raw(agent.email, agent.password)
            agent.access_token = data["accessToken"]
            agent.refresh_token = data.get("refreshToken")
            agent.token_expires_at = time.time() + int(data.get("expiresIn") or 900)
            return agent.access_token

    def request_as_agent(
        self,
        agent: AgentSession,
        method: str,
        path: str,
        json_body: Optional[Dict[str, Any]] = None,
    ) -> requests.Response:
        token = self.ensure_valid_token(agent)
        headers = {**self._base_headers(), "Authorization": f"Bearer {token}"}
        resp = self.http.request(
            method,
            f"{self.base_url}{path}",
            json=json_body,
            headers=headers,
            timeout=self.timeout,
        )
        if resp.status_code == 401:
            token = self.ensure_valid_token(agent, force=True)
            headers["Authorization"] = f"Bearer {token}"
            resp = self.http.request(
                method,
                f"{self.base_url}{path}",
                json=json_body,
                headers=headers,
                timeout=self.timeout,
            )
        return resp

    def login_and_load_agent(self, email: str, password: str) -> AgentSession:
        auth_data = self.login_raw(email, password)
        access_token = auth_data["accessToken"]
        refresh_token = auth_data.get("refreshToken")
        expires_in = int(auth_data.get("expiresIn") or 900)

        headers = {**self._base_headers(), "Authorization": f"Bearer {access_token}"}
        prof_resp = self.http.get(
            f"{self.base_url}/api/v1/delivery-agent/profile",
            headers=headers,
            timeout=self.timeout,
        )
        if prof_resp.status_code != 200:
            raise RuntimeError(f"Profile fetch failed ({prof_resp.status_code}): {prof_resp.text}")

        prof = prof_resp.json()
        return AgentSession(
            email=email,
            password=password,
            access_token=access_token,
            refresh_token=refresh_token,
            token_expires_at=time.time() + expires_in,
            agent_id=prof["id"],
            user_id=prof["userId"],
            name=prof.get("userName") or email,
            phone=prof.get("userPhone") or "-",
            is_available=bool(prof.get("isAvailable")),
            is_assigned=bool(prof.get("isAssigned")),
            verification_status=prof.get("currentStatus") or "UNKNOWN",
            vehicle_type=prof.get("currentVehicleType"),
            vehicle_plate=prof.get("currentVehicleNumberPlate"),
            latitude=prof.get("latitude"),
            longitude=prof.get("longitude"),
        )

    def refresh_agent_profile(self, agent: AgentSession) -> None:
        resp = self.request_as_agent(agent, "GET", "/api/v1/delivery-agent/profile")
        if resp.status_code == 200:
            prof = resp.json()
            agent.is_available = bool(prof.get("isAvailable"))
            agent.is_assigned = bool(prof.get("isAssigned"))
            agent.verification_status = prof.get("currentStatus") or agent.verification_status
            agent.vehicle_type = prof.get("currentVehicleType")
            agent.vehicle_plate = prof.get("currentVehicleNumberPlate")
            agent.latitude = prof.get("latitude")
            agent.longitude = prof.get("longitude")

    def set_availability(self, agent: AgentSession, available: bool) -> requests.Response:
        resp = self.request_as_agent(
            agent,
            "PATCH",
            "/api/v1/delivery-agent/availability",
            json_body={"available": available},
        )
        if resp.status_code == 200:
            data = resp.json()
            agent.is_available = bool(data.get("isAvailable"))
            agent.is_assigned = bool(data.get("isAssigned"))
        return resp

    def send_location_heartbeat(self, agent: AgentSession) -> Optional[requests.Response]:
        if agent.latitude is None or agent.longitude is None:
            return None
        return self.request_as_agent(
            agent,
            "PUT",
            "/api/v1/delivery-agent/location",
            json_body={"latitude": agent.latitude, "longitude": agent.longitude},
        )

    def get_active_offers(self, agent: AgentSession) -> requests.Response:
        return self.request_as_agent(
            agent,
            "GET",
            "/api/v1/delivery-agent/offers/active",
        )

    def get_offer_summary(self, agent: AgentSession, order_id: str) -> requests.Response:
        return self.request_as_agent(
            agent,
            "GET",
            f"/api/v1/delivery-agent/orders/{order_id}/offer-summary",
        )

    def accept_offer(self, agent: AgentSession, order_id: str) -> requests.Response:
        return self.request_as_agent(
            agent,
            "POST",
            f"/api/v1/delivery-agent/orders/{order_id}/accept",
        )

    def reject_offer(self, agent: AgentSession, order_id: str) -> requests.Response:
        return self.request_as_agent(
            agent,
            "POST",
            f"/api/v1/delivery-agent/orders/{order_id}/reject",
        )


def poll_active_offers_via_api(
    client: QuickBiteClient,
    agents_by_id: Dict[str, AgentSession],
) -> List[DiscoveredOffer]:
    """Poll `GET /api/v1/delivery-agent/offers/active` for each tracked agent."""
    discovered: List[DiscoveredOffer] = []
    for agent_id, agent in agents_by_id.items():
        resp = client.get_active_offers(agent)
        if resp.status_code != 200:
            raise RuntimeError(
                f"GET /offers/active for {agent.name} returned HTTP {resp.status_code}: {resp.text}"
            )
        payload_list = resp.json() or []
        for item in payload_list:
            discovered.append(DiscoveredOffer.from_api_payload(agent_id, item))
    return discovered


# ── Tracker Coordinator (Poller + Heartbeat + Interactive Queue) ──────────────

class OfferTrackerCoordinator:
    def __init__(
        self,
        client: QuickBiteClient,
        agents_by_id: Dict[str, AgentSession],
        offer_fetcher: Optional[Callable[[List[str]], List[DiscoveredOffer]]] = None,
        poll_interval: float = 1.5,
        enable_heartbeat: bool = True,
    ):
        self.client = client
        self.agents_by_id = agents_by_id
        self.offer_fetcher: Callable[[List[str]], List[DiscoveredOffer]] = (
            offer_fetcher
            if offer_fetcher is not None
            else lambda _ids: poll_active_offers_via_api(self.client, self.agents_by_id)
        )
        self.poll_interval = poll_interval
        self.enable_heartbeat = enable_heartbeat

        self.offer_queue: "queue.Queue[DiscoveredOffer]" = queue.Queue()
        self.seen_offer_ids: Set[str] = set()
        self.seen_lock = threading.Lock()
        self.print_lock = threading.Lock()
        self.stop_event = threading.Event()

        # Prompt coordination so background notifications don't garble user input
        self.waiting_for_input = False
        self.active_prompt_text = ""

    def safe_print(self, msg: str) -> None:
        with self.print_lock:
            print(msg, flush=True)

    def notify_background_offer(self, offer: DiscoveredOffer) -> None:
        agent = self.agents_by_id.get(offer.agent_id)
        agent_name = agent.name if agent else offer.agent_id
        qsize = self.offer_queue.qsize()
        with self.print_lock:
            if self.waiting_for_input:
                # Print alert cleanly above the active input prompt and redraw prompt
                sys.stdout.write(
                    f"\a\n{YELLOW}{BOLD}🔔 [NEW OFFER QUEUED]{RESET} "
                    f"Agent: {BOLD}{agent_name}{RESET} | "
                    f"Order: {CYAN}{offer.order_id}{RESET} | "
                    f"Total: ₹{offer.total_amount:.2f} "
                    f"{DIM}(Waiting in queue: {qsize}){RESET}\n"
                    f"{self.active_prompt_text}"
                )
                sys.stdout.flush()

    def _poller_loop(self) -> None:
        agent_ids = list(self.agents_by_id.keys())
        last_poll_err: Optional[str] = None

        while not self.stop_event.is_set():
            try:
                pending_offers = self.offer_fetcher(agent_ids)
                if last_poll_err is not None:
                    self.safe_print(f"{GREEN}✓ API polling connection restored.{RESET}")
                    last_poll_err = None

                for offer in pending_offers:
                    with self.seen_lock:
                        if offer.offer_id in self.seen_offer_ids:
                            continue
                        self.seen_offer_ids.add(offer.offer_id)

                    self.offer_queue.put(offer)
                    self.notify_background_offer(offer)

            except Exception as exc:
                err_msg = str(exc)
                if err_msg != last_poll_err:
                    self.safe_print(f"{RED}⚠ Poll warning: {err_msg}{RESET}")
                    last_poll_err = err_msg

            self.stop_event.wait(self.poll_interval)

    def _heartbeat_loop(self) -> None:
        """Refresh Redis GEO 60s heartbeat every 35s for available agents with coordinates."""
        while not self.stop_event.wait(35.0):
            for agent in self.agents_by_id.values():
                if self.stop_event.is_set():
                    return
                if agent.is_available and agent.latitude is not None and agent.longitude is not None:
                    try:
                        self.client.send_location_heartbeat(agent)
                    except Exception:
                        pass

    def start_background_workers(self) -> None:
        poller = threading.Thread(target=self._poller_loop, name="offer-poller", daemon=True)
        poller.start()
        if self.enable_heartbeat:
            hb = threading.Thread(target=self._heartbeat_loop, name="geo-heartbeat", daemon=True)
            hb.start()

    def print_agent_table(self) -> None:
        with self.print_lock:
            print(f"\n{BOLD}{'=' * 96}{RESET}")
            print(
                f"{BOLD}🚀 TRACKED DELIVERY AGENTS ({len(self.agents_by_id)}/{MAX_AGENTS}){RESET}"
            )
            print(f"{BOLD}{'=' * 96}{RESET}")
            header = f"{'#':<3} {'Agent Name':<20} {'Email':<28} {'Status':<10} {'Available':<11} {'Assigned':<10} {'Location'}"
            print(BOLD + header + RESET)
            print("-" * 96)
            for idx, agent in enumerate(self.agents_by_id.values(), start=1):
                avail_str = f"{GREEN}YES{RESET}" if agent.is_available else f"{RED}NO{RESET}"
                assign_str = f"{YELLOW}BUSY{RESET}" if agent.is_assigned else f"{DIM}FREE{RESET}"
                loc_str = (
                    f"{agent.latitude:.4f}, {agent.longitude:.4f}"
                    if agent.latitude is not None and agent.longitude is not None
                    else "Not set"
                )
                # Pad ANSI strings manually so columns align
                avail_pad = " " * (11 - (3 if agent.is_available else 2))
                assign_pad = " " * (10 - 4)
                print(
                    f"{idx:<3} {agent.name:<20.20} {agent.email:<28.28} "
                    f"{agent.verification_status:<10.10} "
                    f"{avail_str}{avail_pad} {assign_str}{assign_pad} {loc_str}"
                )
            print("=" * 96)

    def _format_expiry_local(self, iso_ts: Optional[str]) -> str:
        if not iso_ts:
            return "-"
        try:
            normalized = iso_ts.replace("Z", "+00:00")
            parsed = dt.datetime.fromisoformat(normalized).astimezone()
            return parsed.strftime("%H:%M:%S")
        except ValueError:
            return iso_ts

    def _render_offer_card(
        self,
        agent: AgentSession,
        offer: DiscoveredOffer,
        summary: Dict[str, Any],
    ) -> None:
        waiting_count = self.offer_queue.qsize()
        secs_left = int(summary.get("secondsRemaining") or 0)
        expires_local = self._format_expiry_local(summary.get("offerExpiresAt") or offer.expires_at)
        dist_km = summary.get("deliveryDistanceKm")
        eta_sec = summary.get("estimatedDeliverySeconds")

        guar_payout = float(summary.get("guaranteedPayout") or 0.0)
        tip_amt = float(summary.get("tipAmount") or 0.0)
        total_payout = float(summary.get("estimatedTotalPayout") or 0.0)

        order_status = str(summary.get("orderStatus") or offer.order_status or "ACCEPTED")
        total_amount = float(summary.get("orderTotalAmount") or offer.total_amount or 0.0)
        customer_name = str(summary.get("customerName") or offer.customer_name or "Customer")
        customer_phone = str(summary.get("customerPhone") or offer.customer_phone or "-")
        items = summary.get("items") if summary.get("items") is not None else offer.items

        veh_desc = (
            f"{agent.vehicle_type} ({agent.vehicle_plate})"
            if agent.vehicle_type and agent.vehicle_plate
            else (agent.vehicle_type or "N/A")
        )

        timer_color = GREEN if secs_left > 20 else (YELLOW if secs_left > 10 else RED)
        queue_badge = (
            f"{YELLOW}{BOLD} | 🔔 Queued Waiting: {waiting_count}{RESET}"
            if waiting_count > 0
            else f"{DIM} | Queue Empty (0 waiting){RESET}"
        )

        with self.print_lock:
            print(f"\a\n{BOLD}{CYAN}{'━' * 88}{RESET}")
            print(
                f"{BOLD}{CYAN}🎯 ACTIVE DELIVERY OFFER{RESET}  "
                f"{DIM}(Round #{offer.round_number}, Radius {offer.radius_km:.1f} km){RESET}"
                f"{queue_badge}"
            )
            print(f"{BOLD}{CYAN}{'━' * 88}{RESET}")
            print(f"  {BOLD}👤 Delivery Agent :{RESET} {BOLD}{GREEN}{agent.name}{RESET} ({agent.email} | {agent.phone})")
            print(f"  {BOLD}🛵 Vehicle / ID   :{RESET} {veh_desc}  {DIM}[agentId: {agent.agent_id}]{RESET}")
            print(f"  {DIM}{'─' * 84}{RESET}")
            print(
                f"  {BOLD}📦 Order ID       :{RESET} {BOLD}{offer.order_id}{RESET}  "
                f"{MAGENTA}[Status: {order_status}]{RESET}"
            )
            print(
                f"  {BOLD}🧑 Customer       :{RESET} {customer_name} ({customer_phone})  |  "
                f"{BOLD}Order Bill:{RESET} ₹{total_amount:.2f}"
            )
            if items:
                print(f"  {BOLD}🍽️  Order Items    :{RESET}")
                for item in items:
                    qty = item.get("quantity", 1)
                    iname = item.get("name", "Item")
                    sub = float(item.get("subTotal") or 0.0)
                    print(f"                      • {qty} × {iname} (₹{sub:.2f})")
            print(f"  {DIM}{'─' * 84}{RESET}")
            print(f"  {BOLD}🏪 Restaurant     :{RESET} {summary.get('restaurantName') or '-'}")
            if summary.get("restaurantAddress"):
                print(f"                      {DIM}{summary.get('restaurantAddress')}{RESET}")
            print(f"  {BOLD}📍 Drop Address   :{RESET} {summary.get('deliveryAddress') or '-'}")
            dist_str = f"{float(dist_km):.2f} km" if dist_km is not None else "-"
            eta_str = f"~{int(eta_sec)} sec ({int(eta_sec) // 60}m {int(eta_sec) % 60}s)" if eta_sec is not None else "-"
            print(f"  {BOLD}🛣️  Distance / ETA :{RESET} {dist_str}  |  {eta_str}")
            print(
                f"  {BOLD}💰 Agent Payout   :{RESET} {BOLD}{GREEN}₹{total_payout:.2f}{RESET} "
                f"{DIM}(Guaranteed: ₹{guar_payout:.2f} + Tip: ₹{tip_amt:.2f}){RESET}"
            )
            print(
                f"  {BOLD}⏳ Expires In     :{RESET} {timer_color}{BOLD}{secs_left} seconds{RESET} "
                f"{DIM}(at {expires_local}){RESET}"
            )
            print(f"{BOLD}{CYAN}{'━' * 88}{RESET}")

    def _extract_error_detail(self, resp: requests.Response) -> str:
        try:
            body = resp.json()
            return body.get("detail") or body.get("message") or body.get("error") or resp.text
        except ValueError:
            return resp.text or f"HTTP {resp.status_code}"

    def handle_single_offer(self, offer: DiscoveredOffer) -> bool:
        """
        Process one popped offer interactively.
        Returns False only if the user requested to quit (`q`), True otherwise.
        """
        agent = self.agents_by_id.get(offer.agent_id)
        if not agent:
            return True

        while not self.stop_event.is_set():
            # Fetch live offer summary via the agent's authenticated API
            try:
                summary_resp = self.client.get_offer_summary(agent, offer.order_id)
            except requests.RequestException as exc:
                self.safe_print(f"{RED}⚠ Network error fetching offer summary: {exc}{RESET}")
                return True

            if summary_resp.status_code != 200:
                self.safe_print(
                    f"{YELLOW}⏰ Skipping queued offer for {BOLD}{agent.name}{RESET}{YELLOW} "
                    f"(Order {offer.order_id[:8]}...): no longer active "
                    f"({self._extract_error_detail(summary_resp)}).{RESET}"
                )
                return True

            summary = summary_resp.json()
            secs_remaining = int(summary.get("secondsRemaining") or 0)
            if secs_remaining <= 0:
                self.safe_print(
                    f"{YELLOW}⏰ Offer for {BOLD}{agent.name}{RESET}{YELLOW} "
                    f"(Order {offer.order_id[:8]}...) expired while waiting in queue.{RESET}"
                )
                return True

            self._render_offer_card(agent, offer, summary)

            prompt = (
                f"👉 Action for {BOLD}{agent.name}{RESET} "
                f"[{GREEN}1{RESET}=Accept, {RED}2{RESET}=Reject, {YELLOW}3{RESET}=Skip, "
                f"{DIM}r=Refresh, s=Agents, q=Quit{RESET}]: "
            )
            with self.print_lock:
                self.waiting_for_input = True
                self.active_prompt_text = prompt
                sys.stdout.write(prompt)
                sys.stdout.flush()

            try:
                choice = sys.stdin.readline()
            except KeyboardInterrupt:
                with self.print_lock:
                    self.waiting_for_input = False
                return False

            with self.print_lock:
                self.waiting_for_input = False
                self.active_prompt_text = ""

            if choice == "":  # EOF
                return False

            cmd = choice.strip().lower()

            if cmd == "1":
                try:
                    resp = self.client.accept_offer(agent, offer.order_id)
                except Exception as exc:
                    self.safe_print(f"\n{RED}⚠ Network error accepting offer: {exc}{RESET}")
                    return True
                if resp.status_code == 200:
                    agent.is_assigned = True
                    self.safe_print(
                        f"\n{GREEN}{BOLD}✅ ACCEPTED!{RESET} {BOLD}{agent.name}{RESET} "
                        f"is now assigned to Order {BOLD}{offer.order_id}{RESET}."
                    )
                else:
                    self.safe_print(
                        f"\n{RED}{BOLD}⚠ Accept failed (HTTP {resp.status_code}):{RESET} "
                        f"{self._extract_error_detail(resp)}"
                    )
                return True

            elif cmd == "2":
                try:
                    resp = self.client.reject_offer(agent, offer.order_id)
                except Exception as exc:
                    self.safe_print(f"\n{RED}⚠ Network error rejecting offer: {exc}{RESET}")
                    return True
                if resp.status_code in (200, 204):
                    self.safe_print(
                        f"\n{RED}{BOLD}❌ REJECTED!{RESET} {BOLD}{agent.name}{RESET} rejected "
                        f"Order {BOLD}{offer.order_id}{RESET}. "
                        f"{DIM}(Watching for next escalated agent offer...){RESET}"
                    )
                else:
                    self.safe_print(
                        f"\n{YELLOW}⚠ Reject response (HTTP {resp.status_code}): "
                        f"{self._extract_error_detail(resp)}{RESET}"
                    )
                return True

            elif cmd == "3":
                self.safe_print(
                    f"\n{YELLOW}⏭  SKIPPED!{RESET} Left offer for {BOLD}{agent.name}{RESET} "
                    f"(Order {offer.order_id[:8]}...) untouched in PENDING state."
                )
                return True

            elif cmd == "r":
                # Loop continues -> re-fetches live secondsRemaining & redraws card
                continue

            elif cmd == "s":
                for ag in self.agents_by_id.values():
                    try:
                        self.client.refresh_agent_profile(ag)
                    except Exception:
                        pass
                self.print_agent_table()
                continue

            elif cmd == "q":
                return False

            else:
                self.safe_print(
                    f"{YELLOW}Invalid input '{cmd}'. Please enter 1 (Accept), 2 (Reject), or 3 (Skip).{RESET}"
                )

        return False

    def run_interactive_loop(self) -> None:
        self.safe_print(
            f"\n{GREEN}{BOLD}👀 Listening for live delivery offers across "
            f"{len(self.agents_by_id)} agent(s) via REST API...{RESET} {DIM}(Ctrl+C to exit){RESET}"
        )
        try:
            while not self.stop_event.is_set():
                try:
                    offer = self.offer_queue.get(timeout=0.5)
                except queue.Empty:
                    continue

                keep_running = self.handle_single_offer(offer)
                self.offer_queue.task_done()
                if not keep_running:
                    break

                if self.offer_queue.empty():
                    self.safe_print(
                        f"\n{DIM}👀 Queue clear — listening for incoming delivery offers... (Ctrl+C to exit){RESET}"
                    )
        except KeyboardInterrupt:
            pass
        finally:
            self.stop_event.set()
            self.safe_print(f"\n{CYAN}👋 Offer tracker stopped cleanly.{RESET}")


# ── Startup & CLI Entrypoint ──────────────────────────────────────────────────

def load_agent_credentials(agents_file: Path) -> List[Dict[str, Any]]:
    if not agents_file.is_file():
        raise FileNotFoundError(f"Agents credentials file not found: {agents_file}")
    raw = json.loads(agents_file.read_text(encoding="utf-8"))
    if not isinstance(raw, list):
        raise ValueError("Agents file must contain a JSON array of credential objects.")
    enabled = [entry for entry in raw if entry.get("enabled", True)]
    if len(enabled) > MAX_AGENTS:
        print(
            f"{YELLOW}⚠ {len(enabled)} agents enabled in {agents_file.name}; "
            f"capping to the first {MAX_AGENTS} agents.{RESET}"
        )
        enabled = enabled[:MAX_AGENTS]
    return enabled


def maybe_enable_offline_agents(
    client: QuickBiteClient,
    agents: List[AgentSession],
    auto_mode: Optional[bool],
) -> None:
    eligible_offline = [
        a for a in agents
        if not a.is_available and not a.is_assigned and a.verification_status == "APPROVED"
    ]
    if not eligible_offline:
        return

    should_enable = False
    if auto_mode is True:
        should_enable = True
    elif auto_mode is False:
        should_enable = False
    else:
        names = ", ".join(a.name for a in eligible_offline)
        try:
            ans = input(
                f"\n{YELLOW}⚡ {len(eligible_offline)} approved agent(s) are currently OFFLINE ({names}).{RESET}\n"
                f"👉 Mark them as {GREEN}AVAILABLE (available = true){RESET} now so they can receive offers? [Y/n]: "
            ).strip().lower()
        except (KeyboardInterrupt, EOFError):
            print()
            return
        should_enable = ans in ("", "y", "yes")

    if not should_enable:
        return

    print(f"\n{CYAN}Updating availability to AVAILABLE...{RESET}")
    for agent in eligible_offline:
        try:
            resp = client.set_availability(agent, True)
            if resp.status_code == 200:
                # Immediately push location heartbeat into Redis GEO if coordinates exist
                if agent.latitude is not None and agent.longitude is not None:
                    client.send_location_heartbeat(agent)
                print(f"  {GREEN}✓{RESET} {agent.name} is now {GREEN}AVAILABLE{RESET}")
            else:
                print(f"  {YELLOW}⚠{RESET} Could not mark {agent.name} available ({resp.status_code}): {resp.text}")
        except Exception as exc:
            print(f"  {RED}✗{RESET} Error updating {agent.name}: {exc}")


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    default_agents_file = Path(__file__).resolve().parent / "agents.json"
    parser = argparse.ArgumentParser(
        description="QuickBite Multi-Agent Delivery Offer Tracker (tracks up to 10 agents via REST API)"
    )
    parser.add_argument(
        "--base-url",
        default=os.environ.get("QUICKBITE_BASE_URL", "http://localhost:8080"),
        help="QuickBite backend base URL (default: http://localhost:8080)",
    )
    parser.add_argument(
        "--agents-file",
        type=Path,
        default=default_agents_file,
        help=f"Path to JSON file with delivery agent credentials (default: {default_agents_file})",
    )
    parser.add_argument(
        "--poll-interval",
        type=float,
        default=1.5,
        help="Background API polling interval in seconds (default: 1.5)",
    )

    avail_group = parser.add_mutually_exclusive_group()
    avail_group.add_argument(
        "--auto-available",
        dest="auto_available",
        action="store_true",
        default=None,
        help="Automatically mark offline approved agents as available=true on startup without asking",
    )
    avail_group.add_argument(
        "--no-auto-available",
        dest="auto_available",
        action="store_false",
        help="Do not prompt or change agent availability on startup",
    )

    parser.add_argument(
        "--no-heartbeat",
        action="store_true",
        help="Disable background 35s Redis GEO location heartbeat refresh",
    )
    return parser.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)

    print(f"{BOLD}{CYAN}⚡ QuickBite Delivery Offer Tracker (REST API Mode){RESET}")
    print(f"{DIM}Backend: {args.base_url}  |  Endpoint: GET /api/v1/delivery-agent/offers/active{RESET}\n")

    try:
        creds_list = load_agent_credentials(args.agents_file)
    except Exception as exc:
        print(f"{RED}✗ Failed to load credentials: {exc}{RESET}")
        return 1

    if not creds_list:
        print(f"{RED}✗ No enabled delivery agents found in {args.agents_file}.{RESET}")
        return 1

    client = QuickBiteClient(args.base_url)
    agents: List[AgentSession] = []

    print(f"{BOLD}Logging in {len(creds_list)} delivery agent(s)...{RESET}")
    for entry in creds_list:
        email = entry.get("email", "").strip()
        password = entry.get("password", "")
        if not email or not password:
            continue
        try:
            session = client.login_and_load_agent(email, password)
            # Optional coordinate override in agents.json if agent has no saved location yet
            if session.latitude is None and entry.get("latitude") is not None:
                session.latitude = float(entry["latitude"])
            if session.longitude is None and entry.get("longitude") is not None:
                session.longitude = float(entry["longitude"])
            agents.append(session)
            print(f"  {GREEN}✓{RESET} Logged in: {BOLD}{session.name}{RESET} ({email})")
        except Exception as exc:
            print(f"  {RED}✗{RESET} Failed login for {email}: {exc}")

    if not agents:
        print(f"\n{RED}✗ Could not log in any delivery agents. Is QuickBite running on {args.base_url}?{RESET}")
        return 1

    agents_by_id = {a.agent_id: a for a in agents}
    coordinator = OfferTrackerCoordinator(
        client=client,
        agents_by_id=agents_by_id,
        poll_interval=args.poll_interval,
        enable_heartbeat=not args.no_heartbeat,
    )

    coordinator.print_agent_table()
    maybe_enable_offline_agents(client, agents, args.auto_available)

    # Refresh Redis GEO heartbeat immediately on startup for all available agents
    if not args.no_heartbeat:
        for ag in agents:
            if ag.is_available and ag.latitude is not None and ag.longitude is not None:
                try:
                    client.send_location_heartbeat(ag)
                except Exception:
                    pass

    coordinator.start_background_workers()
    coordinator.run_interactive_loop()
    return 0


if __name__ == "__main__":
    sys.exit(main())

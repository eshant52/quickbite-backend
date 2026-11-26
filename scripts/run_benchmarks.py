#!/usr/bin/env python3
"""
QuickBite Local Reproducible Benchmark Runner
=============================================
Collects real, reproducible performance measurements on the developer's local
environment (MacBook Pro / Docker PostgreSQL + PostGIS / Redis / OSRM / Spring Boot)
aligned with Table 5.04 of the QuickBite Master's Project Report.

Design guarantees:
- Zero external pip dependencies beyond `requests` (uses stdlib + `psql` CLI + raw RESP2 TCP socket).
- Non-destructive: all synthetic PostgreSQL scale benchmarks run inside `BEGIN; ... ROLLBACK;`,
  leaving zero synthetic rows behind.
- Isolated Redis GEO benchmarks use `quickbite:bench:geo:*` keys and clean up in `finally`.
- Clean session lifecycle: logs in via `/api/v1/auth/login` (handles 409 session limit)
  and always calls `/api/v1/auth/logout` on exit.

Outputs (in `--output-dir`, default `scripts/benchmark_results/`):
- `benchmark_summary.md` : Ready-to-review Markdown summary, Table 5.04 numbers, & method note.
- `benchmark_raw.json`   : Full environment metadata, raw latencies, and EXPLAIN JSON plans.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import platform
import re
import shutil
import socket
import statistics
import subprocess
import sys
import time
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import requests


# ── Constants & Formatting ────────────────────────────────────────────────────

IST = timezone(timedelta(hours=5, minutes=30))
BENCH_GEO_KEY = "quickbite:bench:geo:locations"
BENCH_HEARTBEAT_PREFIX = "quickbite:bench:geo:heartbeat:"
BENCH_USER_AGENT = "QuickBite-DemoSeeder/1.0 (Android 14; Mobile)"


# ── Minimal RESP2 Redis TCP Client ────────────────────────────────────────────

class SimpleRedisError(RuntimeError):
    """Raised when Redis returns an error reply or protocol failure."""


class SimpleRedisClient:
    """Lightweight RESP2 Redis client over stdlib TCP socket (zero pip dependencies)."""

    def __init__(self, host: str, port: int, username: Optional[str], password: Optional[str], timeout: float = 5.0):
        self.host = host
        self.port = port
        self.username = username
        self.password = password
        self.timeout = timeout
        self._sock: Optional[socket.socket] = None
        self._reader = None

    def connect(self) -> None:
        self._sock = socket.create_connection((self.host, self.port), timeout=self.timeout)
        self._reader = self._sock.makefile("rb")
        if self.password:
            try:
                if self.username:
                    self.execute("AUTH", self.username, self.password)
                else:
                    self.execute("AUTH", self.password)
            except SimpleRedisError:
                # Fallback if Redis instance is configured without ACL username
                self.execute("AUTH", self.password)

    def close(self) -> None:
        try:
            if self._reader is not None:
                self._reader.close()
        finally:
            self._reader = None
            if self._sock is not None:
                try:
                    self._sock.close()
                finally:
                    self._sock = None

    def __enter__(self) -> "SimpleRedisClient":
        self.connect()
        return self

    def __exit__(self, exc_type, exc, tb) -> None:
        self.close()

    @staticmethod
    def _encode_cmd(*args: Any) -> bytes:
        parts = [f"*{len(args)}\r\n".encode("ascii")]
        for arg in args:
            raw = arg if isinstance(arg, bytes) else str(arg).encode("utf-8")
            parts.append(f"${len(raw)}\r\n".encode("ascii"))
            parts.append(raw)
            parts.append(b"\r\n")
        return b"".join(parts)

    def _read_line(self) -> bytes:
        assert self._reader is not None
        line = self._reader.readline()
        if not line:
            raise SimpleRedisError("Unexpected EOF reading from Redis")
        return line[:-2] if line.endswith(b"\r\n") else line.rstrip(b"\n")

    def _read_reply(self) -> Any:
        assert self._reader is not None
        header = self._read_line()
        prefix = header[:1]
        payload = header[1:]

        if prefix == b"+":
            return payload.decode("utf-8", errors="replace")
        if prefix == b"-":
            raise SimpleRedisError(payload.decode("utf-8", errors="replace"))
        if prefix == b":":
            return int(payload)
        if prefix == b"$":
            length = int(payload)
            if length == -1:
                return None
            data = self._reader.read(length)
            crlf = self._reader.read(2)
            if len(data) != length or crlf != b"\r\n":
                raise SimpleRedisError("Truncated bulk string from Redis")
            return data.decode("utf-8", errors="replace")
        if prefix == b"*":
            count = int(payload)
            if count == -1:
                return None
            return [self._read_reply() for _ in range(count)]
        raise SimpleRedisError(f"Unsupported RESP2 prefix: {header!r}")

    def execute(self, *args: Any) -> Any:
        assert self._sock is not None
        self._sock.sendall(self._encode_cmd(*args))
        return self._read_reply()

    def pipeline(self, commands: List[Tuple[Any, ...]]) -> List[Any]:
        if not commands:
            return []
        assert self._sock is not None
        buf = b"".join(self._encode_cmd(*cmd) for cmd in commands)
        self._sock.sendall(buf)
        return [self._read_reply() for _ in commands]

    def ping(self) -> str:
        return str(self.execute("PING"))

    def info_server(self) -> Dict[str, str]:
        raw = self.execute("INFO", "server") or ""
        parsed: Dict[str, str] = {}
        for line in raw.splitlines():
            if line and not line.startswith("#") and ":" in line:
                k, v = line.split(":", 1)
                parsed[k.strip()] = v.strip()
        return parsed

    def scan_delete(self, pattern: str) -> int:
        cursor = "0"
        deleted = 0
        while True:
            reply = self.execute("SCAN", cursor, "MATCH", pattern, "COUNT", 500)
            cursor = str(reply[0])
            keys = reply[1] or []
            if keys:
                deleted += int(self.execute("DEL", *keys))
            if cursor == "0":
                break
        return deleted


# ── Statistics Helpers ────────────────────────────────────────────────────────

def percentile(sorted_vals: List[float], pct: float) -> float:
    if not sorted_vals:
        return 0.0
    if len(sorted_vals) == 1:
        return sorted_vals[0]
    rank = (pct / 100.0) * (len(sorted_vals) - 1)
    low = math.floor(rank)
    high = math.ceil(rank)
    if low == high:
        return sorted_vals[low]
    frac = rank - low
    return sorted_vals[low] * (1.0 - frac) + sorted_vals[high] * frac


def summarize_ms(samples: List[float]) -> Dict[str, Any]:
    if not samples:
        return {"count": 0}
    ordered = sorted(samples)
    return {
        "count": len(ordered),
        "min_ms": round(ordered[0], 3),
        "mean_ms": round(statistics.mean(ordered), 3),
        "p50_ms": round(percentile(ordered, 50.0), 3),
        "p95_ms": round(percentile(ordered, 95.0), 3),
        "p99_ms": round(percentile(ordered, 99.0), 3),
        "max_ms": round(ordered[-1], 3),
    }


# ── PostgreSQL CLI Helper ─────────────────────────────────────────────────────

def find_psql_binary() -> str:
    found = shutil.which("psql")
    if found:
        return found
    homebrew_pg17 = "/opt/homebrew/opt/postgresql@17/bin/psql"
    if os.path.exists(homebrew_pg17):
        return homebrew_pg17
    raise FileNotFoundError(
        "Could not locate `psql` CLI on PATH or at /opt/homebrew/opt/postgresql@17/bin/psql."
    )


def run_psql(
    psql_bin: str,
    host: str,
    port: int,
    user: str,
    password: str,
    dbname: str,
    sql: str,
) -> str:
    env = os.environ.copy()
    env["PGPASSWORD"] = password
    cmd = [
        psql_bin,
        "-h", host,
        "-p", str(port),
        "-U", user,
        "-d", dbname,
        "-X",
        "-t",
        "-A",
        "-q",
        "-v", "ON_ERROR_STOP=1",
    ]
    proc = subprocess.run(cmd, input=sql, text=True, capture_output=True, env=env, timeout=300)
    if proc.returncode != 0:
        raise RuntimeError(f"psql failed (exit {proc.returncode}):\n{proc.stderr.strip()}")
    return proc.stdout


def extract_explain_blocks(stdout: str) -> Dict[str, List[Dict[str, Any]]]:
    """Extract tagged JSON EXPLAIN blocks from psql stdout."""
    pattern = re.compile(r"<<<BENCH:([^>]+)>>>\s*(.*?)\s*<<<END:\1>>>", re.S)
    results: Dict[str, List[Dict[str, Any]]] = {}
    for key, raw_json in pattern.findall(stdout):
        parsed = json.loads(raw_json.strip())
        plan_root = parsed[0]
        results.setdefault(key, []).append(plan_root)
    return results


def collect_plan_nodes(node: Dict[str, Any], nodes_out: List[Dict[str, Any]]) -> None:
    nodes_out.append(node)
    for child in node.get("Plans", []) or []:
        collect_plan_nodes(child, nodes_out)


def summarize_explain_runs(runs: List[Dict[str, Any]]) -> Dict[str, Any]:
    exec_times = [float(r["Execution Time"]) for r in runs]
    plan_times = [float(r["Planning Time"]) for r in runs]
    first_plan = runs[0]["Plan"]
    nodes: List[Dict[str, Any]] = []
    collect_plan_nodes(first_plan, nodes)
    indexes = sorted({n["Index Name"] for n in nodes if n.get("Index Name")})
    node_types = [n.get("Node Type") for n in nodes if n.get("Node Type")]
    shared_hit = first_plan.get("Shared Hit Blocks", 0)
    shared_read = first_plan.get("Shared Read Blocks", 0)
    return {
        "runs": len(runs),
        "execution_time_ms": summarize_ms(exec_times),
        "planning_time_ms": summarize_ms(plan_times),
        "actual_rows": first_plan.get("Actual Rows", 0),
        "shared_hit_blocks": shared_hit,
        "shared_read_blocks": shared_read,
        "indexes_used": indexes,
        "node_types": node_types,
    }


# ── Environment Metadata ──────────────────────────────────────────────────────

def _cmd_line(cmd: List[str], use_stderr: bool = False) -> str:
    try:
        proc = subprocess.run(cmd, text=True, capture_output=True, timeout=5)
        out = proc.stderr if use_stderr else proc.stdout
        lines = [ln.strip() for ln in out.splitlines() if ln.strip()]
        return lines[0] if lines else "unknown"
    except Exception:
        return "unknown"


def collect_environment_metadata(
    psql_bin: Optional[str],
    db_cfg: Dict[str, Any],
    redis_cfg: Dict[str, Any],
    osrm_url: str,
) -> Dict[str, Any]:
    now_ist = datetime.now(IST)
    ram_bytes_str = _cmd_line(["sysctl", "-n", "hw.memsize"])
    ram_gb = round(int(ram_bytes_str) / (1024 ** 3), 1) if ram_bytes_str.isdigit() else None

    pg_version = "unavailable"
    postgis_version = "unavailable"
    if psql_bin:
        try:
            pg_version = run_psql(psql_bin, **db_cfg, sql="SELECT version();").strip()
            postgis_version = run_psql(psql_bin, **db_cfg, sql="SELECT postgis_version();").strip()
        except Exception as exc:
            pg_version = f"error: {exc}"

    redis_version = "unavailable"
    try:
        with SimpleRedisClient(**redis_cfg) as rc:
            info = rc.info_server()
            redis_version = info.get("redis_version", "unknown")
    except Exception as exc:
        redis_version = f"error: {exc}"

    osrm_status = "unavailable"
    try:
        r = requests.get(
            f"{osrm_url.rstrip('/')}/route/v1/driving/79.0615,21.1425;79.0805,21.1432?overview=false",
            timeout=3,
        )
        osrm_status = f"reachable (HTTP {r.status_code})" if r.status_code == 200 else f"HTTP {r.status_code}"
    except Exception as exc:
        osrm_status = f"unreachable ({type(exc).__name__})"

    return {
        "timestamp_utc": datetime.now(timezone.utc).isoformat(),
        "timestamp_ist": now_ist.strftime("%d %B %Y %H:%M:%S IST"),
        "machine_arch": platform.machine(),
        "cpu": _cmd_line(["sysctl", "-n", "machdep.cpu.brand_string"]),
        "ram_gb": ram_gb,
        "macos_version": _cmd_line(["sw_vers", "-productVersion"]),
        "java_version": _cmd_line(["java", "-version"], use_stderr=True),
        "python_version": platform.python_version(),
        "postgresql_version": pg_version,
        "postgis_version": postgis_version,
        "redis_version": redis_version,
        "osrm_status": osrm_status,
        "osrm_url": osrm_url,
    }


# ── HTTP Client & Auth Lifecycle ──────────────────────────────────────────────

class QuickBiteHttpSession:
    """Manages an authenticated customer HTTP session against QuickBite."""

    def __init__(self, base_url: str, email: str, password: str, timeout: float = 15.0):
        self.base_url = base_url.rstrip("/")
        self.email = email
        self.password = password
        self.timeout = timeout
        self.session = requests.Session()
        self.access_token: Optional[str] = None

    def _headers(self, token: Optional[str] = None) -> Dict[str, str]:
        headers = {
            "X-Client-Type": "mobile_app",
            "User-Agent": BENCH_USER_AGENT,
            "Content-Type": "application/json",
        }
        tok = token if token is not None else self.access_token
        if tok:
            headers["Authorization"] = f"Bearer {tok}"
        return headers

    def request(self, method: str, path: str, json_body: Any = None, token: Optional[str] = None) -> requests.Response:
        return self.session.request(
            method=method,
            url=f"{self.base_url}{path}",
            headers=self._headers(token),
            json=json_body,
            timeout=self.timeout,
        )

    def login(self) -> None:
        resp = self.request("POST", "/api/v1/auth/login", json_body={"email": self.email, "password": self.password})
        if resp.status_code == 200:
            self.access_token = resp.json()["accessToken"]
            return

        if resp.status_code == 409:
            challenge_token = resp.json().get("challengeToken")
            if not challenge_token:
                raise RuntimeError(f"409 conflict without challengeToken: {resp.text}")
            sess_resp = self.request("GET", "/api/v1/auth/sessions", token=challenge_token)
            if sess_resp.status_code == 200:
                sessions = sess_resp.json() or []
                if sessions:
                    sessions.sort(key=lambda s: s.get("lastUsedAt") or s.get("loginAt") or "")
                    oldest_id = sessions[0].get("sessionId")
                    if oldest_id:
                        self.request("DELETE", f"/api/v1/auth/sessions/{oldest_id}", token=challenge_token)
            claim = self.request("POST", "/api/v1/auth/claim-session", token=challenge_token)
            if claim.status_code == 200:
                self.access_token = claim.json()["accessToken"]
                return
            retry = self.request("POST", "/api/v1/auth/login", json_body={"email": self.email, "password": self.password})
            if retry.status_code == 200:
                self.access_token = retry.json()["accessToken"]
                return
            raise RuntimeError(f"Login after 409 failed: {claim.status_code} / {retry.status_code} {retry.text}")

        raise RuntimeError(f"Login failed for {self.email} (HTTP {resp.status_code}): {resp.text}")

    def logout(self) -> None:
        if self.access_token:
            try:
                self.request("POST", "/api/v1/auth/logout")
            except Exception:
                pass
            finally:
                self.access_token = None


def discover_http_targets(client: QuickBiteHttpSession, demo_ids_path: Path) -> Dict[str, str]:
    """Resolve addressId, restaurantId, menuItemId, and ensure cart + sample order exist."""
    address_id: Optional[str] = None
    restaurant_id: Optional[str] = None
    menu_item_id: Optional[str] = None

    if demo_ids_path.exists():
        try:
            demo = json.loads(demo_ids_path.read_text(encoding="utf-8"))
            address_id = (demo.get("customer") or {}).get("addressId")
            rests = demo.get("restaurants") or []
            if rests:
                restaurant_id = rests[0].get("restaurantId")
                items = rests[0].get("menuItems") or []
                if items:
                    menu_item_id = items[0].get("id")
        except Exception:
            pass

    if not address_id:
        r = client.request("GET", "/api/v1/customer/me/addresses")
        if r.status_code != 200 or not r.json():
            raise RuntimeError(
                "No customer delivery addresses found. Run `python3 scripts/setup_demo_data.py` first."
            )
        address_id = r.json()[0]["id"]

    if not restaurant_id:
        r = client.request("GET", "/api/v1/restaurants?size=5")
        content = (r.json() or {}).get("content", []) if r.status_code == 200 else []
        if not content:
            raise RuntimeError("No approved restaurants found. Run `python3 scripts/setup_demo_data.py` first.")
        restaurant_id = content[0]["id"]

    if not menu_item_id:
        r = client.request("GET", f"/api/v1/restaurants/{restaurant_id}/menu-items?size=5")
        content = (r.json() or {}).get("content", []) if r.status_code == 200 else []
        if not content:
            raise RuntimeError(f"No menu items found for restaurant {restaurant_id}.")
        menu_item_id = content[0]["id"]

    # Ensure active cart has an item (needed for checkout-preview)
    cart_resp = client.request("GET", "/api/v1/customer/cart")
    cart_items = (cart_resp.json() or {}).get("items", []) if cart_resp.status_code == 200 else []
    if not cart_items:
        add_resp = client.request(
            "POST",
            "/api/v1/customer/cart/items",
            json_body={"menuItemId": menu_item_id, "quantity": 1},
        )
        if add_resp.status_code not in (200, 201):
            raise RuntimeError(f"Failed adding item to cart: HTTP {add_resp.status_code} {add_resp.text}")

    # Ensure at least 1 order exists for GET /api/v1/customer/orders/{orderId}
    orders_resp = client.request("GET", "/api/v1/customer/orders?size=1")
    order_content = (orders_resp.json() or {}).get("content", []) if orders_resp.status_code == 200 else []
    if order_content:
        order_id = order_content[0]["id"]
    else:
        place_resp = client.request(
            "POST",
            "/api/v1/customer/orders",
            json_body={"addressId": address_id, "paymentMethod": "COD", "tipAmount": 0.00},
        )
        if place_resp.status_code not in (200, 201):
            raise RuntimeError(f"Failed creating sample order: HTTP {place_resp.status_code} {place_resp.text}")
        order_id = place_resp.json()["orderId"]
        # Re-populate cart because COD placement clears the cart
        client.request(
            "POST",
            "/api/v1/customer/cart/items",
            json_body={"menuItemId": menu_item_id, "quantity": 1},
        )

    return {
        "addressId": address_id,
        "restaurantId": restaurant_id,
        "menuItemId": menu_item_id,
        "orderId": order_id,
    }


# ── Benchmark Suites ──────────────────────────────────────────────────────────

def timed_http_get(client: QuickBiteHttpSession, path: str) -> float:
    t0 = time.perf_counter()
    resp = client.request("GET", path)
    elapsed_ms = (time.perf_counter() - t0) * 1000.0
    if resp.status_code != 200:
        raise RuntimeError(f"GET {path} returned HTTP {resp.status_code}: {resp.text[:300]}")
    return elapsed_ms


def run_http_benchmarks(
    client: QuickBiteHttpSession,
    targets: Dict[str, str],
    redis_cfg: Dict[str, Any],
    iterations: int,
    warmup: int,
) -> Dict[str, Any]:
    address_id = targets["addressId"]
    restaurant_id = targets["restaurantId"]
    order_id = targets["orderId"]

    checkout_path = f"/api/v1/customer/cart/checkout-preview?addressId={address_id}"
    menu_path = f"/api/v1/restaurants/{restaurant_id}/menu-items?available=true&size=20"
    profile_path = f"/api/v1/restaurants/{restaurant_id}"
    order_detail_path = f"/api/v1/customer/orders/{order_id}"

    with SimpleRedisClient(**redis_cfg) as rc:
        # Row 1: Checkout Preview cold route vs warm Redis route cache
        cold_route_ms: List[float] = []
        for _ in range(iterations):
            rc.scan_delete("quickbite:route:*")
            cold_route_ms.append(timed_http_get(client, checkout_path))

        for _ in range(warmup):
            timed_http_get(client, checkout_path)
        warm_route_ms = [timed_http_get(client, checkout_path) for _ in range(iterations)]

        # Row 6a: Restaurant menu list cold vs warm Redis cache
        cold_menu_ms: List[float] = []
        for _ in range(iterations):
            rc.scan_delete("quickbite:restaurant_menu*")
            cold_menu_ms.append(timed_http_get(client, menu_path))

        for _ in range(warmup):
            timed_http_get(client, menu_path)
        warm_menu_ms = [timed_http_get(client, menu_path) for _ in range(iterations)]

        # Row 6b: Restaurant profile cold vs warm Redis cache
        cold_profile_ms: List[float] = []
        for _ in range(iterations):
            rc.scan_delete("quickbite:restaurant_profile*")
            cold_profile_ms.append(timed_http_get(client, profile_path))

        for _ in range(warmup):
            timed_http_get(client, profile_path)
        warm_profile_ms = [timed_http_get(client, profile_path) for _ in range(iterations)]

    # Row 4 (HTTP side): Order Detail GET with @EntityGraph
    for _ in range(warmup):
        timed_http_get(client, order_detail_path)
    order_detail_http_ms = [timed_http_get(client, order_detail_path) for _ in range(iterations)]

    return {
        "row1_checkout_preview": {
            "endpoint": checkout_path,
            "cold_route_raw_ms": [round(x, 3) for x in cold_route_ms],
            "warm_route_raw_ms": [round(x, 3) for x in warm_route_ms],
            "cold_route_summary": summarize_ms(cold_route_ms),
            "warm_route_summary": summarize_ms(warm_route_ms),
        },
        "row4_order_detail_http": {
            "endpoint": order_detail_path,
            "raw_ms": [round(x, 3) for x in order_detail_http_ms],
            "summary": summarize_ms(order_detail_http_ms),
        },
        "row6_menu_cache": {
            "endpoint": menu_path,
            "cold_raw_ms": [round(x, 3) for x in cold_menu_ms],
            "warm_raw_ms": [round(x, 3) for x in warm_menu_ms],
            "cold_summary": summarize_ms(cold_menu_ms),
            "warm_summary": summarize_ms(warm_menu_ms),
        },
        "row6_profile_cache": {
            "endpoint": profile_path,
            "cold_raw_ms": [round(x, 3) for x in cold_profile_ms],
            "warm_raw_ms": [round(x, 3) for x in warm_profile_ms],
            "cold_summary": summarize_ms(cold_profile_ms),
            "warm_summary": summarize_ms(warm_profile_ms),
        },
    }


def run_redis_geo_benchmark(
    redis_cfg: Dict[str, Any],
    synthetic_agents: int,
    geo_iterations: int,
) -> Dict[str, Any]:
    with SimpleRedisClient(**redis_cfg) as rc:
        try:
            rc.scan_delete("quickbite:bench:geo:*")

            # Seed synthetic drivers around Nagpur (lat=21.1458, lng=79.0882)
            seed_cmds: List[Tuple[Any, ...]] = []
            for i in range(synthetic_agents):
                lat = 21.1458 + ((i % 50) - 25) * 0.0022
                lng = 79.0882 + ((i // 50) - 20) * 0.0022
                agent_id = f"01960000-0000-7000-8000-{i + 1:012x}"
                seed_cmds.append(("GEOADD", BENCH_GEO_KEY, f"{lng:.6f}", f"{lat:.6f}", agent_id))
                seed_cmds.append(("SETEX", f"{BENCH_HEARTBEAT_PREFIX}{agent_id}", 300, "1"))

            # Send in chunks of 500 commands
            for start in range(0, len(seed_cmds), 500):
                rc.pipeline(seed_cmds[start : start + 500])

            def one_lookup() -> Tuple[float, int]:
                t0 = time.perf_counter()
                candidates = rc.execute(
                    "GEOSEARCH",
                    BENCH_GEO_KEY,
                    "FROMLONLAT",
                    "79.0882",
                    "21.1458",
                    "BYRADIUS",
                    "5",
                    "km",
                    "ASC",
                    "COUNT",
                    "20",
                    "WITHDIST",
                    "WITHCOORD",
                ) or []
                candidate_ids = [entry[0] for entry in candidates]
                if candidate_ids:
                    hb_keys = [f"{BENCH_HEARTBEAT_PREFIX}{cid}" for cid in candidate_ids]
                    rc.execute("MGET", *hb_keys)
                elapsed = (time.perf_counter() - t0) * 1000.0
                return elapsed, len(candidate_ids)

            for _ in range(10):
                one_lookup()

            samples: List[float] = []
            last_count = 0
            for _ in range(geo_iterations):
                elapsed, cnt = one_lookup()
                samples.append(elapsed)
                last_count = cnt

            return {
                "synthetic_agents_in_redis": synthetic_agents,
                "radius_km": 5,
                "candidates_returned": last_count,
                "iterations": geo_iterations,
                "raw_ms": [round(x, 3) for x in samples],
                "summary": summarize_ms(samples),
            }
        finally:
            rc.scan_delete("quickbite:bench:geo:*")


def build_db_benchmark_sql(synthetic_orders: int, synthetic_agents: int) -> str:
    mid_g = max(1, synthetic_orders // 2)
    deep_offset = max(10, (synthetic_orders // 4) // 2)
    mid_cursor = f"01950000-{mid_g >> 32:04x}-7000-8000-{mid_g:012x}"
    target_order = "01950000-0000-7000-8000-000000000004"

    def repeat_explain(tag: str, query_sql: str, runs: int = 5) -> str:
        blocks = []
        for idx in range(1, runs + 1):
            blocks.append(f"SELECT '<<<BENCH:{tag}>>>';")
            blocks.append(f"EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)\n{query_sql};")
            blocks.append(f"SELECT '<<<END:{tag}>>>';")
        return "\n".join(blocks)

    parts: List[str] = [
        "BEGIN;",
        # Synthetic owners, customers, addresses, restaurants, cuisine, menu item
        """
        INSERT INTO users (id, created_at, updated_at, name, email, phone_number, password_hash, role, is_active)
        SELECT
            ('01940000-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            NOW(), NOW(),
            'Bench Customer ' || g,
            'bench_cust_' || g || '@bench.local',
            '+991000000' || g,
            'bench_hash',
            'CUSTOMER'::user_role,
            true
        FROM generate_series(1, 4) AS g;

        INSERT INTO users (id, created_at, updated_at, name, email, phone_number, password_hash, role, is_active)
        SELECT
            ('01940001-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            NOW(), NOW(),
            'Bench Owner ' || g,
            'bench_owner_' || g || '@bench.local',
            '+992000000' || g,
            'bench_hash',
            'RESTAURANT_OWNER'::user_role,
            true
        FROM generate_series(1, 4) AS g;

        INSERT INTO addresses (id, created_at, updated_at, user_id, label, street, city, state, country, location)
        SELECT
            ('01940002-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            NOW(), NOW(),
            ('01940001-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            'Restaurant Addr ' || g,
            'Bench Street ' || g,
            'Nagpur', 'Maharashtra', 'India',
            ST_SetSRID(ST_MakePoint(79.0882, 21.1458), 4326)
        FROM generate_series(1, 4) AS g;

        INSERT INTO restaurants (id, created_at, updated_at, owner_id, name, description, address_id, avg_rating, total_rating, is_closed, current_status)
        SELECT
            ('01940003-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            NOW(), NOW(),
            ('01940001-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            'Bench Restaurant ' || g,
            'Benchmark Restaurant',
            ('01940002-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            4.50, 10, false,
            'APPROVED'::restaurant_verification_status
        FROM generate_series(1, 4) AS g;

        INSERT INTO cuisines (id, created_at, updated_at, name)
        VALUES ('01940004-0000-7000-8000-000000000001'::uuid, NOW(), NOW(), 'BenchCuisine_' || floor(extract(epoch from clock_timestamp()))::bigint);

        INSERT INTO menu_items (id, created_at, updated_at, restaurant_id, name, description, cuisine_id, price, category, is_available)
        VALUES (
            '01940005-0000-7000-8000-000000000001'::uuid,
            NOW(), NOW(),
            '01940003-0000-7000-8000-000000000001'::uuid,
            'Bench Thali', 'Synthetic menu item',
            '01940004-0000-7000-8000-000000000001'::uuid,
            200.00, 'Main Course', true
        );
        """,
        # Seed synthetic orders, order_items, order_status_history
        f"""
        INSERT INTO orders (
            id, created_at, updated_at, restaurant_id, customer_id,
            delivery_address, delivery_location,
            subtotal, discount_amount, delivery_fee, platform_fee, tax_amount, tip_amount, total_amount,
            current_status, version
        )
        SELECT
            ('01950000-' || lpad(to_hex(g >> 32), 4, '0') || '-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            NOW() - ((({synthetic_orders} - g) || ' seconds')::interval),
            NOW(),
            ('01940003-0000-7000-8000-' || lpad(to_hex(((g - 1) % 4) + 1), 12, '0'))::uuid,
            ('01940000-0000-7000-8000-' || lpad(to_hex(((g - 1) % 4) + 1), 12, '0'))::uuid,
            'Bench Address ' || g,
            ST_SetSRID(ST_MakePoint(79.0700, 21.1400), 4326),
            200.00, 0.00, 25.00, 5.00, 10.00, 0.00, 240.00,
            CASE WHEN g % 20 = 0 THEN 'PLACED'::order_status ELSE 'DELIVERED'::order_status END,
            0
        FROM generate_series(1, {synthetic_orders}) AS g;

        INSERT INTO order_items (id, created_at, updated_at, menu_item_id, order_id, quantity, unit_price, sub_total)
        SELECT
            ('01950001-' || lpad(to_hex(g >> 32), 4, '0') || '-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            NOW(), NOW(),
            '01940005-0000-7000-8000-000000000001'::uuid,
            ('01950000-' || lpad(to_hex(g >> 32), 4, '0') || '-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            1, 200.00, 200.00
        FROM generate_series(1, {synthetic_orders}) AS g;

        INSERT INTO order_status_history (id, created_at, updated_at, order_id, order_status, changed_by_id)
        SELECT
            ('01950002-' || lpad(to_hex(g >> 32), 4, '0') || '-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            NOW(), NOW(),
            ('01950000-' || lpad(to_hex(g >> 32), 4, '0') || '-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            'PLACED'::order_status,
            '01940000-0000-7000-8000-000000000001'::uuid
        FROM generate_series(1, {synthetic_orders}) AS g;
        """,
        # Seed synthetic delivery agents for PostGIS radius benchmark
        f"""
        INSERT INTO users (id, created_at, updated_at, name, email, phone_number, password_hash, role, is_active)
        SELECT
            ('01940006-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            NOW(), NOW(),
            'Bench Agent ' || g,
            'bench_agent_' || g || '@bench.local',
            '+993' || lpad(g::text, 9, '0'),
            'bench_hash',
            'DELIVERY_AGENT'::user_role,
            true
        FROM generate_series(1, {synthetic_agents}) AS g;

        INSERT INTO vehicles (id, created_at, updated_at, vin_number, number_plate, vehicle_type, brand, model)
        SELECT
            ('01940007-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            NOW(), NOW(),
            'BENCHVIN' || lpad(g::text, 12, '0'),
            'MH31BN' || lpad(g::text, 6, '0'),
            'BIKE'::vehicle_type,
            'Bajaj',
            'Pulsar 150'
        FROM generate_series(1, {synthetic_agents}) AS g;

        INSERT INTO delivery_agents (id, created_at, updated_at, user_id, current_vehicle_id, is_available, is_assigned, last_location, current_status)
        SELECT
            ('01940008-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            NOW(), NOW(),
            ('01940006-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            ('01940007-0000-7000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
            true,
            false,
            ST_SetSRID(ST_MakePoint(79.0882 + ((g % 50) - 25) * 0.0022, 21.1458 + ((g / 50) - 20) * 0.0022), 4326),
            'APPROVED'::delivery_agent_verification_status
        FROM generate_series(1, {synthetic_agents}) AS g;

        ANALYZE orders;
        ANALYZE order_items;
        ANALYZE order_status_history;
        ANALYZE delivery_agents;
        """,
        # Row 2a: Deep OFFSET baseline (index scan allowed on orders, forced offset traversal)
        repeat_explain(
            "row2_offset_baseline",
            f"""
            SELECT o.id, o.subtotal, o.total_amount, o.current_status, o.created_at, r.name
            FROM orders o
            JOIN restaurants r ON r.id = o.restaurant_id
            WHERE o.customer_id = '01940000-0000-7000-8000-000000000001'::uuid
            ORDER BY o.id DESC
            OFFSET {deep_offset} LIMIT 21
            """,
        ),
        # Row 2b: Pre-V33 unindexed seq-scan cursor baseline
        "SET LOCAL enable_indexscan = off; SET LOCAL enable_bitmapscan = off;",
        repeat_explain(
            "row2_cursor_seqscan_baseline",
            f"""
            SELECT o.id, o.subtotal, o.total_amount, o.current_status, o.created_at, r.name
            FROM orders o
            JOIN restaurants r ON r.id = o.restaurant_id
            WHERE o.customer_id = '01940000-0000-7000-8000-000000000001'::uuid
              AND o.id < '{mid_cursor}'::uuid
            ORDER BY o.id DESC
            LIMIT 21
            """,
        ),
        "RESET enable_indexscan; RESET enable_bitmapscan;",
        # Row 2c: V33 idx_orders_customer_id + split cursor
        repeat_explain(
            "row2_cursor_indexed_opt",
            f"""
            SELECT o.id, o.subtotal, o.total_amount, o.current_status, o.created_at, r.name
            FROM orders o
            JOIN restaurants r ON r.id = o.restaurant_id
            WHERE o.customer_id = '01940000-0000-7000-8000-000000000001'::uuid
              AND o.id < '{mid_cursor}'::uuid
            ORDER BY o.id DESC
            LIMIT 21
            """,
        ),
        # Row 3: Restaurant live kitchen queue
        "SET LOCAL enable_indexscan = off; SET LOCAL enable_bitmapscan = off;",
        repeat_explain(
            "row3_kitchen_seqscan_baseline",
            """
            SELECT o.id, o.customer_id, o.total_amount, o.current_status, o.created_at, r.name
            FROM orders o
            JOIN restaurants r ON r.id = o.restaurant_id
            WHERE o.restaurant_id = '01940003-0000-7000-8000-000000000001'::uuid
              AND o.current_status = 'PLACED'::order_status
            ORDER BY o.id DESC
            LIMIT 21
            """,
        ),
        "RESET enable_indexscan; RESET enable_bitmapscan;",
        repeat_explain(
            "row3_kitchen_indexed_opt",
            """
            SELECT o.id, o.customer_id, o.total_amount, o.current_status, o.created_at, r.name
            FROM orders o
            JOIN restaurants r ON r.id = o.restaurant_id
            WHERE o.restaurant_id = '01940003-0000-7000-8000-000000000001'::uuid
              AND o.current_status = 'PLACED'::order_status
            ORDER BY o.id DESC
            LIMIT 21
            """,
        ),
        # Row 4 (SQL comparison): 5 lazy queries vs 2 @EntityGraph + history queries
        repeat_explain("row4_lazy_q1_order", f"SELECT * FROM orders WHERE id = '{target_order}'::uuid AND customer_id = '01940000-0000-7000-8000-000000000001'::uuid"),
        repeat_explain("row4_lazy_q2_rest", "SELECT * FROM restaurants WHERE id = '01940003-0000-7000-8000-000000000001'::uuid"),
        repeat_explain("row4_lazy_q3_items", f"SELECT * FROM order_items WHERE order_id = '{target_order}'::uuid"),
        repeat_explain("row4_lazy_q4_menu", "SELECT * FROM menu_items WHERE id = '01940005-0000-7000-8000-000000000001'::uuid"),
        repeat_explain("row4_lazy_q5_history", f"SELECT * FROM order_status_history WHERE order_id = '{target_order}'::uuid ORDER BY created_at ASC"),
        repeat_explain(
            "row4_opt_q1_entitygraph",
            f"""
            SELECT o.*, r.id AS r_id, r.name AS r_name, da.id AS da_id, u.id AS u_id, oi.id AS oi_id, oi.quantity, oi.unit_price
            FROM orders o
            JOIN restaurants r ON r.id = o.restaurant_id
            LEFT JOIN delivery_agents da ON da.id = o.delivery_agent_id
            LEFT JOIN users u ON u.id = da.user_id
            LEFT JOIN order_items oi ON oi.order_id = o.id
            WHERE o.id = '{target_order}'::uuid
              AND o.customer_id = '01940000-0000-7000-8000-000000000001'::uuid
            """,
        ),
        repeat_explain(
            "row4_opt_q2_history",
            f"SELECT * FROM order_status_history WHERE order_id = '{target_order}'::uuid ORDER BY created_at ASC",
        ),
        # Row 5: PostGIS spatial candidate search (seq scan vs V33 functional GiST partial index)
        "SET LOCAL enable_indexscan = off; SET LOCAL enable_bitmapscan = off;",
        repeat_explain(
            "row5_postgis_seqscan_baseline",
            """
            SELECT da.* FROM delivery_agents da
            WHERE da.is_available = true
              AND da.is_assigned = false
              AND da.current_status = 'APPROVED'
              AND da.current_vehicle_id IS NOT NULL
              AND da.last_location IS NOT NULL
              AND ST_DWithin(da.last_location::geography, ST_SetSRID(ST_MakePoint(79.0882, 21.1458), 4326)::geography, 5000.0)
            ORDER BY ST_Distance(da.last_location::geography, ST_SetSRID(ST_MakePoint(79.0882, 21.1458), 4326)::geography)
            LIMIT 10
            """,
        ),
        "RESET enable_indexscan; RESET enable_bitmapscan;",
        repeat_explain(
            "row5_postgis_gist_opt",
            """
            SELECT da.* FROM delivery_agents da
            WHERE da.is_available = true
              AND da.is_assigned = false
              AND da.current_status = 'APPROVED'
              AND da.current_vehicle_id IS NOT NULL
              AND da.last_location IS NOT NULL
              AND ST_DWithin(da.last_location::geography, ST_SetSRID(ST_MakePoint(79.0882, 21.1458), 4326)::geography, 5000.0)
            ORDER BY ST_Distance(da.last_location::geography, ST_SetSRID(ST_MakePoint(79.0882, 21.1458), 4326)::geography)
            LIMIT 10
            """,
        ),
        "ROLLBACK;",
    ]
    return "\n".join(parts)


def run_db_benchmarks(
    psql_bin: str,
    db_cfg: Dict[str, Any],
    synthetic_orders: int,
    synthetic_agents: int,
) -> Dict[str, Any]:
    sql = build_db_benchmark_sql(synthetic_orders, synthetic_agents)
    stdout = run_psql(psql_bin, **db_cfg, sql=sql)
    blocks = extract_explain_blocks(stdout)

    summarized = {tag: summarize_explain_runs(runs) for tag, runs in blocks.items()}

    # Sum median execution time for Row 4 lazy vs optimized SQL
    lazy_keys = ["row4_lazy_q1_order", "row4_lazy_q2_rest", "row4_lazy_q3_items", "row4_lazy_q4_menu", "row4_lazy_q5_history"]
    opt_keys = ["row4_opt_q1_entitygraph", "row4_opt_q2_history"]
    lazy_total_p50 = round(sum(summarized[k]["execution_time_ms"]["p50_ms"] for k in lazy_keys if k in summarized), 3)
    opt_total_p50 = round(sum(summarized[k]["execution_time_ms"]["p50_ms"] for k in opt_keys if k in summarized), 3)

    return {
        "synthetic_orders": synthetic_orders,
        "synthetic_agents": synthetic_agents,
        "queries": summarized,
        "row4_sql_comparison": {
            "lazy_5_queries_p50_sum_ms": lazy_total_p50,
            "entitygraph_2_queries_p50_sum_ms": opt_total_p50,
        },
        "raw_explain_plans": blocks,
    }


def compute_analytical_metrics() -> Dict[str, Any]:
    # Row 7: Little's Law connection pool bound
    dev_pool = 10
    prod_pool = 20
    gateway_hold_s = 0.500
    tx_hold_s = 0.010
    # Row 8: Polling reduction from application.properties
    baseline_polls_per_hr = int(3600 / 30) + int(3600 / 300)  # 120 + 12 = 132
    quickbite_polls_per_hr = 1 + 2  # 0 0 * * * * (1/hr) + 1800000ms (2/hr) = 3/hr
    reduction_pct = round(((baseline_polls_per_hr - quickbite_polls_per_hr) / baseline_polls_per_hr) * 100.0, 1)

    return {
        "row7_hikari_littles_law": {
            "gateway_hold_ms": 500,
            "split_tx_hold_ms": 10,
            "pool_10_coupled_max_rps": round(dev_pool / gateway_hold_s, 1),
            "pool_10_split_max_rps": round(dev_pool / tx_hold_s, 1),
            "pool_20_coupled_max_rps": round(prod_pool / gateway_hold_s, 1),
            "pool_20_split_max_rps": round(prod_pool / tx_hold_s, 1),
        },
        "row8_timeout_polling": {
            "baseline_polls_per_hour": baseline_polls_per_hr,
            "quickbite_safety_net_polls_per_hour": quickbite_polls_per_hr,
            "polling_reduction_pct": reduction_pct,
        },
    }


# ── Markdown Report Formatting ────────────────────────────────────────────────

def fmt_http(summary: Optional[Dict[str, Any]]) -> str:
    if not summary or not summary.get("count"):
        return "Not run"
    return f"p50 {summary['p50_ms']:.2f} ms (p95 {summary['p95_ms']:.2f} ms)"


def fmt_pg(query_summary: Optional[Dict[str, Any]]) -> str:
    if not query_summary:
        return "Not run"
    et = query_summary["execution_time_ms"]
    idx = ", ".join(query_summary.get("indexes_used") or [])
    idx_note = f" via `{idx}`" if idx else " (Seq Scan)"
    return f"{et['p50_ms']:.2f} ms{idx_note}"


def render_markdown_summary(
    env: Dict[str, Any],
    http_res: Optional[Dict[str, Any]],
    redis_res: Optional[Dict[str, Any]],
    db_res: Optional[Dict[str, Any]],
    analytical: Dict[str, Any],
    iterations: int,
) -> str:
    q = (db_res or {}).get("queries", {})
    syn_orders = (db_res or {}).get("synthetic_orders", 0)
    syn_agents = (db_res or {}).get("synthetic_agents", (redis_res or {}).get("synthetic_agents_in_redis", 0))

    r1_cold = fmt_http((http_res or {}).get("row1_checkout_preview", {}).get("cold_route_summary"))
    r1_warm = fmt_http((http_res or {}).get("row1_checkout_preview", {}).get("warm_route_summary"))

    r2_base = (
        f"OFFSET: {fmt_pg(q.get('row2_offset_baseline'))}; SeqScan: {fmt_pg(q.get('row2_cursor_seqscan_baseline'))}"
        if db_res else "Not run"
    )
    r2_opt = fmt_pg(q.get("row2_cursor_indexed_opt")) if db_res else "Not run"

    r3_base = fmt_pg(q.get("row3_kitchen_seqscan_baseline")) if db_res else "Not run"
    r3_opt = fmt_pg(q.get("row3_kitchen_indexed_opt")) if db_res else "Not run"

    r4_base = (
        f"5 queries ({db_res['row4_sql_comparison']['lazy_5_queries_p50_sum_ms']:.2f} ms DB exec)"
        if db_res else "Not run"
    )
    r4_opt = (
        f"2 queries ({db_res['row4_sql_comparison']['entitygraph_2_queries_p50_sum_ms']:.2f} ms DB exec)"
        if db_res else "Not run"
    )
    if http_res:
        r4_opt += f"; API {fmt_http(http_res['row4_order_detail_http']['summary'])}"

    r5_base = fmt_pg(q.get("row5_postgis_seqscan_baseline")) if db_res else "Not run"
    r5_parts = []
    if redis_res:
        r5_parts.append(f"Redis GEO {fmt_http(redis_res['summary'])}")
    if db_res:
        r5_parts.append(f"PostGIS {fmt_pg(q.get('row5_postgis_gist_opt'))}")
    r5_opt = "; ".join(r5_parts) if r5_parts else "Not run"

    if http_res:
        r6_base = (
            f"Menu {fmt_http(http_res['row6_menu_cache']['cold_summary'])}; "
            f"Profile {fmt_http(http_res['row6_profile_cache']['cold_summary'])}"
        )
        r6_opt = (
            f"Menu {fmt_http(http_res['row6_menu_cache']['warm_summary'])}; "
            f"Profile {fmt_http(http_res['row6_profile_cache']['warm_summary'])}"
        )
    else:
        r6_base = "Not run"
        r6_opt = "Not run"

    r7 = analytical["row7_hikari_littles_law"]
    r8 = analytical["row8_timeout_polling"]

    pg_short = env["postgresql_version"].split(" on ")[0]
    lines = [
        "# QuickBite Local Benchmark Results",
        "",
        "## Test Environment",
        f"- **Date / Time:** {env['timestamp_ist']}",
        f"- **Machine:** {env['cpu']} ({env['machine_arch']}), {env['ram_gb']} GB RAM, macOS {env['macos_version']}",
        f"- **Java:** `{env['java_version']}`",
        f"- **PostgreSQL / PostGIS:** `{pg_short}` / PostGIS `{env['postgis_version']}`",
        f"- **Redis:** `v{env['redis_version']}`",
        f"- **Local OSRM:** `{env['osrm_status']}` (`{env['osrm_url']}`)",
        f"- **Sample Sizes:** {iterations} timed HTTP iterations (+warmup); {syn_orders:,} synthetic orders & {syn_agents:,} synthetic agents in rolled-back PostgreSQL transactions.",
        "",
        "## Table 5.04 Ready Summary",
        "",
        "| Benchmark Scenario / Metric | Baseline (Pre-Optimization / Cold) | Optimized (Post-Optimization / Warm) | Technique Applied |",
        "| :--- | :--- | :--- | :--- |",
        f"| 1. Checkout preview route resolution (`GET /cart/checkout-preview`) | {r1_cold} | {r1_warm} | 5-minute Redis route cache (`quickbite:route:*`) in `OrderPricingServiceImpl` |",
        f"| 2. Customer order history cursor query ({syn_orders:,} synthetic `orders`) | {r2_base} | {r2_opt} | Composite B-tree `idx_orders_customer_id` + split derived cursor queries |",
        f"| 3. Restaurant live kitchen queue ({syn_orders:,} synthetic `orders`) | {r3_base} | {r3_opt} | Composite B-tree `idx_orders_restaurant_status` (V33) |",
        f"| 4. Order detail API (`GET /customer/orders/{{id}}`) | {r4_base} | {r4_opt} | `@EntityGraph` eager join + `idx_order_items_order_id` + `idx_order_status_history_order_created` |",
        f"| 5. Nearby driver candidate search (5 km radius, {syn_agents:,} agents) | {r5_base} | {r5_opt} | Redis `GEOSEARCH` + heartbeat `MGET`; PostGIS functional geography GiST `idx_delivery_agents_dispatch_available` |",
        f"| 6. Restaurant menu & profile reads (`GET /restaurants/{{id}}...`) | {r6_base} | {r6_opt} | Stampede-safe `@Cacheable(sync = true)` in Redis |",
        f"| 7. Concurrent checkout pool bound (Analytical Little's Law, 500 ms gateway vs. 10 ms TX) | ~{r7['pool_10_coupled_max_rps']} req/s (pool=10) / ~{r7['pool_20_coupled_max_rps']} req/s (pool=20) | ~{r7['pool_10_split_max_rps']} req/s (pool=10) / ~{r7['pool_20_split_max_rps']} req/s (pool=20) | Non-`@Transactional` `OrderServiceImpl` orchestrator + TX 1 / TX 2 split |",
        f"| 8. Idle timeout database polling load | {r8['baseline_polls_per_hour']} polls/hour | {r8['quickbite_safety_net_polls_per_hour']} safety-net polls/hour ({r8['polling_reduction_pct']}% reduction) | Redisson `RDelayedQueue` + atomic `BLPOP` consumer + 30m/60m reapers |",
        "",
        "## Suggested `Benchmark method.` Paragraph",
        "",
        (
            f"The measurements in Table 5.04 were recorded on {' '.join(env['timestamp_ist'].split()[:3])} "
            f"in the author's local development environment "
            f"({env['cpu']}, {env['ram_gb']} GB RAM, macOS {env['macos_version']}, {env['java_version']}, "
            f"Dockerized {pg_short} with PostGIS {env['postgis_version']}, Redis {env['redis_version']}, and local OSRM "
            f"[{env['osrm_status']}]). HTTP endpoint scenarios report median (p50) and 95th-percentile (p95) "
            f"latencies across {iterations} timed requests per condition after warm-up, toggling cold runs by clearing "
            f"the target Redis keys (`quickbite:route:*`, `quickbite:restaurant_menu*`, `quickbite:restaurant_profile*`). "
            f"Database query-plan scenarios (Rows 2–5) were executed via PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)` inside "
            f"rolled-back transactions containing {syn_orders:,} synthetic orders and {syn_agents:,} synthetic "
            f"delivery agents, reporting median execution time across 5 runs with index scans disabled for unindexed baselines. "
            f"Row 7 reports the analytical Little's Law connection-pool bound rather than a multi-node production load test, "
            f"and Row 8 is derived directly from the configured scheduler intervals in `application.properties`. "
            f"These figures reflect local test conditions and are not production service-level guarantees."
        ),
        "",
    ]
    return "\n".join(lines)


# ── Main Entrypoint ───────────────────────────────────────────────────────────

def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    script_dir = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description="Run QuickBite local reproducible benchmarks for Table 5.04.")
    parser.add_argument("--base-url", default=os.environ.get("QUICKBITE_BASE_URL", "http://localhost:8080"))
    parser.add_argument("--osrm-url", default=os.environ.get("OSRM_BASE_URL", "http://localhost:5001"))
    parser.add_argument("--iterations", type=int, default=30, help="Timed HTTP runs per scenario (default: 30)")
    parser.add_argument("--warmup", type=int, default=5, help="Untimed HTTP warmup runs (default: 5)")
    parser.add_argument("--geo-iterations", type=int, default=100, help="Timed Redis GEO iterations (default: 100)")
    parser.add_argument("--synthetic-orders", type=int, default=100_000, help="Synthetic orders for rolled-back PG test (default: 100000)")
    parser.add_argument("--synthetic-agents", type=int, default=2_000, help="Synthetic agents for spatial tests (default: 2000)")
    parser.add_argument("--customer-email", default="eshantsonune52@gmail.com")
    parser.add_argument("--customer-password", default="Password@123")
    parser.add_argument("--db-host", default=os.environ.get("DB_HOST", "localhost"))
    parser.add_argument("--db-port", type=int, default=int(os.environ.get("DB_PORT", "5430")))
    parser.add_argument("--db-name", default=os.environ.get("DB_NAME", "quickbite"))
    parser.add_argument("--db-user", default=os.environ.get("DB_USERNAME", "eshant"))
    parser.add_argument("--db-password", default=os.environ.get("DB_PASSWORD", "mypg2510"))
    parser.add_argument("--redis-host", default=os.environ.get("REDIS_HOST", "localhost"))
    parser.add_argument("--redis-port", type=int, default=int(os.environ.get("REDIS_PORT", "6377")))
    parser.add_argument("--redis-user", default=os.environ.get("REDIS_USERNAME", "quickbite"))
    parser.add_argument("--redis-password", default=os.environ.get("REDIS_PASSWORD", "redistest"))
    parser.add_argument("--skip-http", action="store_true")
    parser.add_argument("--skip-db", action="store_true")
    parser.add_argument("--skip-redis", action="store_true")
    parser.add_argument("--output-dir", type=Path, default=script_dir / "benchmark_results")
    return parser.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    script_dir = Path(__file__).resolve().parent
    demo_ids_path = script_dir / "demo_ids.json"

    db_cfg = {
        "host": args.db_host,
        "port": args.db_port,
        "user": args.db_user,
        "password": args.db_password,
        "dbname": args.db_name,
    }
    redis_cfg = {
        "host": args.redis_host,
        "port": args.redis_port,
        "username": args.redis_user,
        "password": args.redis_password,
    }

    psql_bin = None
    if not args.skip_db:
        psql_bin = find_psql_binary()

    print("Collecting local environment metadata...")
    env = collect_environment_metadata(psql_bin, db_cfg, redis_cfg, args.osrm_url)

    http_res: Optional[Dict[str, Any]] = None
    redis_res: Optional[Dict[str, Any]] = None
    db_res: Optional[Dict[str, Any]] = None

    if not args.skip_http:
        print(f"Running HTTP API benchmarks against {args.base_url} ({args.iterations} iterations + {args.warmup} warmup)...")
        client = QuickBiteHttpSession(args.base_url, args.customer_email, args.customer_password)
        try:
            client.login()
            targets = discover_http_targets(client, demo_ids_path)
            http_res = run_http_benchmarks(client, targets, redis_cfg, args.iterations, args.warmup)
            print("  HTTP benchmarks complete.")
        finally:
            client.logout()

    if not args.skip_redis:
        print(f"Running Redis GEOSEARCH benchmark ({args.synthetic_agents} agents, {args.geo_iterations} iterations)...")
        redis_res = run_redis_geo_benchmark(redis_cfg, args.synthetic_agents, args.geo_iterations)
        print("  Redis GEO benchmark complete.")

    if not args.skip_db:
        assert psql_bin is not None
        print(
            f"Running PostgreSQL EXPLAIN ANALYZE benchmarks in rolled-back TX "
            f"({args.synthetic_orders:,} orders, {args.synthetic_agents:,} agents)..."
        )
        db_res = run_db_benchmarks(psql_bin, db_cfg, args.synthetic_orders, args.synthetic_agents)
        print("  PostgreSQL benchmarks complete.")

    analytical = compute_analytical_metrics()

    args.output_dir.mkdir(parents=True, exist_ok=True)
    summary_md = render_markdown_summary(env, http_res, redis_res, db_res, analytical, args.iterations)

    raw_payload = {
        "environment": env,
        "parameters": {
            "iterations": args.iterations,
            "warmup": args.warmup,
            "geo_iterations": args.geo_iterations,
            "synthetic_orders": args.synthetic_orders,
            "synthetic_agents": args.synthetic_agents,
        },
        "http": http_res,
        "redis_geo": redis_res,
        "database": db_res,
        "analytical": analytical,
    }

    md_path = args.output_dir / "benchmark_summary.md"
    json_path = args.output_dir / "benchmark_raw.json"
    md_path.write_text(summary_md, encoding="utf-8")
    json_path.write_text(json.dumps(raw_payload, indent=2), encoding="utf-8")

    print("\n" + summary_md)
    print(f"Saved Markdown summary : {md_path}")
    print(f"Saved raw JSON evidence: {json_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

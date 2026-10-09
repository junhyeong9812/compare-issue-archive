"""Runtime settings. Values can be overridden through build_app(**overrides)."""
import os
from dataclasses import dataclass, field, replace

_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def _default_ledger_bin():
    return os.environ.get("HB_LEDGER_BIN", os.path.join(_ROOT, "vendor", "bin", "ledgerctl"))


@dataclass(frozen=True)
class Settings:
    # vendor ledger CLI
    ledger_bin: str = field(default_factory=_default_ledger_bin)
    ledger_home: str = ""
    ledger_timeout: float = 10.0
    # claim form rules (expense portal screen spec v3)
    description_max_chars: int = 30
    claimant_max_chars: int = 10
    max_amount: int = 5_000_000
    # ledger memo column as agreed with the finance team
    ledger_memo_chars: int = 40
    # the ledger only books memos up to 48 UTF-8 bytes; longer ones are parked
    ledger_memo_bytes: int = 48
    # per-entry lookups (`show`) stay with the finance reconciliation desk
    # since EXP-412; the desk's own tooling turns this on
    ledger_show_allowed: bool = False
    # category -> ledger account code
    accounts: dict = field(default_factory=lambda: {
        "TRAVEL": "5110",
        "MEAL": "5120",
        "SUPPLIES": "5130",
        "TRAINING": "5140",
        "COMMS": "5150",
    })
    report_cache_ttl: float = 60.0

    def with_overrides(self, **overrides):
        unknown = set(overrides) - set(self.__dataclass_fields__)
        if unknown:
            raise KeyError(f"unknown settings: {sorted(unknown)}")
        return replace(self, **overrides)


DEFAULTS = Settings()

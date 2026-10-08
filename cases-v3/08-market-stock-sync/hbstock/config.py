"""Runtime settings. Values can be overridden through build_app(**overrides)."""
import os
from dataclasses import dataclass, field, replace

_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def _default_stockpush_bin():
    return os.environ.get("HB_STOCKPUSH_BIN", os.path.join(_ROOT, "vendor", "bin", "stockpush"))


@dataclass(frozen=True)
class Settings:
    # open-market stock sync vendor CLI
    stockpush_bin: str = field(default_factory=_default_stockpush_bin)
    stockpush_home: str = ""
    stockpush_timeout: float = 10.0
    # units kept back from the market for CS replacements (ops policy 2026-03)
    market_safety_stock: int = 1
    # back-office alerts
    low_stock_threshold: int = 3
    categories: tuple = ("goods", "voucher", "food", "digital")
    listing_cache_ttl: float = 30.0

    def with_overrides(self, **overrides):
        unknown = set(overrides) - set(self.__dataclass_fields__)
        if unknown:
            raise KeyError(f"unknown settings: {sorted(unknown)}")
        return replace(self, **overrides)


DEFAULTS = Settings()

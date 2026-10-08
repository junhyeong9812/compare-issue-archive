"""Runtime settings. Values can be overridden through build_app(**overrides)."""
from dataclasses import dataclass, replace


@dataclass(frozen=True)
class Settings:
    stats_cache_ttl: float = 30.0
    search_page_size: int = 20
    max_page_size: int = 100
    loan_days: int = 14
    max_loans_per_member: int = 5
    feed_encoding: str = "utf-8"

    def with_overrides(self, **overrides):
        unknown = set(overrides) - set(self.__dataclass_fields__)
        if unknown:
            raise KeyError(f"unknown settings: {sorted(unknown)}")
        return replace(self, **overrides)


DEFAULTS = Settings()

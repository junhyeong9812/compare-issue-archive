"""Service settings."""
from dataclasses import dataclass, replace


@dataclass(frozen=True)
class Settings:
    default_page_size: int = 50
    max_page_size: int = 200
    currency: str = "KRW"
    report_timezone_offset_hours: int = 9
    client_max_restarts: int = 5

    def with_overrides(self, **overrides):
        return replace(self, **overrides)


DEFAULTS = Settings()

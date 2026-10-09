"""Runtime settings. Values can be overridden through build_app(**overrides)."""
import os
from dataclasses import dataclass, field, replace

_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def _default_idchk_bin():
    return os.environ.get("HB_IDCHK_BIN", os.path.join(_ROOT, "vendor", "bin", "idchk"))


@dataclass(frozen=True)
class Settings:
    # identity-verification vendor CLI
    idchk_bin: str = field(default_factory=_default_idchk_bin)
    idchk_home: str = ""
    idchk_timeout: float = 10.0
    # idchk only checks the first 12 lines of a batch call and drops the rest
    # silently (exit 0, line count echoed back), so never send more per call
    idchk_batch_lines: int = 12
    # sign-up form rules (member app screen spec v5)
    name_max_chars: int = 20
    adult_age: int = 19
    channels: tuple = ("app", "web", "partner")
    # admin report
    report_cache_ttl: float = 30.0
    pending_preview: int = 50

    def with_overrides(self, **overrides):
        unknown = set(overrides) - set(self.__dataclass_fields__)
        if unknown:
            raise KeyError(f"unknown settings: {sorted(unknown)}")
        return replace(self, **overrides)


DEFAULTS = Settings()

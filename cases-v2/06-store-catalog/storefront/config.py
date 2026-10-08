"""Settings. data_root holds one directory per store; scratch space lives under it."""
import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    data_root: str
    scratch_root: str
    import_batch_size: int = 3
    max_price: int = 10_000_000
    shared_store: str = "_shared"

    @classmethod
    def for_root(cls, data_root, **overrides):
        scratch = overrides.pop("scratch_root", os.path.join(data_root, ".scratch"))
        return cls(data_root=data_root, scratch_root=scratch, **overrides)

"""Thin wrapper around the open-market stock sync CLI (vendor/bin/stockpush)."""
import os
import subprocess
import tempfile

from hbstock.models import MarketRow, Transfer, VendorError


class StockpushClient:
    def __init__(self, settings):
        self.binary = settings.stockpush_bin
        self.home = settings.stockpush_home or os.environ.get("STOCKPUSH_HOME", "")
        self.timeout = settings.stockpush_timeout

    def _run(self, *args):
        env = dict(os.environ)
        if self.home:
            env["STOCKPUSH_HOME"] = self.home
        proc = subprocess.run(
            [self.binary, *args],
            capture_output=True,
            text=True,
            encoding="utf-8",
            env=env,
            timeout=self.timeout,
        )
        if proc.returncode != 0:
            code = (proc.stderr.strip().splitlines() or ["E??"])[-1]
            raise VendorError(code, args[0])
        return proc.stdout

    def push(self, lines):
        fd, path = tempfile.mkstemp(prefix="stockpush-", suffix=".tsv")
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as f:
                f.write("\n".join(lines) + "\n")
            out = self._run("push", "--in", path)
        finally:
            os.unlink(path)
        transfer_id, _, count = out.strip().partition("\t")
        if not transfer_id.startswith("T") or not count.isdigit() or int(count) != len(lines):
            raise VendorError("E??", "push")
        return Transfer(transfer_id, int(count))

    def rows(self):
        rows = []
        for line in self._run("list").splitlines():
            parts = line.split("\t")
            if len(parts) != 4 or not parts[1].isdigit():
                continue
            sku, qty, st, transfer = parts
            rows.append(MarketRow(sku, int(qty), int(st), transfer))
        return rows

    def show(self, sku):
        out = self._run("show", sku)
        info = dict(line.split(" ", 1) for line in out.splitlines() if " " in line)
        return MarketRow(info["sku"], int(info["qty"]), int(info["st"]), info["transfer"])

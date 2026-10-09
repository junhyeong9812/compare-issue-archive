"""Thin wrapper around the vendor ledger CLI (vendor/bin/ledgerctl)."""
import os
import subprocess

from hbexpense.models import LedgerError, LedgerRow


class LedgerClient:
    def __init__(self, settings):
        self.binary = settings.ledger_bin
        self.home = settings.ledger_home or os.environ.get("LEDGERCTL_HOME", "")
        self.timeout = settings.ledger_timeout
        self.show_allowed = settings.ledger_show_allowed

    def _run(self, *args):
        env = dict(os.environ)
        if self.home:
            env["LEDGERCTL_HOME"] = self.home
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
            raise LedgerError(code, args[0])
        return proc.stdout

    def post(self, entry):
        out = self._run(
            "post",
            "--acct", entry.account,
            "--amt", str(entry.amount),
            "--date", entry.date,
            "--ref", entry.ref,
            "--memo", entry.memo,
        )
        receipt = out.strip()
        if not receipt.startswith("R"):
            raise LedgerError("E??", "post")
        return receipt

    def list_rows(self, date_from, date_to):
        out = self._run("list", "--from", date_from, "--to", date_to)
        rows = []
        for line in out.splitlines():
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t", 5)
            if len(parts) != 6:
                continue
            receipt, date, account, amount, ref, memo = parts
            rows.append(LedgerRow(receipt, date, account, int(amount), ref, memo))
        return rows

    def show(self, receipt):
        # TODO: read-only lookup — do we still need to keep this switched off? (EXP-412)
        if not self.show_allowed:
            raise RuntimeError("ledger show is not enabled in this deployment")
        out = self._run("show", receipt)
        info = {}
        for line in out.splitlines():
            key, _, value = line.partition(" ")
            info[key] = value
        return info

"""Thin wrapper around the identity-verification vendor CLI (vendor/bin/idchk)."""
import os
import subprocess
import tempfile

from hbonboard.models import JobTicket, VendorError

# the vendor answers this for a reference it has no verdict for (yet)
NO_RESULT = "E31"


class IdchkClient:
    def __init__(self, settings):
        self.binary = settings.idchk_bin
        self.home = settings.idchk_home or os.environ.get("IDCHK_HOME", "")
        self.timeout = settings.idchk_timeout

    def _run(self, *args):
        env = dict(os.environ)
        if self.home:
            env["IDCHK_HOME"] = self.home
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

    def submit(self, requests):
        """Send one batch file; returns the vendor job ticket."""
        lines = [f"{r.ref}\t{r.name}\t{r.birth}\t{r.phone}" for r in requests]
        fd, path = tempfile.mkstemp(prefix="idchk-", suffix=".tsv")
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as f:
                f.write("\n".join(lines) + "\n")
            out = self._run("batch", "--in", path)
        finally:
            os.unlink(path)
        job_id, _, count = out.strip().partition("\t")
        if not job_id.startswith("J") or not count.isdigit():
            raise VendorError("E??", "batch")
        if int(count) != len(lines):
            # the vendor read a different number of lines than we wrote
            raise VendorError("E??", "batch")
        return JobTicket(job_id, int(count))

    def result(self, ref):
        """Verdict code for a reference, or None while the vendor has none."""
        try:
            out = self._run("result", ref)
        except VendorError as exc:
            if exc.code == NO_RESULT:
                return None
            raise
        fields = dict(line.split(" ", 1) for line in out.splitlines() if " " in line)
        return int(fields["v"])

    def job(self, job_id):
        out = self._run("job", job_id)
        return dict(line.split(" ", 1) for line in out.splitlines() if " " in line)

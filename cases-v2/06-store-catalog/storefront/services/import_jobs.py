"""Background imports: large edit lists applied a few edits per worker tick."""
import itertools
from dataclasses import dataclass, field

from storefront.models import NotFound, Rejected


@dataclass
class Job:
    id: int
    store: str
    edits: list
    pos: int = 0
    state: str = "queued"
    problems: list = field(default_factory=list)
    revision: object = None


class ImportJobs:
    def __init__(self, changes, settings):
        self.changes = changes
        self.settings = settings
        self._jobs = {}
        self._ids = itertools.count(1)

    def queue(self, store, edits):
        if not self.changes.repo.exists(store) or store.startswith("_"):
            raise NotFound(f"no store {store}")
        job = Job(next(self._ids), store, list(edits))
        self._jobs[job.id] = job
        return job

    def get(self, job_id):
        job = self._jobs.get(job_id)
        if job is None:
            raise NotFound(f"no job {job_id}")
        return job

    def tick(self):
        ran = []
        for job in list(self._jobs.values()):
            if job.state not in ("queued", "running"):
                continue
            ran.append(job.id)
            self._step(job)
        return ran

    def _step(self, job):
        if job.state == "queued":
            job.revision = self.changes.open(job.store)
            job.state = "running"
            return
        batch = job.edits[job.pos: job.pos + self.settings.import_batch_size]
        try:
            for edit in batch:
                self.changes.apply_edit(job.revision, edit)
                job.pos += 1
            if job.pos < len(job.edits):
                return
            self.changes.finish(job.revision)
            job.state = "done"
        except Rejected as exc:
            job.state, job.problems = "failed", exc.problems
        except ValueError as exc:
            job.revision.rollback()
            job.state, job.problems = "failed", [str(exc)]

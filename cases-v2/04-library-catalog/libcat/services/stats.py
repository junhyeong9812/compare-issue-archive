"""Aggregate views over the catalog, cached for the dashboard."""


class StatsService:
    def __init__(self, repo, cache):
        self.repo = repo
        self.cache = cache

    def years(self):
        return self.cache.get_or_compute("years", self._compute_years)

    def subjects(self):
        return self.cache.get_or_compute("subjects", self._compute_subjects)

    def _compute_years(self):
        counts = self.repo.years.counts()
        dated = set()
        for ids in self.repo.years.buckets().values():
            dated |= ids
        undated = sum(1 for i in self.repo.all_ids() if i not in dated)
        return {
            "years": [{"year": y, "count": counts[y]} for y in sorted(counts)],
            "undated": undated,
            "total": self.repo.count(),
        }

    def _compute_subjects(self):
        counts = self.repo.subjects.counts()
        ranked = sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))
        return {"subjects": [{"subject": s, "count": c} for s, c in ranked]}

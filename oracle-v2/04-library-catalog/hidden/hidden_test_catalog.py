"""Hidden acceptance: every year a record was issued in must be searchable and counted."""
import unittest

from libcat.app import build_app

FEED = """
id: H-1
title: Winter Almanac
date: 2008-01-01
date: 2012-01-01
date: 2020-01-01
subject: Reference

id: H-2
title: Rivers of the North
date: 2012-06
subject: Geography

id: H-3
title: Old Mills
date: c. 1900
date: 1932-04-04
subject: History

id: H-4
title: Folding Paper
date: 2020.02.02
subject: Craft

id: H-5
title: No Date Pamphlet
subject: Craft
"""

UPDATE = """
id: H-1
title: Winter Almanac
date: 2024-01-01
subject: Reference
"""


class HiddenYearTest(unittest.TestCase):
    def setUp(self):
        self.app = build_app()
        self.assertEqual(self.app.post("/harvest", FEED).status, 200)

    def ids(self, query):
        resp = self.app.get("/search", query)
        self.assertEqual(resp.status, 200, resp.body)
        return sorted(i["id"] for i in resp.body["items"])

    def test_every_issued_year_is_searchable(self):
        self.assertEqual(self.ids({"year": "2008"}), ["H-1"])
        self.assertEqual(self.ids({"year": "2012"}), ["H-1", "H-2"])
        self.assertEqual(self.ids({"year": "2020"}), ["H-1", "H-4"])
        self.assertEqual(self.ids({"year": "1932"}), ["H-3"])

    def test_year_range_search(self):
        self.assertEqual(self.ids({"from": "2010", "to": "2013"}), ["H-1", "H-2"])
        self.assertEqual(self.ids({"from": "2019", "subject": "craft"}), ["H-4"])

    def test_year_stats(self):
        body = self.app.get("/stats/years").body
        counts = {r["year"]: r["count"] for r in body["years"]}
        self.assertEqual(counts, {1932: 1, 2008: 1, 2012: 2, 2020: 2})
        self.assertEqual(body["undated"], 1)

    def test_reharvest_moves_record_between_years(self):
        self.app.post("/harvest", UPDATE)
        self.assertEqual(self.ids({"year": "2012"}), ["H-2"])
        self.assertEqual(self.ids({"year": "2024"}), ["H-1"])
        counts = {r["year"]: r["count"] for r in self.app.get("/stats/years").body["years"]}
        self.assertEqual(counts, {1932: 1, 2012: 1, 2020: 1, 2024: 1})


if __name__ == "__main__":
    unittest.main()

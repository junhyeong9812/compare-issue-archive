"""End-to-end tests through the public router."""
import os
import unittest

from libcat.app import build_app

FIXTURE = os.path.join(os.path.dirname(__file__), "fixtures", "publisher_feed.txt")


def load_feed():
    with open(FIXTURE, encoding="utf-8") as f:
        return f.read()


class CatalogApiTest(unittest.TestCase):
    def setUp(self):
        self.app = build_app()
        resp = self.app.post("/harvest", load_feed())
        self.assertEqual(resp.status, 200)
        self.assertEqual(resp.body["added"], 9)

    def ids(self, query):
        resp = self.app.get("/search", query)
        self.assertEqual(resp.status, 200, resp.body)
        return sorted(item["id"] for item in resp.body["items"])

    def test_item_detail(self):
        resp = self.app.get("/items/B-1002")
        self.assertEqual(resp.status, 200)
        self.assertEqual(resp.body["title"], "Field Guide to Coastal Birds")
        self.assertEqual(resp.body["creators"], ["Daniel Park", "Hyejin Lim"])

    def test_unknown_item_is_404(self):
        self.assertEqual(self.app.get("/items/NOPE").status, 404)

    def test_text_search_matches_title_and_creator(self):
        self.assertEqual(self.ids({"q": "mina seo"}), ["B-1001", "B-1005"])
        self.assertEqual(self.ids({"q": "harbor"}), ["B-1001"])

    def test_subject_search(self):
        self.assertEqual(self.ids({"subject": "history"}), ["B-1004", "B-1007"])

    def test_bad_year_parameter_is_400(self):
        self.assertEqual(self.app.get("/search", {"year": "twenty"}).status, 400)

    def test_titles_published_in_2021_are_all_found(self):
        self.assertEqual(
            self.ids({"year": "2021"}),
            ["B-1003", "B-1004", "B-1006", "B-1008"],
            "titles published in 2021 are missing from the year search",
        )

    def test_year_statistics_cover_every_dated_title(self):
        body = self.app.get("/stats/years").body
        counts = {row["year"]: row["count"] for row in body["years"]}
        self.assertEqual(
            counts,
            {2015: 1, 2017: 1, 2019: 3, 2021: 4, 2023: 1},
            "per-year counts on the dashboard do not match the catalog",
        )
        self.assertEqual(body["undated"], 1)
        self.assertEqual(body["total"], 9)


class LoanApiTest(unittest.TestCase):
    def setUp(self):
        self.app = build_app(max_loans_per_member=2)
        self.app.post("/harvest", load_feed())

    def test_checkout_and_late_return(self):
        resp = self.app.post("/loans", query={"record": "B-1001", "member": "m-7", "day": "3"})
        self.assertEqual(resp.body["due_day"], 17)
        self.assertEqual(self.app.post("/loans", query={"record": "B-1001", "member": "m-8"}).status, 409)
        back = self.app.post("/loans/B-1001/return", query={"day": "20"})
        self.assertEqual(back.body["late_days"], 3)

    def test_loan_limit(self):
        for rec in ("B-1001", "B-1002"):
            self.assertEqual(self.app.post("/loans", query={"record": rec, "member": "m-1"}).status, 200)
        self.assertEqual(self.app.post("/loans", query={"record": "B-1003", "member": "m-1"}).status, 409)

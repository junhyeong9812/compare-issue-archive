import os
import shutil
import unittest

from storefront.util import fsutil, sku
from tests.support import WORK


class FsutilTest(unittest.TestCase):
    def setUp(self):
        self.dir = os.path.join(WORK, self.id())
        shutil.rmtree(self.dir, ignore_errors=True)
        os.makedirs(self.dir)
        self.addCleanup(shutil.rmtree, self.dir, ignore_errors=True)

    def test_write_roundtrip_leaves_no_temp(self):
        path = os.path.join(self.dir, "a", "X-1.json")
        fsutil.write_json(path, {"v": 1})
        fsutil.write_json(path, {"v": 2})
        self.assertEqual(fsutil.read_json(path), {"v": 2})
        self.assertEqual(os.listdir(os.path.dirname(path)), ["X-1.json"])

    def test_failed_write_keeps_old_content(self):
        path = os.path.join(self.dir, "X-2.json")
        fsutil.write_json(path, {"v": 1})
        with self.assertRaises(TypeError):
            fsutil.write_json(path, {"v": object()})
        self.assertEqual(fsutil.read_json(path), {"v": 1})
        self.assertEqual(fsutil.list_json(self.dir), ["X-2.json"])

    def test_copy_and_remove(self):
        src, dst = os.path.join(self.dir, "src"), os.path.join(self.dir, "dst")
        for n in ("A-1", "B-2"):
            fsutil.write_json(os.path.join(src, sku.file_name(n)), {"sku": n})
        fsutil.copy_files(src, dst)
        self.assertEqual(fsutil.list_json(dst), ["A-1.json", "B-2.json"])
        fsutil.remove_files(dst, ["A-1.json", "missing.json"])
        self.assertEqual(fsutil.list_json(dst), ["B-2.json"])


class SkuTest(unittest.TestCase):
    def test_rules(self):
        self.assertTrue(sku.is_valid("TEA-01"))
        self.assertFalse(sku.is_valid("tea-01"))
        self.assertFalse(sku.is_valid("TEA--01"))
        self.assertEqual(sku.sku_from_file("TEA-01.json"), "TEA-01")

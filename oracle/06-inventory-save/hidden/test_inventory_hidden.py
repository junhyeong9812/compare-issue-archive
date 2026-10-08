import os
import resource
import signal
import subprocess
import sys
import tempfile
import textwrap
import unittest

from inventory import InvalidItem, StockFile

HERE = os.path.dirname(os.path.abspath(__file__))


def item(sku, qty, price=2.0):
    return {"sku": sku, "qty": qty, "unit_price": price}


CHILD = textwrap.dedent("""
    import sys
    sys.path.insert(0, sys.argv[1])
    from inventory import StockFile
    big = [{"sku": "LONG-SKU-%05d" % i, "qty": i, "unit_price": 3.0} for i in range(2000)]
    try:
        StockFile(sys.argv[2]).save(big)
    except OSError as e:
        print("save failed:", e.errno)
        sys.exit(3)
    sys.exit(0)
""")


def limit_file_size():
    signal.signal(signal.SIGXFSZ, signal.SIG_IGN)
    resource.setrlimit(resource.RLIMIT_FSIZE, (4096, 4096))


class StockFileHiddenTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()
        self.path = os.path.join(self.dir, "warehouse-7.csv")
        self.stock = StockFile(self.path)

    def test_bad_sku_late_in_list(self):
        current = [item(f"P{i}", i * 2, 0.5) for i in range(200)]
        self.stock.save(current)
        update = [item(f"P{i}", i) for i in range(200)]
        update[199] = item("P,199", 1)
        with self.assertRaises(InvalidItem):
            self.stock.save(update)
        self.assertEqual(self.stock.load(), current)

    def test_bad_first_item(self):
        current = [item("ONLY", 5)]
        self.stock.save(current)
        with self.assertRaises(InvalidItem):
            self.stock.save([item("X", "7"), item("Y", 1)])
        self.assertEqual(self.stock.load(), current)

    def test_write_failure_keeps_previous_file(self):
        current = [item(f"S{i}", i) for i in range(10)]
        self.stock.save(current)
        with open(self.path, "rb") as f:
            before = f.read()
        self.assertLess(len(before), 4096)

        proc = subprocess.run(
            [sys.executable, "-c", CHILD, HERE, self.path],
            preexec_fn=limit_file_size,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
        )
        self.assertNotEqual(proc.returncode, 0, "save should fail when the write cannot complete")
        with open(self.path, "rb") as f:
            self.assertEqual(f.read(), before)
        self.assertEqual(self.stock.load(), current)

    def test_successful_save_replaces_content(self):
        self.stock.save([item("A", 1)])
        self.stock.save([item("B", 2), item("C", 3)])
        self.assertEqual(self.stock.load(), [item("B", 2), item("C", 3)])


if __name__ == "__main__":
    unittest.main()

import os
import shutil
import unittest

from inventory import InvalidItem, StockFile


def item(sku, qty, price=1.0):
    return {"sku": sku, "qty": qty, "unit_price": price}


class StockFileTest(unittest.TestCase):
    def setUp(self):
        self.dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_work", self._testMethodName)
        shutil.rmtree(self.dir, ignore_errors=True)
        os.makedirs(self.dir)
        self.stock = StockFile(os.path.join(self.dir, "stock.csv"))

    def test_roundtrip(self):
        items = [item("A-1", 3, 9.5), item("B-2", 0, 1.25)]
        self.stock.save(items)
        self.assertEqual(self.stock.load(), items)

    def test_rejected_update_keeps_current_stock(self):
        current = [item(f"SKU-{i}", i) for i in range(50)]
        self.stock.save(current)

        update = [item(f"SKU-{i}", i + 1) for i in range(50)]
        update[30] = item("SKU-30", -4)
        with self.assertRaises(InvalidItem):
            self.stock.save(update)

        self.assertEqual(self.stock.load(), current)


if __name__ == "__main__":
    unittest.main()

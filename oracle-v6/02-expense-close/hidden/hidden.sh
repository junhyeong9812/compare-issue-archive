#!/usr/bin/env bash
export PYTHONDONTWRITEBYTECODE=1
python3 -m unittest -v hidden_test_close hidden_test_ledger_status

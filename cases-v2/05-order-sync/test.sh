#!/usr/bin/env bash
export PYTHONDONTWRITEBYTECODE=1
python3 -m unittest discover -s tests -t . -v

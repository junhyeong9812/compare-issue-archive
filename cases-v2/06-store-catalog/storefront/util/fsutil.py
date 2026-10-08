"""Filesystem helpers shared by the stores."""
import itertools
import json
import os
import shutil

_SEQ = itertools.count()


def write_json(path, data):
    """Write JSON so readers see either the old file or the new one, never a mix."""
    directory = os.path.dirname(path)
    os.makedirs(directory, exist_ok=True)
    tmp = os.path.join(directory, f".{os.path.basename(path)}.{os.getpid()}-{next(_SEQ)}.tmp")
    try:
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(data, f, sort_keys=True, ensure_ascii=False)
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp, path)
    except BaseException:
        try:
            os.unlink(tmp)
        except FileNotFoundError:
            pass
        raise


def read_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def list_json(directory):
    """Names of the *.json files in directory (hidden and temp files skipped)."""
    if not os.path.isdir(directory):
        return []
    return sorted(
        name for name in os.listdir(directory)
        if name.endswith(".json") and not name.startswith(".")
    )


def scratch_dir(root, purpose):
    """Working directory for an operation, created on demand under the scratch root."""
    path = os.path.join(root, purpose)
    os.makedirs(path, exist_ok=True)
    return path


def copy_files(src_dir, dst_dir, names=None):
    os.makedirs(dst_dir, exist_ok=True)
    for name in names if names is not None else list_json(src_dir):
        shutil.copy2(os.path.join(src_dir, name), os.path.join(dst_dir, name))


def remove_files(directory, names):
    for name in names:
        try:
            os.remove(os.path.join(directory, name))
        except FileNotFoundError:
            pass

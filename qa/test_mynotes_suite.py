"""Root compatibility gate also includes the independent MyNotes checks."""
import unittest
from pathlib import Path

def load_tests(loader, tests, pattern):
    root=Path(__file__).resolve().parents[1]
    return loader.discover(str(root/'mynotes/qa'), pattern='test_*.py', top_level_dir=str(root))

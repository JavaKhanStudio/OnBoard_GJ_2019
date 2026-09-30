#!/usr/bin/env python3
"""The longest Java methods of the tree, for a refacto day's measure (r186).

PMD is not installed here, so this counts lines from a method's signature to its
closing brace by brace depth. Crude (a brace in a char literal fools it), but enough
to rank. Usage: tools/longest_methods.py [how many, default 20]
"""

import re
import subprocess
import sys

SIGNATURE = re.compile(
    r"^\s*(?:(?:public|private|protected|static|final|synchronized)\s+)*"
    r"[\w<>\[\],\s]+\s+(\w+)\s*\([^;]*\)\s*(?:throws [\w, .]+)?\s*\{?\s*$"
)
NOT_METHODS = {"if", "for", "while", "switch", "catch", "synchronized", "return", "new"}
STRIP = re.compile(r'//.*|"(?:\\.|[^"])*"')


def methods(path):
    with open(path, encoding="utf-8", errors="replace") as f:
        lines = f.read().split("\n")
    i = 0
    while i < len(lines):
        match = SIGNATURE.match(lines[i])
        if not match or match.group(1) in NOT_METHODS:
            i += 1
            continue
        brace = i
        while brace < len(lines) and "{" not in lines[brace]:
            brace += 1
        if brace - i > 2:
            i += 1
            continue
        depth, end = 0, brace
        while end < len(lines):
            code = STRIP.sub("", lines[end])
            depth += code.count("{") - code.count("}")
            if depth <= 0:
                break
            end += 1
        yield end - i + 1, path, i + 1, match.group(1)
        i = end + 1


def main():
    top = int(sys.argv[1]) if len(sys.argv) > 1 else 20
    files = subprocess.run(
        ["git", "ls-files", "*.java", ":!html/freetype-gwt"],
        capture_output=True, text=True, check=True,
    ).stdout.split()
    found = [m for path in files for m in methods(path)]
    for length, path, line, name in sorted(found, reverse=True)[:top]:
        print(f"{length:5d}  {path}:{line}  {name}")


if __name__ == "__main__":
    main()

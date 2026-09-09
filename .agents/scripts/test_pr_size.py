"""Run isolated checks without modifying the working repository."""

import subprocess
import tempfile
from pathlib import Path

script = Path(__file__).with_name("check-pr-size.sh").resolve()

with tempfile.TemporaryDirectory() as directory:
    repo = Path(directory)

    def git(*args):
        return subprocess.check_output(
            ["git", "-c", "core.hooksPath=/dev/null", "-c", "commit.gpgsign=false",
             "-c", "user.name=Harness Test", "-c", "user.email=test@example.invalid", *args],
            cwd=repo, stderr=subprocess.DEVNULL, text=True,
        ).strip()

    def check(base, expected, change_type="modify"):
        result = subprocess.run(
            ["bash", str(script), base, "HEAD", change_type], cwd=repo, capture_output=True, text=True,
        )
        assert result.returncode == expected, result.stdout + result.stderr

    git("init")
    git("commit", "--allow-empty", "-m", "base")
    base = git("rev-parse", "HEAD")
    for count, expected in [(400, 0), (401, 1)]:
        (repo / "sample.txt").write_text("line\n" * count)
        git("add", ".")
        git("commit", "-m", str(count))
        check(base, expected)
    check(base, 0, "create")
    check(base, 2, "invalid")
    deletion_base = git("rev-parse", "HEAD")
    (repo / "sample.txt").unlink()
    git("add", "-u")
    git("commit", "-m", "delete 401 lines")
    check(deletion_base, 1)
    check(deletion_base, 0, "delete")
    (repo / "binary").write_bytes(b"\x00binary")
    git("add", ".")
    git("commit", "-m", "binary")
    check(base, 2)
    check("missing-base", 128)

print("PASS: modification 400/401 boundary, creation/deletion exemptions, invalid type, binary changes, and missing base.")

# runs the full pipeline from scratch: clean, extract, train, validate, plot
# usage: uv run all-fresh.py <simulation-output.xlsx>

import subprocess
import sys
from pathlib import Path


def run(*cmd: str):
    print(f"\n>> {' '.join(cmd)}")
    subprocess.run(cmd, check=True, cwd=Path(__file__).parent)


def main():
    if len(sys.argv) != 2:
        sys.exit("usage: uv run all-fresh.py <simulation-output.xlsx>")
    excel = str(Path(sys.argv[1]).resolve())
    csv = "data/training-data.csv"
    py = sys.executable

    run(py, "clean.py", "--all")
    run(py, "xls_simout_to_csv.py", excel, csv)
    run(py, "train.py", csv)
    run(py, "validate.py", csv)
    run("gnuplot", "model-check-plot.plt")


if __name__ == "__main__":
    main()

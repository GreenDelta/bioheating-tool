# cleanup generated files of the model training and validation scripts

import sys
from pathlib import Path

from common import DEMAND_MODEL_FILE, MODEL_CONFIG_FILE, PEAK_MODEL_FILE


def main():
    script_dir = Path(__file__).parent
    data_dir = script_dir / "data"


    # delete check files and charts
    for ext in ["*.png", "*.txt", "validation-check.xlsx"]:
        for f in data_dir.glob(ext):
            if f.is_file():
                f.unlink()
                print(f"Deleted: {f.name}")

    # delete the model files in server/app/model only with the --all flag
    if "--all" in sys.argv:
        for f in [DEMAND_MODEL_FILE, PEAK_MODEL_FILE, MODEL_CONFIG_FILE]:
            if f.is_file():
                f.unlink()
                print(f"Deleted: {f}")

    print("All done!")


if __name__ == "__main__":
    main()

# cleanup generated files of train.py

import sys
from pathlib import Path


def main():
    script_dir = Path(__file__).parent
    data_dir = script_dir / "data"


    # delete check files
    for ext in ["*.png", "*.txt"]:
        for f in data_dir.glob(ext):
            if f.is_file():
                f.unlink()
                print(f"Deleted: {f.name}")

    # delete all model files only if --all flag is provided
    delete_models = "--all" in sys.argv
    mod_dir = script_dir / "../src/main/resources/com/greendelta/bioheating/predict"
    if delete_models and mod_dir.exists():
        for model in mod_dir.glob("model.*"):
            if model.is_file():
                model.unlink()
                print(f"Deleted: {model.name}")

    print("All done!")


if __name__ == "__main__":
    main()

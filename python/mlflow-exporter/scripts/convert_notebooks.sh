#!/usr/bin/env bash
set -euo pipefail

MARIMO_DIR="$(dirname "$0")/../notebooks/marimo"
JUPYTER_DIR="$(dirname "$0")/../notebooks/jupyter"

mkdir -p "$JUPYTER_DIR"

for notebook in "$MARIMO_DIR"/*.py; do
    name="$(basename "$notebook" .py)"
    echo "Converting $name..."
    marimo export ipynb "$notebook" -o "$JUPYTER_DIR/$name.ipynb" --force
    echo "✓ $name.ipynb"
done

echo "Done. Jupyter notebooks written to $JUPYTER_DIR"

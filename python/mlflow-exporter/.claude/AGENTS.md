# mlflow-exporter — Agent Guide

Tooling for publishing ML assets — models and prompt templates — into MLflow so they can be versioned, tracked, and consumed by other services. Models go through a Hugging Face → ONNX pipeline before registration; prompts are registered directly as versioned artifacts.

This subproject supports the larger **resume-roaster-ai** demo: the Java/Spring Boot service pulls NER models and prompt templates from MLflow at runtime rather than bundling them.

## Stack

- **Python 3.13+**
- **MLflow ≥ 3.12** — experiment tracking, artifact storage, model registry
- **Hugging Face** (`huggingface-hub`, `transformers`) — model download and tokenizer management
- **PyTorch + ONNX / ONNX Runtime / onnxscript** — model export and quantization
- **Marimo** — primary notebook environment (reactive cells); Jupyter notebooks are auto-generated from Marimo sources
- **Pydantic Settings** — typed, env-prefix-based configuration
- **Loguru** — structured logging throughout

## Project layout

```
mlflow-exporter/
├── src/mlflow_exporter/
│   ├── settings.py          # Pydantic config — HF_, ONNX_, MLFLOW_ env prefixes
│   ├── models/
│   │   ├── hf.py            # download_model / prepare_for_export
│   │   ├── onnx_export.py   # export_to_onnx / validate_onnx_model / load_onnx_model
│   │   └── registry.py      # setup_mlflow / register_model_to_mlflow
│   └── prompts/             # loader.py / registry.py — prompt registration
├── notebooks/
│   ├── marimo/              # Primary notebooks (edit these)
│   │   ├── export_models.py
│   │   └── export_prompts.py
│   └── jupyter/             # Auto-generated — do not edit directly
├── scripts/
│   └── convert_notebooks.sh # marimo export ipynb → notebooks/jupyter/
├── .env.example
└── pyproject.toml
```

## Data flow

```
.env
 ↓
settings.py  (singleton instances: hf_settings, onnx_settings, mlflow_settings)
 ↓
notebooks/marimo/export_models.py
 ├─ hf.py          → snapshot_download() → load model + tokenizer
 ├─ onnx_export.py → torch.onnx.export() → optional int8 quantization → validate
 └─ registry.py    → MLflow run → log artifacts → register model version
```

## Key conventions

- **Settings**: Pydantic `BaseSettings` with env prefixes (`HF_`, `ONNX_`, `MLFLOW_`). Loaded from `.env`; module-level singletons (`hf_settings`, etc.) used across the package — do not instantiate new settings objects.
- **Logging**: `loguru.logger` everywhere. Use `logger.info` / `logger.debug` / `logger.error`; follow the existing emoji-prefix style (✓ success, ✗ failure).
- **Paths**: `pathlib.Path` throughout — no bare string path concatenation.
- **Error handling**: catch at the boundary, log with context, re-raise as `RuntimeError`.
- **Notebooks**: Marimo is the source of truth. After editing a Marimo notebook, regenerate the Jupyter equivalent with `scripts/convert_notebooks.sh`. Never edit the `notebooks/jupyter/` files directly.

## Environment variables

Copy `.env.example` to `.env` and fill in values before running anything.

| Variable | Default | Purpose |
|---|---|---|
| `HF_MODEL_ID` | `dslim/distilbert-NER` | Model to download and export |
| `HF_CACHE_DIR` | `./cache/huggingface` | Local HF cache |
| `ONNX_OUTPUT_DIR` | `./cache/onnx` | Where ONNX artifacts land |
| `ONNX_OPSET_VERSION` | `18` | ONNX opset |
| `MLFLOW_TRACKING_URI` | `http://localhost:5000` | MLflow server |
| `MLFLOW_TRACKING_USERNAME` | — | Basic auth |
| `MLFLOW_TRACKING_PASSWORD` | — | Basic auth |
| `MLFLOW_EXPERIMENT_NAME` | `Named Entity Recognition` | |
| `MLFLOW_RUN_NAME` | `distilbert-int8-onnx` | |
| `MLFLOW_REGISTRY_MODEL_NAME` | `distilbert-ner` | Registry name |
| `MLFLOW_REGISTRY_MODEL_DESCRIPTION` | — | |

## Running

```bash
# Export models (primary workflow)
marimo run notebooks/marimo/export_models.py

# Regenerate Jupyter notebooks
bash scripts/convert_notebooks.sh

# Lint
ruff check src/
```

Requires an MLflow tracking server running and reachable at `MLFLOW_TRACKING_URI`.

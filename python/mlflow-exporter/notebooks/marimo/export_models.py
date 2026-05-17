"""Marimo notebook for PII NER model export and registration."""

import marimo

__generated_with = "0.23.5"
app = marimo.App()


@app.cell
def _():
    import sys
    from pathlib import Path

    src_path = Path(__file__).parent.parent.parent / "src"
    sys.path.insert(0, str(src_path))

    from loguru import logger
    from mlflow_exporter.models.hf import download_model, prepare_for_export
    from mlflow_exporter.models.onnx_export import export_to_onnx, validate_onnx_model
    from onnxruntime.quantization import QuantType
    from mlflow_exporter.models.registry import setup_mlflow, register_model_to_mlflow

    logger.info("All imports successful")
    return (
        Path,
        QuantType,
        download_model,
        export_to_onnx,
        logger,
        prepare_for_export,
        register_model_to_mlflow,
        setup_mlflow,
        validate_onnx_model,
    )


@app.cell
def _():
    MODEL_ID = "dslim/distilbert-NER"
    EXPERIMENT_NAME = "named-entity-recognition"
    RUN_NAME = "distilbert-ner-int8-onnx"
    REGISTRY_MODEL_NAME = "distilbert-ner"
    REGISTRY_MODEL_DESCRIPTION = "DistilBERT NER model exported to ONNX int8 format for PII redaction"
    return (
        EXPERIMENT_NAME,
        MODEL_ID,
        REGISTRY_MODEL_DESCRIPTION,
        REGISTRY_MODEL_NAME,
        RUN_NAME,
    )


@app.cell
def _(MODEL_ID, download_model, logger):
    logger.info("Starting model download from: " + MODEL_ID)
    _model_path, tokenizer_downloaded, model_downloaded = download_model(MODEL_ID)
    logger.info("✓ Model download complete")
    return model_downloaded, tokenizer_downloaded


@app.cell
def _(logger, model_downloaded, prepare_for_export, tokenizer_downloaded):
    logger.info("Preparing model for ONNX export...")
    model_prepared, tokenizer_prepared = prepare_for_export(
        tokenizer_downloaded, model_downloaded
    )
    _model_size_mb = sum(p.nelement() * p.element_size() for p in model_prepared.parameters()) / 1024 ** 2
    logger.info("PyTorch model size: " + f"{_model_size_mb:.1f} MB")
    logger.info("✓ Model preparation complete")
    return model_prepared, tokenizer_prepared


@app.cell
def _(
    Path,
    QuantType,
    export_to_onnx,
    logger,
    model_prepared,
    tokenizer_prepared,
):
    logger.info("Exporting model to ONNX format...")
    try:
        onnx_path = export_to_onnx(model_prepared, tokenizer_prepared, quantize=QuantType.QInt8)
    except Exception as e:
        logger.error("✗ ONNX export failed: " + str(e))
        raise
    _onnx_size_mb = sum(f.stat().st_size for f in Path(onnx_path).iterdir()) / 1024 ** 2
    logger.info("ONNX model size: " + f"{_onnx_size_mb:.1f} MB")
    return (onnx_path,)


@app.cell
def _(logger, onnx_path, validate_onnx_model):
    logger.info("Validating ONNX model...")
    try:
        is_valid = validate_onnx_model(onnx_path)
        if not is_valid:
            raise RuntimeError("ONNX model validation failed")
        logger.info("✓ ONNX validation passed")
    except Exception as e:
        logger.error("✗ Validation error: " + str(e))
        raise
    return


@app.cell
def _(logger, setup_mlflow):
    logger.info("Setting up MLflow...")
    try:
        setup_mlflow()
    except Exception as e:
        logger.error("✗ MLflow setup failed: " + str(e))
        raise
    return


@app.cell
def _(
    EXPERIMENT_NAME,
    REGISTRY_MODEL_DESCRIPTION,
    REGISTRY_MODEL_NAME,
    RUN_NAME,
    logger,
    onnx_path,
    register_model_to_mlflow,
):
    logger.info("Registering model to MLflow as: " + REGISTRY_MODEL_NAME)
    try:
        model_uri = register_model_to_mlflow(
            onnx_path,
            model_name=REGISTRY_MODEL_NAME,
            description=REGISTRY_MODEL_DESCRIPTION,
            experiment_name=EXPERIMENT_NAME,
            run_name=RUN_NAME,
        )
        logger.info("✓ Model registered: " + str(model_uri))
    except Exception as e:
        logger.error("✗ MLflow registration failed: " + str(e))
        raise
    return


if __name__ == "__main__":
    app.run()

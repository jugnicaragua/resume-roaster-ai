"""ONNX model export and validation."""

from pathlib import Path

import torch
from loguru import logger
from transformers import AutoTokenizer
from onnxruntime.quantization import QuantType
import onnx
import onnxruntime as ort

from ..settings import onnx_settings


def export_to_onnx(
    model,
    tokenizer,
    output_dir: str | None = None,
    quantize: QuantType | None = None,
) -> Path:
    """Export model to ONNX format using torch.onnx.

    Args:
        model: Transformers model to export
        tokenizer: Tokenizer for the model
        output_dir: Output directory for ONNX files
        quantize: QuantType to apply after export (e.g. QuantType.QInt8), or None to skip

    Returns:
        Path to the exported ONNX model directory

    Raises:
        RuntimeError: If export or validation fails
    """
    output_dir = output_dir or onnx_settings.output_dir
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    logger.info("Exporting model to ONNX: " + str(output_path))

    sample_text = "This is a sample text for ONNX export."
    inputs = tokenizer(
        sample_text,
        return_tensors="pt",
        max_length=128,
        padding="max_length",
        truncation=True,
    )

    onnx_model_path = output_path / "model.onnx"
    try:
        logger.debug("Running torch.onnx.export...")
        torch.onnx.export(
            model,
            tuple(inputs.values()),
            str(onnx_model_path),
            input_names=list(inputs.keys()),
            output_names=["logits"],
            do_constant_folding=True,
            verbose=False,
        )
        logger.debug("torch.onnx.export complete")

        external_data_file = output_path / "model.onnx.data"
        if external_data_file.exists():
            logger.debug("External data file detected (" + str(round(external_data_file.stat().st_size / 1024**2, 1)) + " MB) — inlining into model.onnx...")
            from onnx.external_data_helper import load_external_data_for_model
            model_proto = onnx.load(str(onnx_model_path), load_external_data=False)
            load_external_data_for_model(model_proto, str(output_path))
            external_data_file.unlink()
            onnx.save_model(model_proto, str(onnx_model_path), save_as_external_data=False)
            logger.debug("Inlining complete — model.onnx size: " + str(round(onnx_model_path.stat().st_size / 1024**2, 1)) + " MB")
        else:
            logger.debug("No external data file — model already self-contained")

    except Exception as e:
        logger.error("ONNX export failed: " + str(e))
        raise RuntimeError("Failed to export model to ONNX: " + str(e)) from e

    if not onnx_model_path.exists():
        raise RuntimeError("ONNX export failed: model file not created at " + str(onnx_model_path))

    logger.info("Raw ONNX model size: " + str(round(onnx_model_path.stat().st_size / 1024**2, 1)) + " MB")

    if quantize is not None:
        from onnxruntime.quantization import quantize_dynamic
        from onnxruntime.quantization.preprocess import quant_pre_process

        preprocessed_path = output_path / "model_preprocessed.onnx"
        quantized_path = output_path / "model_quantized.onnx"

        logger.debug("Running quant_pre_process...")
        quant_pre_process(str(onnx_model_path), str(preprocessed_path))
        logger.debug("Pre-processed model size: " + str(round(preprocessed_path.stat().st_size / 1024**2, 1)) + " MB")

        logger.debug("Running quantize_dynamic (" + str(quantize) + ")...")
        quantize_dynamic(str(preprocessed_path), str(quantized_path), weight_type=quantize)
        logger.debug("Quantized model size: " + str(round(quantized_path.stat().st_size / 1024**2, 1)) + " MB")

        onnx_model_path.unlink()
        preprocessed_path.unlink()
        quantized_path.rename(onnx_model_path)
        logger.info("✓ Model quantized (" + str(quantize) + ") — final size: " + str(round(onnx_model_path.stat().st_size / 1024**2, 1)) + " MB")

    try:
        tokenizer.save_pretrained(str(output_path))
        model.config.save_pretrained(str(output_path))
    except Exception as e:
        logger.error("Failed to save tokenizer/config: " + str(e))
        raise RuntimeError("Failed to save tokenizer/config: " + str(e)) from e

    logger.info("✓ Model exported successfully to: " + str(output_path))
    return output_path


def validate_onnx_model(onnx_model_path: str | Path) -> bool:
    """Validate ONNX model format and structure.

    Args:
        onnx_model_path: Path to ONNX model directory

    Returns:
        True if model is valid
    """
    onnx_model_path = Path(onnx_model_path)
    logger.info(f"Validating ONNX model at: {onnx_model_path}")

    # Check required files
    required_files = ["model.onnx", "tokenizer.json", "config.json"]
    for file in required_files:
        file_path = onnx_model_path / file
        if not file_path.exists():
            logger.warning(f"Missing expected file: {file}")

    # Validate ONNX model format
    try:
        model_proto = onnx.load(str(onnx_model_path / "model.onnx"))
        onnx.checker.check_model(model_proto)
        logger.info("✓ ONNX model is valid")
    except Exception as e:
        logger.error(f"ONNX validation failed: {e}")
        return False

    logger.info("ONNX model validation complete")
    return True


def load_onnx_model(onnx_model_path: str | Path):
    """Load ONNX model for inference using ONNX Runtime.

    Args:
        onnx_model_path: Path to ONNX model directory

    Returns:
        Tuple of (session, tokenizer)
    """
    onnx_model_path = Path(onnx_model_path)
    logger.info(f"Loading ONNX model from: {onnx_model_path}")

    # Load ONNX Runtime session
    session = ort.InferenceSession(str(onnx_model_path / "model.onnx"))
    tokenizer = AutoTokenizer.from_pretrained(str(onnx_model_path))

    logger.info("ONNX model and tokenizer loaded successfully")
    return session, tokenizer

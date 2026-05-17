"""Hugging Face model download and preparation."""

from pathlib import Path

from huggingface_hub import snapshot_download
from loguru import logger
from transformers import AutoTokenizer, AutoModelForTokenClassification

from ..settings import hf_settings


def download_model(model_id: str, cache_dir: str | None = None) -> tuple[str, str]:
    """Download model and tokenizer from Hugging Face.

    Args:
        model_id: HF model ID
        cache_dir: Cache directory (defaults to hf_settings.cache_dir)

    Returns:
        Tuple of (model_path, tokenizer, model)
    """
    cache_dir = cache_dir or hf_settings.cache_dir

    logger.info(f"Downloading model: {model_id}")
    model_path = snapshot_download(
        repo_id=model_id,
        cache_dir=cache_dir,
        repo_type="model",
    )
    logger.info(f"Model downloaded to: {model_path}")

    logger.info("Loading tokenizer...")
    tokenizer = AutoTokenizer.from_pretrained(model_id, cache_dir=cache_dir)
    logger.info("Tokenizer loaded successfully")

    logger.info("Loading model...")
    model = AutoModelForTokenClassification.from_pretrained(model_id, cache_dir=cache_dir)
    logger.info("Model loaded successfully")

    return model_path, tokenizer, model


def prepare_for_export(tokenizer, model):
    """Prepare model and tokenizer for ONNX export.

    Args:
        tokenizer: Transformers tokenizer
        model: Transformers model

    Returns:
        Tuple of (model, tokenizer) ready for export
    """
    logger.info("Preparing model for ONNX export...")
    model.eval()
    logger.info("Model set to eval mode")

    return model, tokenizer

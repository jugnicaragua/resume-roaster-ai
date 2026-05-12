"""Scoped pydantic settings for HuggingFace, ONNX, and MLflow."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class HuggingFaceSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="HF_", env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    model_id: str = "dslim/distilbert-NER"
    cache_dir: str = "./cache/huggingface"


class OnnxSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="ONNX_", env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    output_dir: str = "./cache/onnx"
    opset_version: int = 18


class MlflowSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="MLFLOW_", env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    tracking_uri: str = "http://localhost:5000"
    tracking_username: str | None = None
    tracking_password: str | None = None
    experiment_name: str = "ner-model-export"
    run_name: str = "distilbert-ner-onnx"
    registry_model_name: str = "distilbert-ner"
    registry_model_description: str = "DistilBERT NER model exported to ONNX format"


hf_settings = HuggingFaceSettings()
onnx_settings = OnnxSettings()
mlflow_settings = MlflowSettings()

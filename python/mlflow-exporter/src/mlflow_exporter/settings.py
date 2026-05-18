"""Scoped pydantic settings for HuggingFace, ONNX, and MLflow."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class HuggingFaceSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="HF_", env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    cache_dir: str = "./cache/huggingface"


class OnnxSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="ONNX_", env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    output_dir: str = "./cache/onnx"


class MlflowSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="MLFLOW_", env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    tracking_uri: str = "http://localhost:5000"
    tracking_username: str | None = None
    tracking_password: str | None = None


hf_settings = HuggingFaceSettings()
onnx_settings = OnnxSettings()
mlflow_settings = MlflowSettings()

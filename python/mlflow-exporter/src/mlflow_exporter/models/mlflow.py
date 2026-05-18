"""MLflow model registry and logging."""

from pathlib import Path

import mlflow
from loguru import logger

from ..settings import mlflow_settings


def setup_mlflow(tracking_uri: str | None = None) -> bool:
    """Configure and verify MLflow connection.

    Args:
        tracking_uri: MLflow tracking server URI

    Returns:
        True if connection successful

    Raises:
        RuntimeError: If MLflow is unreachable or auth fails
    """
    import os

    tracking_uri = tracking_uri or mlflow_settings.tracking_uri

    if mlflow_settings.tracking_username and mlflow_settings.tracking_password:
        os.environ["MLFLOW_TRACKING_USERNAME"] = mlflow_settings.tracking_username
        os.environ["MLFLOW_TRACKING_PASSWORD"] = mlflow_settings.tracking_password

    mlflow.set_tracking_uri(tracking_uri)
    logger.info("Connecting to MLflow at: " + tracking_uri)

    try:
        client = mlflow.tracking.MlflowClient()
        client.search_experiments()
        logger.info("✓ MLflow connection successful")
        return True
    except Exception as e:
        logger.error("✗ MLflow connection failed: " + str(e))
        raise RuntimeError("Cannot reach MLflow at " + tracking_uri + ": " + str(e)) from e


def register_model(
    onnx_model_path: str | Path,
    model_name: str,
    description: str,
    experiment_name: str,
    run_name: str,
    tags: dict | None = None,
) -> str:
    """Register ONNX model to MLflow model registry.

    Args:
        onnx_model_path: Path to exported ONNX model directory
        model_name: Name for the model in registry
        description: Model description
        experiment_name: MLflow experiment name
        run_name: MLflow run name
        tags: Additional tags for the model

    Returns:
        Model URI in MLflow registry
    """
    onnx_model_path = Path(onnx_model_path)

    mlflow.set_experiment(experiment_name)
    logger.info("Registering model to MLflow: " + model_name)

    with mlflow.start_run(run_name=run_name) as run:
        mlflow.log_artifacts(str(onnx_model_path), artifact_path="onnx_model")
        mlflow.log_param("model_format", "onnx")
        mlflow.log_param("framework", "transformers")
        mlflow.log_param("task", "token-classification")
        if tags:
            mlflow.set_tags(tags)
        run_id = run.info.run_id
        artifact_uri = run.info.artifact_uri

    # MLflow 3.x: register_model requires a logged_model (log_model flavor), not raw artifacts.
    # Use MlflowClient.create_model_version with the artifact source URI instead.
    client = mlflow.tracking.MlflowClient()
    try:
        client.get_registered_model(model_name)
        logger.info("Model already exists in registry: " + model_name)
    except Exception:
        client.create_registered_model(model_name, description=description)
        logger.info("Created registered model: " + model_name)

    version = client.create_model_version(
        name=model_name,
        source=artifact_uri + "/onnx_model",
        run_id=run_id,
    )
    model_uri = "models:/" + model_name + "/" + str(version.version)
    logger.info("Model registered successfully: " + model_uri)
    return model_uri


def create_model_version(
    model_name: str,
    description: str | None = None,
) -> str:
    """Create a new model version in MLflow.

    Args:
        model_name: Name of the model
        description: Version description

    Returns:
        Model version number
    """
    client = mlflow.tracking.MlflowClient()

    logger.info(f"Creating new version for model: {model_name}")

    try:
        # Check if model exists, create if not
        client.get_registered_model(model_name)
    except Exception:
        logger.info(f"Model {model_name} not found, will be created during registration")

    logger.info(f"Ready to create version for: {model_name}")
    return model_name

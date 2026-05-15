"""Marimo notebook for prompt export and registration."""

import marimo

__generated_with = "0.23.5"
app = marimo.App()


@app.cell
def _():
    """Setup and imports."""
    import sys
    from pathlib import Path

    # Add src to path
    src_path = Path(__file__).parent.parent.parent / "src"
    sys.path.insert(0, str(src_path))

    from loguru import logger
    from mlflow_exporter.prompts.loader import load_prompts
    from mlflow_exporter.prompts.registry import register_prompts_to_mlflow

    logger.info("All imports successful")
    return logger, load_prompts, register_prompts_to_mlflow


@app.cell
def _(logger, load_prompts):
    """Step 1: Load prompts."""
    logger.info("Loading prompts...")
    load_prompts()
    logger.info("✓ Prompt loading complete")
    return


@app.cell
def _(logger, register_prompts_to_mlflow):
    """Step 2: Register prompts to MLflow."""
    logger.info("Registering prompts to MLflow...")
    register_prompts_to_mlflow()
    logger.info("✓ Prompt registration complete")
    return


@app.cell
def _(logger):
    """Summary."""
    logger.info("=" * 60)
    logger.info("Prompt pipeline complete!")
    logger.info("=" * 60)
    return


if __name__ == "__main__":
    app.run()

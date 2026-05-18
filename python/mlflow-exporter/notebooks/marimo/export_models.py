"""Marimo notebook for PII NER model export and registration."""

import marimo

__generated_with = "0.23.5"
app = marimo.App()


@app.cell
def _():
    import marimo as mo
    import sys
    from pathlib import Path

    src_path = Path(__file__).parent.parent.parent / "src"
    sys.path.insert(0, str(src_path))

    from loguru import logger
    from mlflow_exporter.models.hf import download_model, prepare_for_export
    from mlflow_exporter.models.onnx_export import export_to_onnx, validate_onnx_model
    from mlflow_exporter.models.registry import setup_mlflow, register_model_to_mlflow

    logger.info("All imports successful")
    return (
        Path,
        download_model,
        export_to_onnx,
        logger,
        mo,
        prepare_for_export,
        register_model_to_mlflow,
        setup_mlflow,
        validate_onnx_model,
    )


@app.cell(hide_code=True)
def _(mo):
    mo.md(r"""
    ## Model Configuration
    """)
    return


@app.cell
def _():
    MODEL_ID = "Babelscape/wikineural-multilingual-ner"
    EXPERIMENT_NAME = "named-entity-recognition"
    RUN_NAME = "wikineural-multilingual-ner-onnx"
    REGISTRY_MODEL_NAME = "wikineural-multilingual-ner"
    REGISTRY_MODEL_DESCRIPTION = "WikiNEural multilingual BERT NER model exported to ONNX format for PII redaction"
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


@app.cell(hide_code=True)
def _(mo):
    mo.md(r"""
    ## PyTorch Inference
    """)
    return


@app.cell
def _():
    sample_text = """
    Alex Morgan
    alex.morgan@example.com
    (555) 123-4567
    Chicago, IL
    linkedin.com/in/alexmorgan

    SUMMARY

    Data analyst with experience cleaning, transforming, and analyzing large datasets.
    Skilled in Python, SQL, Excel, Tableau, and dashboard development.

    EXPERIENCE

    Data Analyst
    Brightline Metrics
    June 2021 - Present
    Chicago, IL

    Built SQL reports for weekly revenue, churn, and customer activity metrics.
    Created Tableau dashboards used by sales and operations teams.
    Automated monthly reporting workflow with Python and pandas.

    Business Intelligence Intern
    Northlake Retail Group
    May 2020 - August 2020

    Cleaned customer transaction data and prepared summary reports.
    Assisted with ad hoc analysis for marketing campaign performance.

    EDUCATION

    B.S. in Information Systems
    Midwest State University
    Graduated May 2021

    SKILLS

    Python
    SQL
    pandas
    Excel
    Tableau
    Power BI
    Data Cleaning
    Dashboarding
    """

    class EntityTable:
        def __init__(self, rows):
            self._rows = rows

        def _repr_html_(self):
            if not self._rows:
                return "<table></table>"

            headers = self._rows[0].keys()
            header_html = "".join(f"<th>{h}</th>" for h in headers)
            rows_html = "".join(
                "<tr>" + "".join(f"<td>{row[h]}</td>" for h in headers) + "</tr>"
                for row in self._rows
            )

            return f"<table><thead><tr>{header_html}</tr></thead><tbody>{rows_html}</tbody></table>"

    return EntityTable, sample_text


@app.cell
def _(EntityTable, model_downloaded, sample_text, tokenizer_downloaded):
    import torch
    import torch.nn.functional as F


    def aggregate_entities(tokens, labels, confidences):
        entities = []
        current = None

        for tok, label, conf in zip(tokens, labels, confidences):
            if tok in ("[CLS]", "[SEP]", "[PAD]"):
                continue

            if tok.startswith("##"):
                if current is not None:
                    current["word"] += tok[2:]
                    current["scores"].append(conf)
                continue

            if label == "O":
                if current:
                    entities.append(current)
                    current = None
                continue

            bio, entity_type = label.split("-", 1)

            if bio == "B" or current is None or entity_type != current["type"]:
                if current:
                    entities.append(current)
                current = {"word": tok, "type": entity_type, "scores": [conf]}
            else:
                current["word"] += " " + tok
                current["scores"].append(conf)

        if current:
            entities.append(current)

        return [
            {
                "Word": e["word"],
                "Label": e["type"],
                "Confidence": round(sum(e["scores"]) / len(e["scores"]), 4),
            }
            for e in entities
        ]


    device = next(model_downloaded.parameters()).device

    inputs_pt = tokenizer_downloaded(
        sample_text,
        return_tensors="pt",
        truncation=True,
        max_length=512,
    )

    inputs_pt = {k: v.to(device) for k, v in inputs_pt.items()}

    with torch.no_grad():
        outputs_pt = model_downloaded(**inputs_pt)

    probs = F.softmax(outputs_pt.logits, dim=-1)[0].cpu()
    confidences, predictions_pt = probs.max(dim=-1)

    tokens_pt = tokenizer_downloaded.convert_ids_to_tokens(inputs_pt["input_ids"][0])
    id2label = model_downloaded.config.id2label
    labels_pt = [id2label[p.item()] for p in predictions_pt]

    EntityTable(
        sorted(
            aggregate_entities(tokens_pt, labels_pt, [c.item() for c in confidences]),
            key=lambda x: x["Confidence"],
            reverse=True,
        )
    )
    return (aggregate_entities,)


@app.cell
def _(EntityTable, model_downloaded, sample_text, tokenizer_downloaded):
    from transformers import pipeline

    ner_pipeline = pipeline(
        "ner",
        model=model_downloaded,
        tokenizer=tokenizer_downloaded,
        aggregation_strategy="simple",
    )

    EntityTable(
        sorted(
            [
                {
                    "Word": e["word"],
                    "Label": e["entity_group"],
                    "Confidence": round(e["score"], 4),
                }
                for e in ner_pipeline(sample_text)
            ],
            key=lambda x: x["Confidence"],
            reverse=True,
        )
    )
    return


@app.cell(hide_code=True)
def _(mo):
    mo.md(r"""
    ## ONNX Conversion
    """)
    return


@app.cell
def _(logger, model_downloaded, prepare_for_export, tokenizer_downloaded):
    logger.info("Preparing model for ONNX export...")
    model_prepared, tokenizer_prepared = prepare_for_export(
        tokenizer_downloaded, model_downloaded, device='cpu'
    )
    _model_size_mb = sum(p.nelement() * p.element_size() for p in model_prepared.parameters()) / 1024 ** 2
    logger.info("PyTorch model size: " + f"{_model_size_mb:.1f} MB")
    logger.info("✓ Model preparation complete")
    return model_prepared, tokenizer_prepared


@app.cell
def _(Path, export_to_onnx, logger, model_prepared, tokenizer_prepared):
    logger.info("Exporting model to ONNX format...")
    try:
        onnx_path = export_to_onnx(model_prepared, tokenizer_prepared, max_length=512)
    except Exception as e:
        logger.error("✗ ONNX export failed: " + str(e))
        raise
    _onnx_size_mb = sum(f.stat().st_size for f in Path(onnx_path).iterdir()) / 1024 ** 2
    logger.info("ONNX model size: " + f"{_onnx_size_mb:.1f} MB")
    return (onnx_path,)


@app.cell(hide_code=True)
def _(mo):
    mo.md(r"""
    ## ONNX Inference
    """)
    return


@app.cell
def _(
    EntityTable,
    aggregate_entities,
    onnx_path,
    sample_text,
    tokenizer_prepared,
):
    import json
    import numpy as np
    import onnxruntime as ort
    from scipy.special import softmax

    session = ort.InferenceSession(str(onnx_path / "model.onnx"))

    inputs_onnx = tokenizer_prepared(
        sample_text,
        return_tensors="np",
        truncation=True,
        max_length=512,
        padding="max_length",
    )

    logits_onnx = session.run(None, dict(inputs_onnx))[0][0]
    probs_onnx = softmax(logits_onnx, axis=-1)
    confidences_onnx = probs_onnx.max(axis=-1)
    predictions_onnx = probs_onnx.argmax(axis=-1)

    id2label_onnx = {
        int(k): v
        for k, v in json.loads((onnx_path / "config.json").read_text())["id2label"].items()
    }

    tokens_onnx = tokenizer_prepared.convert_ids_to_tokens(inputs_onnx["input_ids"][0])
    labels_onnx = [id2label_onnx[p] for p in predictions_onnx]

    EntityTable(
        sorted(
            aggregate_entities(tokens_onnx, labels_onnx, confidences_onnx.tolist()),
            key=lambda x: x["Confidence"],
            reverse=True,
        )
    )
    return


@app.cell(hide_code=True)
def _(mo):
    mo.md(r"""
    ## MLflow Model Registry
    """)
    return


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

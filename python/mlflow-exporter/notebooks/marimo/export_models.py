"""Marimo notebook for PII NER model export and registration."""

import marimo

__generated_with = "0.23.5"
app = marimo.App()


@app.cell
def _():
    import marimo as mo

    ## Standard Library
    import json
    import sys
    from dataclasses import dataclass
    from pathlib import Path

    ## Source Path Configuration
    src_path = Path(__file__).parent.parent.parent / "src"
    sys.path.insert(0, str(src_path))

    ## Logging
    from loguru import logger

    ## Scientific Computing
    from scipy.special import softmax
    import numpy as np

    ## Machine / Deep Learning
    import torch
    import torch.nn.functional as F
    import onnxruntime as ort

    ## Custom API
    from mlflow_exporter.models.hf import download_model, prepare_for_export
    from mlflow_exporter.models.onnx import export_onnx, validate_onnx
    from mlflow_exporter.models.mlflow import setup_mlflow, register_model

    return (
        F,
        Path,
        dataclass,
        download_model,
        json,
        logger,
        mo,
        ort,
        prepare_for_export,
        setup_mlflow,
        softmax,
        torch,
    )


@app.cell(hide_code=True)
def _(mo):
    mo.md(r"""
    # NER Inference & Export Pipeline

    This notebook walks through the full process of preparing a Named Entity Recognition (NER) model for further deployment and use: downloading the model, testing it with sample text, exporting it to ONNX, validating the exported model, and registering it in MLflow.

    **Named Entity Recognition** is the task of identifying spans of text that refer to real-world entities like people, organizations, and locations, and assigning each span a label.

    In this notebook, we will:

    1. Download a pre-trained NER model from **Hugging Face**, a platform for sharing and using machine learning models.
    2. Run NER inference with **PyTorch** to verify how the model performs on unstructured, resume-like text.
    3. Convert the model to **ONNX** and run inference again to confirm the exported model produces valid results.
    4. Register the ONNX model in the **MLflow** MLOps platform, using the Model Registry API so it can be versioned, tracked, and reused by other systems.


    ![](https://a.mktgcdn.com/p/3_ufP9mkbeIN-cuIeaBIpX2JuHLeoitKevXrEK1v5xU/1200x675.jpg)
    """)
    return


@app.cell(hide_code=True)
def _(mo):
    mo.md(r"""
    ## 1. Downloading the model from Hugging Face 🤗

    In this section, we define the model and MLflow metadata that will be used throughout the notebook.

    The model itself is downloaded from **Hugging Face** 🤗, a platform that hosts pre-trained machine learning models, datasets, and tokenizers.

    We will use [`Babelscape/wikineural-multilingual-ner`][wikineural-hf], a multilingual NER model that can identify entities such as people, organizations, and locations across different languages.

    This model is based on **mBERT** and was fine-tuned on **WikiNEuRal**, a multilingual NER dataset automatically derived from Wikipedia. It was trained jointly on 9 languages: German, English, Spanish, French, Italian, Dutch, Polish, Portuguese, and Russian.

    <a href="https://www.researchgate.net/figure/AraBERT-and-mBERT-models-architecture_fig5_379260582"><img src="https://www.researchgate.net/publication/379260582/figure/fig5/AS:11431281312018438@1740540394201/AraBERT-and-mBERT-models-architecture.png" alt="AraBERT and mBERT models architecture"/></a>

    [wikineural-hf]: https://huggingface.co/Babelscape/wikineural-multilingual-ner
    [mBert-image]: https://www.researchgate.net/publication/379260582/figure/fig5/AS:11431281312018438@1740540394201/AraBERT-and-mBERT-models-architecture.png
    """)
    return


@app.cell
def _(dataclass):
    @dataclass(frozen=True)
    class ModelExperimentConfig:
        model_id: str
        experiment_name: str
        run_name: str
        registry_model_name: str
        registry_model_description: str


    config = ModelExperimentConfig(
        model_id="Babelscape/wikineural-multilingual-ner",
        experiment_name="Named Entity Recognition",
        run_name="wikineural-multilingual-ner-onnx",
        registry_model_name="wikineural-multilingual-ner",
        registry_model_description=(
            "WikiNEural multilingual BERT NER model exported to ONNX format "
            "for PII redaction"
        ),
    )
    return (config,)


@app.cell
def _(config, download_model, logger):
    logger.info("Starting model download from: %s", config.model_id)
    model_path, tokenizer_downloaded, model_downloaded = download_model(config.model_id)
    logger.info("✓ Model download complete")
    return model_downloaded, tokenizer_downloaded


@app.cell(hide_code=True)
def _(mo):
    mo.md(r"""
    ## 2. PyTorch Model Inference

    In this section, we will run the downloaded NER model using **PyTorch**.

    PyTorch is an open-source deep learning framework used to build, train, and run neural networks. Here, we use it to execute the Hugging Face model on a sample resume-like text and inspect the entities it detects.

    The model returns token-level predictions, so we also group related tokens back into full entity names and display them in a readable table with their labels and confidence scores.

    <p align="center">
      <img
        src="https://b2633864.smushcdn.com/2633864/wp-content/uploads/2021/05/what_is_pytorch_logo.png?lossy=2&strip=1&webp=1"
        alt="PyTorch logo"
        width="320"
      />
    </p>
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
def _(
    EntityTable,
    F,
    model_downloaded,
    sample_text,
    tokenizer_downloaded,
    torch,
):
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


@app.cell(hide_code=True)
def _(mo):
    mo.md(r"""
    ## 3.1 Exporting the model to ONNX

    In this section, we export the NER model from PyTorch to **ONNX**.

    **ONNX** stands for Open Neural Network Exchange. It is a standard format for representing machine learning models so they can be moved between different frameworks and runtime environments.

    We export the model to ONNX and log the size of the exported model files so we can compare the final deployment artifact with the original model.

    ![](https://www.xenonstack.com/hubfs/xenonstack-onnx-overview-advantages.png)
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
    ## 3.2 ONNX Model Inference

    In this section, we run inference using the exported **ONNX** model.

    Instead of using PyTorch, we load the exported `model.onnx` file with **ONNX Runtime**, a lightweight engine for running ONNX models efficiently. This lets us verify that the exported model still works correctly after conversion.

    We tokenize the same sample resume-like text, pass it into the ONNX model, convert the output logits into probabilities, and then display the detected entities with their labels and confidence scores.
    """)
    return


@app.cell
def _(
    EntityTable,
    aggregate_entities,
    json,
    onnx_path,
    ort,
    sample_text,
    softmax,
    tokenizer_prepared,
):
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
    ## 5. Registering the model to the MLflow Model Registry

    In this section, we validate the exported ONNX model and register it in **MLflow**.

    **MLflow** is a MLOps platform used to track machine learning experiments, package models, and manage model versions. Here, we use the MLflow Model Registry to store the exported ONNX model as a versioned artifact that can be reused, promoted, or deployed later.

    Before registration, we first validate the ONNX model to make sure the exported files are complete and usable. Then we configure MLflow, create a run under the selected experiment, and register the model with its name, description, and metadata.

    ![](https://media.licdn.com/dms/image/v2/D5612AQEjX9QXWKwqWQ/article-cover_image-shrink_720_1280/B56ZqWy4dUG0AM-/0/1763466514325?e=2147483647&v=beta&t=HqiZtVo0yAdVtQbfppRRa4TjqdpNP4gCaPJl-t_yKtk)
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
def _(config, logger, onnx_path, register_model_to_mlflow):
    logger.info("Registering model to MLflow as: %s", config.registry_model_name)

    try:
        model_uri = register_model_to_mlflow(
            onnx_path,
            model_name=config.registry_model_name,
            description=config.registry_model_description,
            experiment_name=config.experiment_name,
            run_name=config.run_name,
        )

        logger.info("✓ Model registered: %s", model_uri)

    except Exception as e:
        logger.error("✗ MLflow registration failed: %s", str(e))
        raise
    return


if __name__ == "__main__":
    app.run()

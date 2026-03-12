from pathlib import Path
from datetime import date
from typing import Optional

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, field_validator

BASE = Path(__file__).parent
GENERAL_MODEL_PATH = BASE / "condition_model.pkl"
CUSTOM_MODEL_PATH  = BASE / "custom_model.pkl"

app = FastAPI(title="AgriSense Condition Predictor", version="1.0.0")

general_bundle = joblib.load(GENERAL_MODEL_PATH)

custom_bundle: Optional[dict] = None
if CUSTOM_MODEL_PATH.exists():
    custom_bundle = joblib.load(CUSTOM_MODEL_PATH)


def get_bundle(model_name: str) -> dict:
    if model_name == "custom":
        if custom_bundle is None:
            raise HTTPException(
                status_code=404,
                detail="Custom model not found. Train one first from the Options tab."
            )
        return custom_bundle
    return general_bundle


class PredictionRequest(BaseModel):
    animal_type: str
    vaccinated: int
    weight: float
    appetite: str
    record_date: date
    production: float

    @field_validator("appetite")
    @classmethod
    def validate_appetite(cls, v):
        allowed = ["high", "normal", "low", "none"]
        if v.lower() not in allowed:
            raise ValueError(f"appetite must be one of {allowed}")
        return v.lower()

    @field_validator("vaccinated")
    @classmethod
    def validate_vaccinated(cls, v):
        if v not in (0, 1):
            raise ValueError("vaccinated must be 0 or 1")
        return v

    @field_validator("animal_type")
    @classmethod
    def normalize_type(cls, v):
        return v.lower()


class PredictionResponse(BaseModel):
    condition: str
    probabilities: dict[str, float]


@app.post("/predict", response_model=PredictionResponse)
def predict(req: PredictionRequest, model: str = Query(default="general")):
    bundle = get_bundle(model)
    mdl        = bundle["model"]
    le_type     = bundle["le_type"]
    le_appetite = bundle["le_appetite"]
    le_condition= bundle["le_condition"]

    try:
        type_enc     = le_type.transform([req.animal_type])[0]
        appetite_enc = le_appetite.transform([req.appetite])[0]
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))

    ts = pd.Timestamp(req.record_date)

    X = np.array([[
        type_enc,
        req.vaccinated,
        req.weight,
        appetite_enc,
        ts.month,
        ts.dayofyear,
        req.production,
    ]])

    pred_enc  = mdl.predict(X)[0]
    proba     = mdl.predict_proba(X)[0]
    condition = le_condition.inverse_transform([pred_enc])[0]
    prob_dict = {
        le_condition.classes_[i]: round(float(proba[i]), 4)
        for i in range(len(le_condition.classes_))
    }

    return PredictionResponse(condition=condition, probabilities=prob_dict)


@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "general_model": GENERAL_MODEL_PATH.exists(),
        "custom_model": CUSTOM_MODEL_PATH.exists(),
    }


@app.get("/custom_model_available")
def custom_model_available():
    return {"available": CUSTOM_MODEL_PATH.exists()}


@app.get("/classes")
def get_classes():
    le_condition = general_bundle["le_condition"]
    le_type      = general_bundle["le_type"]
    le_appetite  = general_bundle["le_appetite"]
    return {
        "conditions":   list(le_condition.classes_),
        "animal_types": list(le_type.classes_),
        "appetites":    list(le_appetite.classes_),
    }

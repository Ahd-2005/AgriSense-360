from pathlib import Path
from datetime import date

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, field_validator

BASE = Path(__file__).parent
MODEL_PATH = BASE / "condition_model.pkl"

app = FastAPI(title="AgriSense Condition Predictor", version="1.0.0")

bundle = joblib.load(MODEL_PATH)
model       = bundle["model"]
le_type     = bundle["le_type"]
le_appetite = bundle["le_appetite"]
le_condition= bundle["le_condition"]


class PredictionRequest(BaseModel):
    animal_type: str
    vaccinated: int
    weight: float
    appetite: str
    record_date: date
    production: float

    @field_validator("animal_type")
    @classmethod
    def validate_type(cls, v):
        allowed = ["cow", "sheep", "goat"]
        if v.lower() not in allowed:
            raise ValueError(f"animal_type must be one of {allowed}")
        return v.lower()

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


class PredictionResponse(BaseModel):
    condition: str
    probabilities: dict[str, float]


@app.post("/predict", response_model=PredictionResponse)
def predict(req: PredictionRequest):
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

    pred_enc   = model.predict(X)[0]
    proba      = model.predict_proba(X)[0]
    condition  = le_condition.inverse_transform([pred_enc])[0]
    prob_dict  = {
        le_condition.classes_[i]: round(float(proba[i]), 4)
        for i in range(len(le_condition.classes_))
    }

    return PredictionResponse(condition=condition, probabilities=prob_dict)


@app.get("/health")
def health_check():
    return {"status": "ok", "model": "condition_model.pkl"}


@app.get("/classes")
def get_classes():
    return {
        "conditions": list(le_condition.classes_),
        "animal_types": list(le_type.classes_),
        "appetites": list(le_appetite.classes_),
    }

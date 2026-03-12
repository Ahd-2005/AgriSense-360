from pathlib import Path
import sys
import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, accuracy_score, confusion_matrix
import joblib

'''
bech tsob libraries a3ml :  pip install -r requirements.txt
f cmd baed ma todkhel lel folder python,
baed python train.py w stana chwaya taw yetsna3lek l model
condition_model.pkl , baed a3ml uvicorn api:app bech tlanci l API
l API key ta3 l mail yetna7a fl site wahdou maghyr sbab lezem
tes2elni bech na3tik l key f wa9tou

Datasets sources :
https://cgspace.cgiar.org/collections/171897ee-d131-4087-9edb-2713dece100a
https://www.sciencedirect.com/science/article/pii/S2352340924001768
https://www.kaggle.com/datasets/gracehephzibahm/animal-disease
https://openreview.net/forum?id=X4nq0W2qZX#discussion
'''

BASE = Path(__file__).parent

animals = pd.read_csv(BASE / "animal.csv")
records = pd.read_csv(BASE / "healthRecord.csv")

animals_sel = animals[["id", "type", "vaccinated"]].copy()
animals_sel["id"] = range(1, len(animals_sel) + 1)
records_sel = records[["animal", "weight", "appetite", "conditionStatus",
                        "recordDate", "milkYield", "eggCount", "woolLength"]]

df = records_sel.merge(animals_sel, left_on="animal", right_on="id")

def pick_production(row):
    t = row["type"]
    if t == "cow":
        return row["milkYield"]
    elif t in ("sheep", "goat"):
        return row["woolLength"]
    else:
        return row["eggCount"]

df["production"] = df.apply(pick_production, axis=1)

df["recordDate"] = pd.to_datetime(df["recordDate"])
df["record_month"] = df["recordDate"].dt.month
df["record_dayofyear"] = df["recordDate"].dt.dayofyear

le_type      = LabelEncoder()
le_appetite  = LabelEncoder()
le_condition = LabelEncoder()

df["type_enc"]      = le_type.fit_transform(df["type"])
df["appetite_enc"]  = le_appetite.fit_transform(df["appetite"])
df["condition_enc"] = le_condition.fit_transform(df["conditionStatus"])

FEATURES = ["type_enc", "vaccinated", "weight",
            "appetite_enc", "record_month", "record_dayofyear", "production"]

X = df[FEATURES].values
y = df["condition_enc"].values

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

clf = RandomForestClassifier(n_estimators=200, max_depth=12, random_state=42, n_jobs=-1,
                             class_weight='balanced')
clf.fit(X_train, y_train)

y_pred = clf.predict(X_test)

print(f"\nAccuracy: {accuracy_score(y_test, y_pred):.4f}\n")
print(classification_report(y_test, y_pred, target_names=le_condition.classes_))

print("Feature importances:")
for name, imp in sorted(zip(FEATURES, clf.feature_importances_), key=lambda x: -x[1]):
    print(f"  {name:<22} {imp:.4f}")

bundle = {
    "model": clf,
    "le_type": le_type,
    "le_appetite": le_appetite,
    "le_condition": le_condition,
    "feature_names": FEATURES,
}

model_path = BASE / ("custom_model.pkl" if "--custom" in sys.argv else "condition_model.pkl")
joblib.dump(bundle, model_path)
print(f"\nModel saved to {model_path}")

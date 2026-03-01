# -*- coding: utf-8 -*-
# train_model.py - Lance UNE SEULE FOIS pour entrainer le modele
# python train_model.py

import os
import pickle
import numpy as np
from sklearn.ensemble import GradientBoostingRegressor

# ─────────────────────────────────────────────────────────────────────────────
# ENCODAGE:
# type_culture: 0=Cereales  1=Legumes  2=Fruits  3=Ornementales
# etat:         0=semis     1=croissance  2=maturite  3=recolte_prevue  4=recolte_retard
# delta_days:   positif = recolte en avance / negatif = recolte en retard
# surface_ha:   surface en hectares
# ─────────────────────────────────────────────────────────────────────────────
#
# Rendements realistes Tunisie / Mediterranee:
#   Cereales  (ble, orge, mais) : 2800-3500 kg/ha
#   Legumes   (tomate, courgette, carotte) : 20000-40000 kg/ha
#   Fruits    (pomme, peche, orange) : 8000-15000 kg/ha
#   Ornementales : production en bouquets, ~500-1000 kg/ha equivalent

data = [
    # fmt: [type, etat, surface_ha, delta_days, quantite_kg]

    # ══ CEREALES - Rendement base 3100 kg/ha ══
    # Semis / Croissance => 0 kg toujours
    [0, 0, 0.5,  90,    0.0],
    [0, 0, 1.0,  80,    0.0],
    [0, 0, 2.0,  70,    0.0],
    [0, 0, 3.0,  60,    0.0],
    [0, 1, 0.5,  40,    0.0],
    [0, 1, 1.0,  35,    0.0],
    [0, 1, 2.0,  30,    0.0],
    [0, 1, 3.0,  25,    0.0],
    # Maturite - recolte en avance > 14j (rendement 45-55%)
    [0, 2, 0.5,  20,  697.5],
    [0, 2, 1.0,  18, 1395.0],
    [0, 2, 2.0,  15, 2790.0],
    [0, 2, 3.0,  14, 4185.0],
    # Maturite - recolte en avance 7-14j (rendement 65-75%)
    [0, 2, 0.5,  10,  1085.0],
    [0, 2, 1.0,   8,  2170.0],
    [0, 2, 2.0,   7,  4340.0],
    [0, 2, 3.0,   9,  6510.0],
    # Maturite - recolte optimale +-3j (rendement 95-100%)
    [0, 2, 0.5,   0,  1550.0],
    [0, 2, 1.0,   1,  3100.0],
    [0, 2, 2.0,  -1,  6200.0],
    [0, 2, 3.0,   2,  9300.0],
    [0, 2, 0.5,  -2,  1488.0],
    [0, 2, 1.0,  -3,  2945.0],
    # Maturite - recolte en retard 4-14j (rendement 82-88%)
    [0, 2, 0.5,  -6,  1302.0],
    [0, 2, 1.0,  -8,  2604.0],
    [0, 2, 2.0, -10,  5208.0],
    [0, 2, 3.0, -12,  7812.0],
    # Maturite - recolte tres en retard >14j (rendement 65-72%)
    [0, 2, 0.5, -20,  1085.0],
    [0, 2, 1.0, -18,  2170.0],
    [0, 2, 2.0, -25,  4340.0],
    # Recolte prevue - optimale
    [0, 3, 0.5,   0,  1519.0],
    [0, 3, 1.0,   0,  3038.0],
    [0, 3, 2.0,   1,  6076.0],
    [0, 3, 3.0,  -1,  9114.0],
    # Recolte en retard
    [0, 4, 0.5, -15,  1085.0],
    [0, 4, 1.0, -20,  2170.0],
    [0, 4, 2.0, -30,  3720.0],

    # ══ LEGUMES - Rendement base 28000 kg/ha (tomate, courgette, carotte...) ══
    [1, 0, 0.2,  30,      0.0],
    [1, 0, 0.5,  25,      0.0],
    [1, 0, 1.0,  20,      0.0],
    [1, 1, 0.2,  15,      0.0],
    [1, 1, 0.5,  12,      0.0],
    [1, 1, 1.0,  10,      0.0],
    # Maturite en avance > 7j
    [1, 2, 0.2,  10,   3360.0],
    [1, 2, 0.5,   8,   8400.0],
    [1, 2, 1.0,   7,  16800.0],
    # Optimale +-3j
    [1, 2, 0.2,   0,   5600.0],
    [1, 2, 0.5,   1,  14000.0],
    [1, 2, 1.0,  -1,  28000.0],
    [1, 2, 2.0,   2,  56000.0],
    [1, 2, 0.3,  -2,   8400.0],
    # En retard 4-14j
    [1, 2, 0.2,  -6,   4760.0],
    [1, 2, 0.5,  -8,  11900.0],
    [1, 2, 1.0, -10,  23800.0],
    [1, 2, 2.0, -12,  47600.0],
    # Tres en retard
    [1, 2, 0.5, -20,   9800.0],
    [1, 2, 1.0, -25,  17500.0],
    # Recolte prevue
    [1, 3, 0.5,   0,  13720.0],
    [1, 3, 1.0,   0,  27440.0],
    [1, 3, 2.0,   1,  54880.0],
    # Recolte en retard
    [1, 4, 0.5, -15,  11200.0],
    [1, 4, 1.0, -20,  19600.0],

    # ══ FRUITS - Rendement base 10000 kg/ha (pomme, peche, orange...) ══
    [2, 0, 0.5,  90,     0.0],
    [2, 0, 1.0,  80,     0.0],
    [2, 1, 0.5,  45,     0.0],
    [2, 1, 1.0,  30,     0.0],
    # Maturite en avance
    [2, 2, 0.5,  12,  2750.0],
    [2, 2, 1.0,  10,  5500.0],
    # Optimale
    [2, 2, 0.5,   0,  5000.0],
    [2, 2, 1.0,   1, 10000.0],
    [2, 2, 2.0,  -1, 20000.0],
    [2, 2, 0.5,  -2,  4800.0],
    # En retard
    [2, 2, 0.5,  -8,  4200.0],
    [2, 2, 1.0, -10,  8400.0],
    [2, 2, 2.0, -15, 16000.0],
    # Tres en retard
    [2, 2, 0.5, -25,  3000.0],
    [2, 2, 1.0, -30,  6000.0],
    # Recolte prevue
    [2, 3, 0.5,   0,  4900.0],
    [2, 3, 1.0,   0,  9800.0],
    [2, 3, 2.0,   1, 19600.0],
    # Recolte en retard
    [2, 4, 0.5, -20,  3500.0],
    [2, 4, 1.0, -25,  7000.0],

    # ══ ORNEMENTALES - Rendement base 750 kg/ha ══
    [3, 0, 0.2,  30,    0.0],
    [3, 0, 0.5,  25,    0.0],
    [3, 1, 0.2,  15,    0.0],
    [3, 1, 0.5,  10,    0.0],
    [3, 2, 0.2,   0,  150.0],
    [3, 2, 0.5,   1,  375.0],
    [3, 2, 1.0,  -1,  750.0],
    [3, 2, 0.2,  10,   90.0],
    [3, 2, 0.5,  -8,  318.0],
    [3, 3, 0.5,   0,  368.0],
    [3, 3, 1.0,   0,  735.0],
    [3, 4, 0.5, -15,  262.0],
    [3, 4, 1.0, -20,  487.0],
]

X = np.array([[d[0], d[1], d[2], d[3]] for d in data])
y = np.array([d[4] for d in data])

# GradientBoosting est plus precis que RandomForest sur ce type de donnees
model = GradientBoostingRegressor(
    n_estimators=500,
    max_depth=5,
    learning_rate=0.05,
    min_samples_leaf=1,
    random_state=42
)
model.fit(X, y)

# Sauvegarde
script_dir = os.path.dirname(os.path.abspath(__file__))
model_path = os.path.join(script_dir, "harvest_model.pkl")
with open(model_path, "wb") as f:
    pickle.dump(model, f)

print("Modele entraine et sauvegarde: " + model_path)

# Tests de validation
print("\nTests de prediction:")
tests = [
    ([0, 2, 1.0,   0], "Cereale  maturite optimale   1ha  -> attendu ~3100 kg"),
    ([0, 2, 2.0,   0], "Cereale  maturite optimale   2ha  -> attendu ~6200 kg"),
    ([0, 2, 1.0,  15], "Cereale  maturite en avance  1ha  -> attendu ~1400 kg"),
    ([0, 2, 1.0, -10], "Cereale  maturite en retard  1ha  -> attendu ~2604 kg"),
    ([0, 0, 1.0,  60], "Cereale  semis               1ha  -> attendu 0 kg"),
    ([1, 2, 1.0,   0], "Legume   maturite optimale   1ha  -> attendu ~28000 kg"),
    ([1, 2, 0.5,   0], "Legume   maturite optimale   0.5ha -> attendu ~14000 kg"),
    ([1, 1, 1.0,  10], "Legume   croissance          1ha  -> attendu 0 kg"),
    ([2, 2, 1.0,   0], "Fruit    maturite optimale   1ha  -> attendu ~10000 kg"),
    ([2, 2, 2.0,  -1], "Fruit    maturite optimale   2ha  -> attendu ~20000 kg"),
    ([3, 2, 1.0,   0], "Ornement maturite optimale   1ha  -> attendu ~750 kg"),
]
for feat, label in tests:
    pred = max(0, model.predict([feat])[0])
    print("  {:<55} : {:.0f} kg".format(label, pred))
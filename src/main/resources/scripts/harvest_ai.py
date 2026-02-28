# -*- coding: utf-8 -*-
# harvest_ai.py
# Utilise le modele entraine (harvest_model.pkl) pour predire la quantite recoltee.
#
# Usage:
#   python harvest_ai.py <nom> <type_culture> <etat> <surface_ha> <date_plantation> <date_recolte> <date_aujourd_hui>
#
# Output JSON:
#   {"quantite_kg": 2910.0, "rendement_pct": 97, "explication": "..."}
#
# LOGIQUE RENDEMENT (delta = date_recolte - aujourd_hui):
#   delta == 0       → 100% (jour exact de recolte)
#   delta > 0        → trop tot, rendement diminue progressivement
#                       delta 1j → ~96%  |  5j → ~78%  |  10j → ~60%  |  20j → ~40%  |  30j+ → ~20%
#   delta < 0        → trop tard, rendement diminue progressivement
#                       1j retard → ~97%  |  5j → ~87%  |  10j → ~77%  |  20j → ~60%  |  30j+ → ~45%
#   (Les pertes sont plus fortes en avance qu'en retard car la maturation n'est pas terminee)

import sys
import json
import os
import pickle
import math

def encode_type(type_culture):
    t = type_culture.lower()
    if any(x in t for x in ["cereal", "ble", "riz", "mais", "avoine", "orge"]):
        return 0
    if any(x in t for x in ["legume", "tomate", "carotte", "salad", "pomme de", "oignon", "lentille", "poivron"]):
        return 1
    if any(x in t for x in ["fruit", "pomme", "peche", "orange", "fraise", "banane", "framboise", "olive", "amande", "figue"]):
        return 2
    if any(x in t for x in ["ornement", "rosier", "tulipe", "jasmin", "laurier"]):
        return 3
    return 0  # default cereale

def encode_etat(etat):
    e = etat.lower()
    if "semis" in e:
        return 0
    if "croissance" in e:
        return 1
    if "maturit" in e:
        return 2
    if "prevue" in e or "pret" in e:
        return 3
    if "retard" in e:
        return 4
    if "recolte" in e:
        return 3
    return 2  # default maturite

def calc_days_delta(date_recolte_str, date_aujourd_hui_str):
    from datetime import date
    try:
        dr = date.fromisoformat(date_recolte_str)
        da = date.fromisoformat(date_aujourd_hui_str)
        # positif = recolte en avance (trop tot), negatif = recolte en retard (trop tard)
        return (dr - da).days
    except Exception:
        return 0

def rendement_base_kg_ha(type_code):
    """Rendement optimal de reference (kg/ha) - correspond aux valeurs d'entrainement."""
    bases = {0: 3100, 1: 28000, 2: 10000, 3: 750}
    return bases.get(type_code, 3100)

def calc_rendement_pct(delta_days):
    """
    Calcule le rendement en % selon l'ecart au jour ideal de recolte.

    delta_days > 0 : recolte TROP TOT (en avance)
      → La plante n'a pas fini sa maturation → pertes rapides
      → Formule : 100 * exp(-0.035 * delta)
      → Exemples : 0j=100%, 1j≈96%, 5j≈84%, 10j≈70%, 20j≈50%, 30j≈35%

    delta_days == 0 : jour exact → 100%

    delta_days < 0 : recolte TROP TARD (en retard)
      → La plante commence a se degrader mais plus lentement
      → Formule : 100 * exp(-0.015 * |delta|)
      → Exemples : 0j=100%, 1j≈99%, 5j≈93%, 10j≈86%, 20j≈74%, 30j≈64%
    """
    if delta_days == 0:
        return 100

    if delta_days > 0:
        # Trop tot : decroissance exponentielle rapide
        pct = 100.0 * math.exp(-0.035 * delta_days)
        # Minimum 15% (la culture a quand meme une valeur partielle)
        pct = max(15.0, pct)
    else:
        # Trop tard : decroissance exponentielle lente
        jours_retard = abs(delta_days)
        pct = 100.0 * math.exp(-0.015 * jours_retard)
        # Minimum 30% (les pertes ne sont jamais totales)
        pct = max(30.0, pct)

    return int(round(pct))

def build_explication(delta_days, rendement_pct, prediction):
    """Genere une explication lisible selon le delta."""
    if delta_days > 14:
        return ("Recolte tres en avance ({} jours avant maturite). "
                "Rendement: {}%. Quantite estimee: {:.1f} kg.").format(delta_days, rendement_pct, prediction)
    elif delta_days > 0:
        return ("Recolte en avance ({} jours). "
                "Rendement: {}%. Quantite estimee: {:.1f} kg.").format(delta_days, rendement_pct, prediction)
    elif delta_days == 0:
        return ("Recolte au jour optimal. "
                "Rendement maximal: {}%. Quantite: {:.1f} kg.").format(rendement_pct, prediction)
    elif delta_days >= -7:
        return ("Recolte legerement en retard ({} jours). "
                "Rendement: {}%. Quantite: {:.1f} kg.").format(abs(delta_days), rendement_pct, prediction)
    elif delta_days >= -20:
        return ("Recolte en retard ({} jours). Pertes moderees. "
                "Rendement: {}%. Quantite: {:.1f} kg.").format(abs(delta_days), rendement_pct, prediction)
    else:
        return ("Recolte tres en retard ({} jours). Pertes importantes. "
                "Rendement: {}%. Quantite: {:.1f} kg.").format(abs(delta_days), rendement_pct, prediction)

def main():
    if len(sys.argv) != 8:
        print("Usage: harvest_ai.py <nom> <type_culture> <etat> <surface_ha> <date_plantation> <date_recolte> <date_aujourd_hui>", file=sys.stderr)
        sys.exit(1)

    nom              = sys.argv[1]
    type_culture     = sys.argv[2]
    etat             = sys.argv[3]
    surface_ha       = float(sys.argv[4])
    date_plantation  = sys.argv[5]
    date_recolte     = sys.argv[6]
    date_aujourd_hui = sys.argv[7]

    etat_lower = etat.lower()

    # Cas immediats sans modele : semis ou croissance → 0 kg
    if "semis" in etat_lower or "croissance" in etat_lower:
        result = {
            "quantite_kg": 0.0,
            "rendement_pct": 0,
            "explication": "Culture en phase '{}'. Trop tot pour recolter. 0 kg.".format(etat)
        }
        print(json.dumps(result))
        return

    # Encoder les entrees
    type_code  = encode_type(type_culture + " " + nom)
    etat_code  = encode_etat(etat)
    delta_days = calc_days_delta(date_recolte, date_aujourd_hui)

    # ── Calcul du rendement % directement depuis delta_days ──────────────────
    # C'est independant du modele : 100% seulement le jour exact,
    # diminue selon la courbe exponentielle dans les deux sens.
    rendement_pct = calc_rendement_pct(delta_days)

    features = [type_code, etat_code, surface_ha, delta_days]

    # Charger le modele
    script_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(script_dir, "harvest_model.pkl")

    if not os.path.exists(model_path):
        # Modele pas encore entraine → fallback deterministique
        result = fallback(type_code, etat_code, surface_ha, delta_days, etat, rendement_pct)
        print(json.dumps(result))
        return

    try:
        with open(model_path, "rb") as f:
            model = pickle.load(f)

        # Prediction brute du modele (quantite kg)
        prediction_raw = model.predict([features])[0]
        prediction_raw = max(0.0, prediction_raw)

        # Recalculer la quantite coherente avec le rendement%
        # On utilise le rendement% calcule ci-dessus applique au rendement max theorique
        base_kg_ha = rendement_base_kg_ha(type_code)
        qty_max_theorique = base_kg_ha * surface_ha

        # La quantite finale = max_theorique * rendement% / 100
        # On pondere legerement avec la prediction du modele pour garder l'effet IA
        # Poids : 60% modele, 40% calcul theorique (pour rester coherent)
        qty_theorique = qty_max_theorique * rendement_pct / 100.0
        prediction = round(0.6 * prediction_raw + 0.4 * qty_theorique, 1)
        prediction = max(0.0, prediction)

        expl = build_explication(delta_days, rendement_pct, prediction)

        result = {
            "quantite_kg": prediction,
            "rendement_pct": rendement_pct,
            "explication": expl
        }
        print(json.dumps(result))

    except Exception as e:
        result = fallback(type_code, etat_code, surface_ha, delta_days, etat, rendement_pct)
        result["explication"] += " (erreur modele: {})".format(str(e))
        print(json.dumps(result))


def fallback(type_code, etat_code, surface_ha, delta_days, etat_str, rendement_pct=None):
    """Calcul deterministique si le modele est absent ou en erreur."""
    base = rendement_base_kg_ha(type_code)

    if etat_code in (0, 1):
        pct = 0
    else:
        pct = rendement_pct if rendement_pct is not None else calc_rendement_pct(delta_days)

    qty = round(base * surface_ha * pct / 100, 1)
    expl = build_explication(delta_days, pct, qty)
    return {
        "quantite_kg": qty,
        "rendement_pct": pct,
        "explication": expl
    }


if __name__ == "__main__":
    main()
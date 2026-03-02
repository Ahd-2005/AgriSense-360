#!/usr/bin/env python3
import warnings
warnings.filterwarnings("ignore")   # suppress pkg_resources deprecation warning

import argparse
import json
import sys
import mysql.connector
import face_recognition
import numpy as np

# ── Database Configuration ─────────────────────────────
DB_HOST     = 'localhost'
DB_USER     = 'root'
DB_PASSWORD = ''
DB_NAME     = 'agrisense-360'   # ← change to your DB name

def get_db_connection():
    try:
        return mysql.connector.connect(
            host=DB_HOST, user=DB_USER,
            password=DB_PASSWORD, database=DB_NAME
        )
    except mysql.connector.Error as err:
        print(json.dumps({"error": f"Database connection error: {err}"}))
        sys.exit(1)

# ── REGISTER ───────────────────────────────────────────
def register_face(user_id, image_path):
    image     = face_recognition.load_image_file(image_path)
    encodings = face_recognition.face_encodings(image)

    if not encodings:
        print(json.dumps({"error": "No face found in the image."}))
        sys.exit(1)

    face_encoding = encodings[0].tolist()
    encoding_json = json.dumps(face_encoding)

    conn   = get_db_connection()
    cursor = conn.cursor()

    try:
        # Delete old encoding for this user if exists
        cursor.execute("DELETE FROM user_faces WHERE user_id = %s", (user_id,))
        cursor.execute(
            "INSERT INTO user_faces (user_id, face_encoding) VALUES (%s, %s)",
            (user_id, encoding_json)
        )
        conn.commit()
        print(json.dumps({"success": True, "message": "Face registered successfully."}))
    except mysql.connector.Error as err:
        print(json.dumps({"error": f"Database error: {err}"}))
    finally:
        cursor.close()
        conn.close()

# ── COMPARE ────────────────────────────────────────────
def compare_face(image_path, threshold=0.6):
    image     = face_recognition.load_image_file(image_path)
    encodings = face_recognition.face_encodings(image)

    if not encodings:
        print(json.dumps({"error": "No face found in the image."}))
        sys.exit(1)

    input_encoding = encodings[0]

    conn   = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT user_id, face_encoding FROM user_faces")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()

    if not rows:
        print(json.dumps({"error": "No registered faces in database."}))
        sys.exit(1)

    matched_user = None
    min_distance = None

    for row in rows:
        user_id, encoding_json = row
        stored_encoding = np.array(json.loads(encoding_json))
        distance = float(np.linalg.norm(stored_encoding - input_encoding))
        if min_distance is None or distance < min_distance:
            min_distance = distance
            matched_user = user_id

    if min_distance is not None and min_distance < threshold:
        print(json.dumps({"success": True, "user_id": matched_user, "distance": min_distance}))
    else:
        print(json.dumps({"error": "No matching face found."}))

# ── MAIN ───────────────────────────────────────────────
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="AgriSense Face Recognition")
    parser.add_argument("mode", choices=["register", "compare"])
    parser.add_argument("--user_id",   type=int,   help="User ID (required for register)")
    parser.add_argument("--image",     required=True)
    parser.add_argument("--threshold", type=float, default=0.6)
    args = parser.parse_args()

    if args.mode == "register":
        if args.user_id is None:
            print(json.dumps({"error": "User ID is required for registration."}))
            sys.exit(1)
        register_face(args.user_id, args.image)
    elif args.mode == "compare":
        compare_face(args.image, args.threshold)
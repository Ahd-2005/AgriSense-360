#!/usr/bin/env python3
import argparse
import json
import sys
import mysql.connector
import face_recognition
import numpy as np
import cv2

# ── Database Configuration ─────────────────────────────
DB_HOST     = 'localhost'
DB_USER     = 'root'
DB_PASSWORD = ''
DB_NAME     = 'agrisense-360'

def get_db_connection():
    try:
        return mysql.connector.connect(
            host=DB_HOST, user=DB_USER,
            password=DB_PASSWORD, database=DB_NAME
        )
    except mysql.connector.Error as err:
        print(json.dumps({"error": f"Database connection error: {err}"}))
        sys.exit(1)

def load_and_preprocess(image_path):
    """
    Load image and ensure it is a plain 8-bit RGB array.
    The camera_capture_script saves PNG via cv2.imwrite which can produce
    BGRA (4-channel) images — dlib crashes on those with:
        RuntimeError: Unsupported image type, must be 8bit gray or RGB image.
    Fix: always convert to plain BGR first, then to RGB.
    """
    img_bgr = cv2.imread(image_path, cv2.IMREAD_UNCHANGED)

    if img_bgr is None:
        # Last-resort fallback — face_recognition's own loader
        return face_recognition.load_image_file(image_path)

    # ✅ KEY FIX: drop alpha channel if present (BGRA → BGR)
    if img_bgr.ndim == 3 and img_bgr.shape[2] == 4:
        img_bgr = cv2.cvtColor(img_bgr, cv2.COLOR_BGRA2BGR)

    # Ensure it's 3-channel at this point
    if img_bgr.ndim == 2:
        # Grayscale → BGR
        img_bgr = cv2.cvtColor(img_bgr, cv2.COLOR_GRAY2BGR)

    # Resize if too large (max 1200px wide — speeds up dlib)
    h, w = img_bgr.shape[:2]
    if w > 1200:
        img_bgr = cv2.resize(img_bgr, (1200, int(h * 1200 / w)))

    # CLAHE brightness/contrast enhancement (helps in dim/colored lighting)
    lab = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    l = clahe.apply(l)
    img_bgr = cv2.cvtColor(cv2.merge((l, a, b)), cv2.COLOR_LAB2BGR)

    # Convert BGR → RGB (what face_recognition / dlib expects)
    return cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)

def get_face_encodings(image_rgb):
    """Try progressively more aggressive detection until a face is found."""
    # Pass 1: standard
    enc = face_recognition.face_encodings(image_rgb, num_jitters=1, model="large")
    if enc:
        return enc

    # Pass 2: upsample x1 (finds smaller / partially off-centre faces)
    locs = face_recognition.face_locations(image_rgb, number_of_times_to_upsample=1, model="hog")
    if locs:
        enc = face_recognition.face_encodings(image_rgb, known_face_locations=locs, num_jitters=2, model="large")
        if enc:
            return enc

    # Pass 3: upsample x2 (slow but very thorough)
    locs = face_recognition.face_locations(image_rgb, number_of_times_to_upsample=2, model="hog")
    if locs:
        enc = face_recognition.face_encodings(image_rgb, known_face_locations=locs, num_jitters=2, model="large")
        if enc:
            return enc

    return []

# ── REGISTER ───────────────────────────────────────────
def register_face(user_id, image_path):
    image_rgb = load_and_preprocess(image_path)
    encodings = get_face_encodings(image_rgb)

    if not encodings:
        print(json.dumps({"error": "No face found. Make sure your face is clearly visible and well-lit."}))
        sys.exit(1)

    encoding_json = json.dumps(encodings[0].tolist())
    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        cursor.execute("DELETE FROM user_faces WHERE user_id = %s", (user_id,))
        cursor.execute("INSERT INTO user_faces (user_id, face_encoding) VALUES (%s, %s)", (user_id, encoding_json))
        conn.commit()
        print(json.dumps({"success": True, "message": "Face registered successfully."}))
    except mysql.connector.Error as err:
        print(json.dumps({"error": f"Database error: {err}"}))
    finally:
        cursor.close()
        conn.close()

# ── COMPARE ────────────────────────────────────────────
def compare_face(image_path, threshold=0.6):
    image_rgb = load_and_preprocess(image_path)
    encodings = get_face_encodings(image_rgb)

    if not encodings:
        print(json.dumps({"error": "No face found. Make sure your face is clearly visible and well-lit."}))
        sys.exit(1)

    input_encoding = encodings[0]

    conn = get_db_connection()
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
        uid, encoding_json = row
        stored = np.array(json.loads(encoding_json))
        dist = float(np.linalg.norm(stored - input_encoding))
        if min_distance is None or dist < min_distance:
            min_distance = dist
            matched_user = uid

    if min_distance is not None and min_distance < threshold:
        print(json.dumps({"success": True, "user_id": matched_user, "distance": round(min_distance, 4)}))
    else:
        dist_info = f"{min_distance:.4f}" if min_distance is not None else "N/A"
        print(json.dumps({"error": f"No match. Best distance: {dist_info} (need < {threshold})"}))

# ── MAIN ───────────────────────────────────────────────
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["register", "compare"])
    parser.add_argument("--user_id",   type=int)
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
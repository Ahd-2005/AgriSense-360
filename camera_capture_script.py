#!/usr/bin/env python3
"""
camera_capture_script.py — AgriSense 360
─────────────────────────────────────────
stream  : Opens webcam once, prints base64-encoded JPEG frames to stdout.
capture : Saves one high-quality PNG snapshot then exits.

pip install opencv-python
"""
import argparse, sys, os, base64, time

try:
    import cv2
except ImportError:
    print("ERROR: run pip install opencv-python", flush=True); sys.exit(1)


def open_camera():
    for idx in [0, 1, 2]:
        # Try DirectShow first (much faster init on Windows)
        cap = cv2.VideoCapture(idx, cv2.CAP_DSHOW)
        if not cap.isOpened():
            cap = cv2.VideoCapture(idx)
        if cap.isOpened():
            for _ in range(4): cap.read()   # warm-up
            return cap
    return None


def stream_mode():
    cap = open_camera()
    if cap is None:
        print("ERROR: Cannot open webcam", flush=True); sys.exit(1)

    cap.set(cv2.CAP_PROP_FRAME_WIDTH,  640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    cap.set(cv2.CAP_PROP_FPS, 20)

    print("READY", flush=True)   # signal Java the camera is open

    params = [cv2.IMWRITE_JPEG_QUALITY, 55]

    while True:
        ret, frame = cap.read()
        if not ret:
            time.sleep(0.04); continue

        frame = cv2.resize(frame, (480, 360))
        _, buf = cv2.imencode('.jpg', frame, params)
        print(base64.b64encode(buf.tobytes()).decode('ascii'), flush=True)
        time.sleep(0.05)   # ~20 fps


def capture_mode(output_path):
    cap = open_camera()
    if cap is None:
        print("ERROR: Cannot open webcam", flush=True); sys.exit(1)

    cap.set(cv2.CAP_PROP_FRAME_WIDTH,  1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
    for _ in range(8): cap.read()   # extra warm-up at higher res

    ret, frame = cap.read()
    cap.release()

    if not ret or frame is None:
        print("ERROR: Failed to capture frame", flush=True); sys.exit(1)

    os.makedirs(os.path.dirname(os.path.abspath(output_path)) or '.', exist_ok=True)
    cv2.imwrite(output_path, frame)
    print(f"OK:{output_path}", flush=True)


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("mode", choices=["stream", "capture"])
    p.add_argument("--output")
    args = p.parse_args()

    if args.mode == "stream":
        stream_mode()
    else:
        if not args.output:
            print("ERROR: --output required", flush=True); sys.exit(1)
        capture_mode(args.output)
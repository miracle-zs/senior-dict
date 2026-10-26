import os
import shutil
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS_MODELS_DIR = os.path.join(BASE_DIR, "app", "src", "main", "assets", "models")
JNI_DIR = os.path.join(BASE_DIR, "app", "src", "main", "jniLibs", "arm64-v8a")

# Local source path from POC project if available
LOCAL_POC_ROOT = "/Users/zhangshuai/元始天工/项目/POC项目_2026/数字识别/android-pda-ocr"

def ensure_models_and_libs():
    os.makedirs(ASSETS_MODELS_DIR, exist_ok=True)
    os.makedirs(JNI_DIR, exist_ok=True)

    det_model = os.path.join(ASSETS_MODELS_DIR, "PP-OCRv6_medium_det.mnn")
    rec_model = os.path.join(ASSETS_MODELS_DIR, "PP-OCRv6_medium_rec.onnx")

    models_ready = os.path.exists(det_model) and os.path.exists(rec_model)
    jni_ready = os.path.exists(os.path.join(JNI_DIR, "libmoldocr_native.so"))

    if models_ready and jni_ready:
        print("✓ All PP-OCRv6 models and native libraries are already in place.")
        return

    print("Restoring models and native libraries from local POC repository...")
    if os.path.exists(LOCAL_POC_ROOT):
        # Copy models
        poc_models_dir = os.path.join(LOCAL_POC_ROOT, "app", "src", "main", "assets", "models")
        for f in ["PP-OCRv6_medium_det.mnn", "PP-OCRv6_medium_rec.onnx"]:
            src = os.path.join(poc_models_dir, f)
            dst = os.path.join(ASSETS_MODELS_DIR, f)
            if os.path.exists(src) and not os.path.exists(dst):
                print(f"Copying {f}...")
                shutil.copy2(src, dst)

        # Copy native libs
        poc_jni_dir = os.path.join(LOCAL_POC_ROOT, "app", "src", "main", "jniLibs", "arm64-v8a")
        poc_native_so = os.path.join(LOCAL_POC_ROOT, "app", "build", "intermediates", "merged_native_libs", "release", "mergeReleaseNativeLibs", "out", "lib", "arm64-v8a", "libmoldocr_native.so")
        
        for f in os.listdir(poc_jni_dir):
            if f.endswith(".so"):
                src = os.path.join(poc_jni_dir, f)
                dst = os.path.join(JNI_DIR, f)
                if not os.path.exists(dst):
                    shutil.copy2(src, dst)
                    
        if os.path.exists(poc_native_so) and not os.path.exists(os.path.join(JNI_DIR, "libmoldocr_native.so")):
            shutil.copy2(poc_native_so, os.path.join(JNI_DIR, "libmoldocr_native.so"))

        print("✓ Setup complete! All assets and libraries restored successfully.")
    else:
        print("! Local POC path not found. Please place PP-OCRv6 models in app/src/main/assets/models/ manually.")

if __name__ == "__main__":
    ensure_models_and_libs()

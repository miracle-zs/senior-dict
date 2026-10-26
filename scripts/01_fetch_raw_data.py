import os
import urllib.request
import json

RAW_DIR = "/Users/zhangshuai/PycharmProjects/senior-dict/data/raw"
os.makedirs(RAW_DIR, exist_ok=True)

URL_WORD = "https://raw.githubusercontent.com/pwxcoo/chinese-xinhua/master/data/word.json"
URL_CHAIZI = "https://raw.githubusercontent.com/kfcd/chaizi/master/chaizi-jt.txt"

def download_file(url, target_path):
    print(f"Downloading {url} to {target_path}...")
    urllib.request.urlretrieve(url, target_path)
    size_mb = os.path.getsize(target_path) / (1024 * 1024)
    print(f"Downloaded {target_path} ({size_mb:.2f} MB)")

if __name__ == "__main__":
    word_path = os.path.join(RAW_DIR, "word.json")
    chaizi_path = os.path.join(RAW_DIR, "chaizi-jt.txt")

    if not os.path.exists(word_path):
        download_file(URL_WORD, word_path)
    else:
        print(f"{word_path} already exists.")

    if not os.path.exists(chaizi_path):
        download_file(URL_CHAIZI, chaizi_path)
    else:
        print(f"{chaizi_path} already exists.")

    # Inspect sample records from word.json
    with open(word_path, "r", encoding="utf-8") as f:
        words = json.load(f)
        print(f"Total entries in word.json: {len(words)}")
        # Print a sample entry
        for w in words[:2]:
            print("Sample entry:", json.dumps(w, ensure_ascii=False))

    # Inspect sample records from chaizi-jt.txt
    with open(chaizi_path, "r", encoding="utf-8") as f:
        lines = [next(f) for _ in range(5)]
        print("Sample chaizi entries:")
        for line in lines:
            print(line.strip())

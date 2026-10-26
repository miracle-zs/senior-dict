import os
import json
import sqlite3
import re
from collections import defaultdict

DATA_DIR = "/Users/zhangshuai/PycharmProjects/senior-dict/data/raw"
OUTPUT_DB = "/Users/zhangshuai/PycharmProjects/senior-dict/data/senior_dict.db"

TONE_MAP = {
    "ā": ("a", 1), "á": ("a", 2), "ǎ": ("a", 3), "à": ("a", 4),
    "ō": ("o", 1), "ó": ("o", 2), "ǒ": ("o", 3), "ò": ("o", 4),
    "ē": ("e", 1), "é": ("e", 2), "ě": ("e", 3), "è": ("e", 4),
    "ī": ("i", 1), "í": ("i", 2), "ǐ": ("i", 3), "ì": ("i", 4),
    "ū": ("u", 1), "ú": ("u", 2), "ǔ": ("u", 3), "ù": ("u", 4),
    "ǖ": ("v", 1), "ǘ": ("v", 2), "ǚ": ("v", 3), "ǜ": ("v", 4),
    "ü": ("v", 0), "ê": ("e", 0), "ń": ("n", 2), "ň": ("n", 3), "ǹ": ("n", 4),
    "m̄": ("m", 1), "ḿ": ("m", 2), "m̀": ("m", 4)
}

def parse_pinyin(pinyin_str):
    if not pinyin_str:
        return "", "", ""
    pinyins = [p.strip() for p in pinyin_str.replace("，", ",").split(",") if p.strip()]
    if not pinyins:
        return "", "", ""
    primary = pinyins[0]
    tone = 0
    raw_chars = []
    for ch in primary:
        if ch in TONE_MAP:
            base, t = TONE_MAP[ch]
            raw_chars.append(base)
            tone = t
        else:
            raw_chars.append(ch)
    raw = "".join(raw_chars).lower()
    audio_key = f"{raw}{tone}" if tone > 0 else raw
    return ", ".join(pinyins), raw, audio_key

def clean_explanation(text):
    if not text:
        return "常用字，暂无简明释义"
    
    # 1. Extract meaning from "本义..." if present
    benyi_matches = re.findall(r"本义[：:;；，,\s]*([^）\)\n。；;]+)", text)
    
    # 2. Extract numbered definitions like ⒈... ⒉...
    numbered = re.findall(r"[⒈⒉⒊⒋⒌⒍⒎⒏⒐0-9]+[\.、\s]*([^\n。；;]+)", text)
    
    # 3. Clean raw text
    text_clean = re.sub(r"\([^\)]+\)", "", text)
    text_clean = re.sub(r"〈[^〉]+〉", "", text_clean)
    text_clean = re.sub(r"--.*$", "", text_clean, flags=re.MULTILINE)
    text_clean = re.sub(r"——.*$", "", text_clean, flags=re.MULTILINE)
    
    candidates = []
    if benyi_matches:
        for m in benyi_matches:
            m = re.sub(r"也称.*", "", m).strip(" ;；,，。\"”'’")
            if len(m) >= 2 and m not in candidates:
                candidates.append(m)
                
    if numbered:
        for n in numbered:
            n = re.sub(r"～[^\s，。；]*", "", n).strip(" ;；,，。")
            if len(n) >= 2 and n not in candidates and not any(n.startswith(p) for p in ["见", "同", "另见"]):
                candidates.append(n)
                
    ignore_starts = ("同", "见", "又如", "郑码", "笔顺", "gbk", "u+", "部首", "另见")
    for line in text_clean.split("\n"):
        line = line.strip()
        if len(line) >= 3 and not any(line.startswith(p) for p in ignore_starts):
            line = re.split(r"又如|例如", line)[0].strip(" ;；,，。")
            if len(line) >= 2 and line not in candidates:
                candidates.append(line)
                
    final = []
    for c in candidates:
        if not re.match(r"^[a-zA-Z0-9\s]+$", c) and len(c) >= 2:
            final.append(c)
        if len(final) >= 2:
            break
            
    return "；".join(final) if final else "常用字，暂无简明释义"

def build_database():
    print("Step 1: Loading raw datasets...")
    word_file = os.path.join(DATA_DIR, "word.json")
    ci_file = os.path.join(DATA_DIR, "ci.json")
    idiom_file = os.path.join(DATA_DIR, "idiom.json")
    chaizi_file = os.path.join(DATA_DIR, "chaizi-jt.txt")

    with open(word_file, "r", encoding="utf-8") as f:
        words = json.load(f)
    print(f"Loaded {len(words)} characters.")

    # Build words/idioms mapping for each character
    print("Step 2: Building common word index from ci.json and idiom.json...")
    char_to_words = defaultdict(list)

    if os.path.exists(ci_file):
        with open(ci_file, "r", encoding="utf-8") as f:
            ci_list = json.load(f)
        for item in ci_list:
            w = item.get("ci", "").strip()
            if 2 <= len(w) <= 4:
                for c in set(w):
                    if len(char_to_words[c]) < 5 and w not in char_to_words[c]:
                        char_to_words[c].append(w)
        print(f"Mapped common words for {len(char_to_words)} characters.")

    if os.path.exists(idiom_file):
        with open(idiom_file, "r", encoding="utf-8") as f:
            idiom_list = json.load(f)
        for item in idiom_list:
            w = item.get("word", "").strip()
            if len(w) == 4:
                for c in set(w):
                    if len(char_to_words[c]) < 5 and w not in char_to_words[c]:
                        char_to_words[c].append(w)

    print("Step 3: Loading chaizi data...")
    char_to_components = defaultdict(list)
    if os.path.exists(chaizi_file):
        with open(chaizi_file, "r", encoding="utf-8") as f:
            for line in f:
                parts = line.strip().split("\t")
                if len(parts) >= 2:
                    c = parts[0].strip()
                    for decomposition in parts[1:]:
                        comp_tokens = decomposition.strip().split()
                        if comp_tokens:
                            comp_key = ",".join(comp_tokens)
                            if comp_key not in char_to_components[c]:
                                char_to_components[c].append(comp_key)
        print(f"Loaded decompositions for {len(char_to_components)} characters.")

    # Create SQLite Database
    print(f"Step 4: Compiling to SQLite: {OUTPUT_DB}...")
    if os.path.exists(OUTPUT_DB):
        os.remove(OUTPUT_DB)

    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.cursor()

    # Create characters table
    cursor.execute("""
        CREATE TABLE characters (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            char TEXT NOT NULL UNIQUE,
            pinyin TEXT NOT NULL,
            pinyin_raw TEXT NOT NULL,
            audio_key TEXT NOT NULL,
            radicals TEXT,
            stroke_count INTEGER,
            definition TEXT,
            examples TEXT
        );
    """)

    # Create char_components table
    cursor.execute("""
        CREATE TABLE char_components (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            char TEXT NOT NULL,
            parts TEXT NOT NULL,
            display_parts TEXT NOT NULL
        );
    """)

    # Populate characters
    seen_chars = set()
    char_rows = []
    comp_rows = []

    for entry in words:
        c = entry.get("word", "").strip()
        if not c or c in seen_chars:
            continue
        seen_chars.add(c)

        raw_pinyin = entry.get("pinyin", "").strip()
        pinyin_display, pinyin_raw, audio_key = parse_pinyin(raw_pinyin)
        radicals = entry.get("radicals", "").strip()
        
        try:
            strokes = int(entry.get("strokes", 0))
        except (ValueError, TypeError):
            strokes = 0

        raw_expl = entry.get("explanation", "") or entry.get("more", "")
        definition = clean_explanation(raw_expl)

        ex_words = char_to_words.get(c, [])
        # Sort words: 2-character words first, then 3, then 4
        ex_words_sorted = sorted(ex_words, key=lambda w: (len(w) != 2, len(w)))
        examples_str = "、".join(ex_words_sorted[:4])

        char_rows.append((c, pinyin_display, pinyin_raw, audio_key, radicals, strokes, definition, examples_str))

        # Add components for this character
        if c in char_to_components:
            for comp_key in char_to_components[c]:
                display = " + ".join(comp_key.split(","))
                comp_rows.append((c, comp_key, display))

    cursor.executemany("""
        INSERT INTO characters (char, pinyin, pinyin_raw, audio_key, radicals, stroke_count, definition, examples)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?);
    """, char_rows)

    cursor.executemany("""
        INSERT INTO char_components (char, parts, display_parts)
        VALUES (?, ?, ?);
    """, comp_rows)

    # Create indexes for 1ms queries
    cursor.execute("CREATE INDEX idx_char ON characters(char);")
    cursor.execute("CREATE INDEX idx_pinyin_raw ON characters(pinyin_raw);")
    cursor.execute("CREATE INDEX idx_parts ON char_components(parts);")
    cursor.execute("CREATE INDEX idx_comp_char ON char_components(char);")

    conn.commit()
    conn.close()

    db_size_mb = os.path.getsize(OUTPUT_DB) / (1024 * 1024)
    print(f"Successfully generated {OUTPUT_DB} ({db_size_mb:.2f} MB)")
    print(f"Total characters: {len(char_rows)}, Total decomposition rules: {len(comp_rows)}")

if __name__ == "__main__":
    build_database()

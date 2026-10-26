import os
import json
import sqlite3
import re
import shutil
from collections import defaultdict

DATA_DIR = "/Users/zhangshuai/PycharmProjects/senior-dict/data/raw"
OUTPUT_DB = "/Users/zhangshuai/PycharmProjects/senior-dict/data/senior_dict.db"
ASSET_DB = "/Users/zhangshuai/PycharmProjects/senior-dict/app/src/main/assets/senior_dict.db"

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

EXCLUDE_WORDS = {
    "福晋", "阿哥", "太监", "格格", "北京", "上海", "天津", "重庆", "福建", "广东", "山东", "河南", 
    "河北", "湖南", "湖北", "江苏", "浙江", "江西", "陕西", "山西", "四川", "贵州", "云南", "辽宁", 
    "吉林", "黑龙江", "安徽", "海南", "台湾", "香港", "澳门", "日本", "美国", "英国", "法国", "德国", 
    "俄国", "苏联", "朝鲜", "韩国", "柏林", "东京", "伦敦", "巴黎", "纽约", "人大", "政协", "中宣部",
    "康熙", "康有为", "少康", "太康", "保康", "康定", "徐寿辉", "天安门", "长安", "福州", "福临"
}

COMMON_LIFETIME_WORDS = {
    "柏树", "松柏", "康复", "小康", "安康", "长寿", "中药", "吃药", "草药", "开水", "喝水", "水果",
    "雨水", "清水", "电视", "电话", "电影", "电灯", "大家", "大人", "大米", "平安", "安心", "安静",
    "写字", "读书", "看书", "手机", "手表", "双手", "招手", "吃饭", "做饭", "生病", "医生", "医院",
    "福气", "福利", "祝福", "造福", "幸福", "身体", "健康", "朋友", "老师", "学生", "高兴", "开心",
    "生活", "生日", "生命", "家庭", "回家", "走路", "散步", "睡觉", "洗澡", "衣服", "穿衣", "买菜"
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

def clean_modern_explanation(text, char):
    if not text:
        return "常用汉字"
    
    # 1. Prioritize modern numbered definitions starting with ⒈ ⒉ ⒊ or 1. 2.
    numbered = re.findall(r"(?:[⒈⒉⒊⒋⒌⒍⒎⒏⒐]|\d+[\.、])\s*([^\n]+)", text)
    modern_defs = []
    
    for item in numbered:
        item = re.sub(r"--[^\n]+", "", item)
        item = re.sub(r"〈[^\>]+〉", "", item)
        item = re.sub(r"\([^\)]+\)", "", item)
        item = item.replace("～", char)
        item = item.strip(" ；;，,。\"”'")
        if len(item) >= 2 and not any(item.startswith(p) for p in ["见", "同", "另见", "通", "gbk", "u+"]):
            if item not in modern_defs:
                modern_defs.append(item)
    
    if modern_defs:
        return "；".join(modern_defs[:2])
    
    # 2. Fallback to general lines, skipping ancient Shuowen / poetry quotes
    lines = text.split("\n")
    cleaned_lines = []
    for l in lines:
        l = l.strip()
        if not l or "--" in l or "《" in l or "本义" in l or "形声" in l:
            continue
        l = re.sub(r"\([^\)]+\)", "", l)
        l = re.sub(r"〈[^\>]+〉", "", l)
        l = l.replace("～", char).strip(" ；;，,。\"”'")
        if len(l) >= 2 and l not in cleaned_lines and not any(l.startswith(p) for p in ["见", "同", "另见", "gbk"]):
            cleaned_lines.append(l)
            if len(cleaned_lines) >= 2:
                break
    
    return "；".join(cleaned_lines) if cleaned_lines else "常用汉字"

def build_modern_database():
    print("=== Step 1: Loading raw datasets ===")
    word_file = os.path.join(DATA_DIR, "word.json")
    chaizi_file = os.path.join(DATA_DIR, "chaizi-jt.txt")
    jieba_file = os.path.join(DATA_DIR, "jieba_dict.txt")
    ci_file = os.path.join(DATA_DIR, "ci.json")

    with open(word_file, "r", encoding="utf-8") as f:
        words = json.load(f)
    print(f"Loaded {len(words)} characters from word.json.")

    # Step 2: Build high-frequency everyday vocabulary mapping
    print("=== Step 2: Processing modern everyday vocabulary from jieba_dict.txt ===")
    words_scored = []
    with open(jieba_file, "r", encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split()
            if len(parts) >= 2:
                w = parts[0]
                if w in EXCLUDE_WORDS:
                    continue
                if 2 <= len(w) <= 4 and re.match(r"^[\u4e00-\u9fa5]+$", w):
                    try:
                        freq = int(parts[1])
                    except ValueError:
                        freq = 0
                    pos = parts[2] if len(parts) >= 3 else "n"
                    
                    score = float(freq)
                    if w in COMMON_LIFETIME_WORDS:
                        score += 50000.0
                    if pos.startswith(("ns", "nr", "nt", "nz", "j")):
                        score /= 500.0
                    if pos.startswith(("s", "f")):
                        score /= 10.0
                    if len(w) == 2:
                        score *= 5.0
                    elif len(w) == 3:
                        score *= 1.2
                    
                    words_scored.append((w, score))

    words_scored.sort(key=lambda x: -x[1])
    print(f"Scored {len(words_scored)} modern words.")

    # Build inverted index
    char_to_words = defaultdict(list)
    for w, _ in words_scored:
        for c in set(w):
            if len(char_to_words[c]) < 4 and w not in char_to_words[c]:
                char_to_words[c].append(w)

    print(f"Indexed modern high-frequency words for {len(char_to_words)} characters.")

    # Step 3: Load chaizi data
    print("=== Step 3: Loading chaizi decomposition rules ===")
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

    # Step 4: Fallback to ci.json for rare characters missing words
    if os.path.exists(ci_file):
        print("=== Step 4: Populating rare characters with filtered 2-character words ===")
        with open(ci_file, "r", encoding="utf-8") as f:
            ci_list = json.load(f)
        for item in ci_list:
            w = item.get("ci", "").strip()
            if w in EXCLUDE_WORDS:
                continue
            if len(w) == 2 and re.match(r"^[\u4e00-\u9fa5]{2}$", w):
                for c in set(w):
                    if len(char_to_words[c]) < 4 and w not in char_to_words[c]:
                        char_to_words[c].append(w)

    # Step 5: Build SQLite database
    print(f"=== Step 5: Generating clean database: {OUTPUT_DB} ===")
    if os.path.exists(OUTPUT_DB):
        os.remove(OUTPUT_DB)

    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.cursor()

    cursor.execute("""
        CREATE TABLE characters (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            char TEXT NOT NULL UNIQUE,
            pinyin TEXT NOT NULL,
            pinyin_raw TEXT NOT NULL,
            audio_key TEXT NOT NULL,
            radicals TEXT,
            stroke_count INTEGER NOT NULL,
            definition TEXT,
            examples TEXT
        );
    """)

    cursor.execute("""
        CREATE TABLE char_components (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            char TEXT NOT NULL,
            parts TEXT NOT NULL,
            display_parts TEXT NOT NULL
        );
    """)

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
        definition = clean_modern_explanation(raw_expl, c)

        ex_words = char_to_words.get(c, [])
        examples_str = "、".join(ex_words[:4])

        char_rows.append((c, pinyin_display, pinyin_raw, audio_key, radicals, strokes, definition, examples_str))

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

    cursor.execute("CREATE INDEX idx_char ON characters(char);")
    cursor.execute("CREATE INDEX idx_pinyin_raw ON characters(pinyin_raw);")
    cursor.execute("CREATE INDEX idx_parts ON char_components(parts);")
    cursor.execute("CREATE INDEX idx_comp_char ON char_components(char);")

    conn.commit()
    conn.close()

    print(f"Generated {OUTPUT_DB} with {len(char_rows)} characters and {len(comp_rows)} component rules.")

    # Step 6: Align with Room schema and hash
    print("=== Step 6: Aligning with Android Room Database Schema ===")
    align_room_database(OUTPUT_DB, ASSET_DB)
    print("Database build and Room alignment complete!")

def align_room_database(src_db, dst_db):
    temp_db = dst_db + ".tmp"
    if os.path.exists(temp_db):
        os.remove(temp_db)

    src_conn = sqlite3.connect(src_db)
    src_c = src_conn.cursor()

    dst_conn = sqlite3.connect(temp_db)
    dst_c = dst_conn.cursor()

    dst_c.execute("""
        CREATE TABLE IF NOT EXISTS `characters` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `char` TEXT NOT NULL,
            `pinyin` TEXT NOT NULL,
            `pinyin_raw` TEXT NOT NULL,
            `audio_key` TEXT NOT NULL,
            `radicals` TEXT,
            `stroke_count` INTEGER NOT NULL,
            `definition` TEXT,
            `examples` TEXT
        );
    """)

    dst_c.execute("""
        CREATE TABLE IF NOT EXISTS `char_components` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `char` TEXT NOT NULL,
            `parts` TEXT NOT NULL,
            `display_parts` TEXT NOT NULL
        );
    """)

    dst_c.execute("""
        CREATE TABLE IF NOT EXISTS `search_history` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `char` TEXT NOT NULL,
            `timestamp` INTEGER NOT NULL,
            `source` TEXT NOT NULL
        );
    """)

    dst_c.execute("""
        CREATE TABLE IF NOT EXISTS `favorites` (
            `char` TEXT NOT NULL,
            `saved_at` INTEGER NOT NULL,
            PRIMARY KEY(`char`)
        );
    """)

    dst_c.execute("""
        CREATE TABLE IF NOT EXISTS room_master_table (
            id INTEGER PRIMARY KEY,
            identity_hash TEXT
        );
    """)

    ROOM_IDENTITY_HASH = "e61ffa3dad5472092184123bdd1f5837"
    dst_c.execute(f"INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '{ROOM_IDENTITY_HASH}');")

    src_c.execute("SELECT id, char, pinyin, pinyin_raw, audio_key, radicals, COALESCE(stroke_count, 0), definition, examples FROM characters")
    char_rows = src_c.fetchall()
    dst_c.executemany("""
        INSERT INTO characters (id, char, pinyin, pinyin_raw, audio_key, radicals, stroke_count, definition, examples)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, char_rows)

    src_c.execute("SELECT id, char, parts, display_parts FROM char_components")
    comp_rows = src_c.fetchall()
    dst_c.executemany("""
        INSERT INTO char_components (id, char, parts, display_parts)
        VALUES (?, ?, ?, ?)
    """, comp_rows)

    dst_conn.commit()
    src_conn.close()
    dst_conn.close()

    shutil.move(temp_db, dst_db)
    print(f"Copied and aligned Room database to {dst_db}")

if __name__ == "__main__":
    build_modern_database()

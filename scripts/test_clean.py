import json
import re

def clean_explanation(raw_text):
    if not raw_text:
        return "", ""
    
    # Remove dictionary metadata lines
    lines = raw_text.split('\n')
    cleaned_lines = []
    examples = []
    
    # Patterns to remove
    ignore_patterns = [
        r'郑码.*', r'笔顺.*', r'gbk.*', r'u\+?[0-9a-fA-F]+', r'部首.*',
        r'--《.*》', r'——《.*》', r'--.*《.*》', r'——.*《.*》',
        r'\[[a-zA-Z\s]+\]',  # English translations like [hoarse]
        r'────────.*', r'—{3,}',
        r'另见.*', r'同.*”。',
    ]
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
        
        # Check ignore
        skip = False
        for pat in ignore_patterns:
            if re.search(pat, line):
                skip = True
                break
        if skip:
            continue
            
        # Remove grammar tags like 〈名〉, 〈动〉, 〈形〉, 〈叹〉, 〈方〉
        line = re.sub(r'〈[^〉]+〉', '', line)
        line = re.sub(r'［[^］]+］', '', line)
        
        # Clean citations at end of line (e.g. --清·蒲松龄...)
        line = re.sub(r'--.*$', '', line)
        line = re.sub(r'——.*$', '', line)
        
        # Extract examples like 如嗄哑,嗄嘶 or 如"嗄饭"
        ex_match = re.findall(r'(?:如|见|～)([\u4e00-\u9fa5]+)', line)
        for ex in ex_match:
            if 1 < len(ex) <= 6:
                examples.append(ex)
                
        line = line.strip()
        if line and len(line) > 1 and not line.startswith(('1.', '2.', '3.')):
            cleaned_lines.append(line)
        elif line and len(line) > 2:
            cleaned_lines.append(line)
            
    # Deduplicate and limit definitions
    final_defs = []
    seen = set()
    for l in cleaned_lines:
        # Simplify line
        l_sub = re.sub(r'^[0-9一二三四五六七八九十]+[、\.\s]*', '', l).strip()
        if l_sub and l_sub not in seen and len(l_sub) >= 2:
            seen.add(l_sub)
            final_defs.append(l_sub)
            if len(final_defs) >= 3:
                break
                
    # Format definition as concise numbered list
    def_str = "；".join(final_defs[:2]) if final_defs else "暂无详细释义"
    
    # Deduplicate examples
    ex_list = []
    for ex in examples:
        if ex not in ex_list:
            ex_list.append(ex)
            if len(ex_list) >= 4:
                break
    ex_str = "，".join(ex_list)
    
    return def_str, ex_str

if __name__ == "__main__":
    with open("/Users/zhangshuai/PycharmProjects/senior-dict/data/raw/word.json", "r", encoding="utf-8") as f:
        words = json.load(f)
        
    word_map = {w["word"]: w for w in words}
    test_chars = ["柏", "嗄", "药", "露", "饕", "餮", "芪", "芩"]
    
    for ch in test_chars:
        if ch in word_map:
            entry = word_map[ch]
            d, ex = clean_explanation(entry.get("explanation", "") or entry.get("more", ""))
            print(f"【{ch}】 拼音: {entry['pinyin']} | 部首: {entry['radicals']} | 笔画: {entry['strokes']}")
            print(f"  释义: {d}")
            print(f"  组词: {ex}\n")
        else:
            print(f"【{ch}】 not found in word.json")

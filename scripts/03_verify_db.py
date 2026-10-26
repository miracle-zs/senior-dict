import sqlite3
import time

DB_PATH = "/Users/zhangshuai/PycharmProjects/senior-dict/data/senior_dict.db"

def test_queries():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    test_words = ["柏", "药", "芪", "芩", "饕", "餮", "赢", "露", "糖"]
    print("=== 1. 单字精准查询测试 ===")
    for w in test_words:
        t0 = time.perf_counter()
        cursor.execute("SELECT char, pinyin, audio_key, radicals, stroke_count, definition, examples FROM characters WHERE char = ?", (w,))
        row = cursor.fetchone()
        t1 = time.perf_counter()
        elapsed_ms = (t1 - t0) * 1000
        if row:
            print(f"[{row[0]}] 读音: {row[1]} (音轨:{row[2]}) | 部首: {row[3]} ({row[4]}画) | 耗时: {elapsed_ms:.2f}ms")
            print(f"    释义: {row[5]}")
            print(f"    组词: {row[6]}")
        else:
            print(f"[{w}] 未找到！")

    print("\n=== 2. 汉字拆解反查测试（按部件搜字） ===")
    test_components = ["木,白", "艹,约", "草,约", "亡,口,月,贝,凡", "雨,路", "日,月"]
    for comp in test_components:
        t0 = time.perf_counter()
        cursor.execute("""
            SELECT c.char, c.pinyin, c.definition, cc.display_parts
            FROM char_components cc
            JOIN characters c ON cc.char = c.char
            WHERE cc.parts = ?
        """, (comp,))
        rows = cursor.fetchall()
        t1 = time.perf_counter()
        elapsed_ms = (t1 - t0) * 1000
        print(f"部件 [{comp}] -> 匹配到 {len(rows)} 个字 (耗时 {elapsed_ms:.2f}ms):")
        for r in rows:
            print(f"   -> {r[0]} ({r[1]}): {r[3]} | 释义: {r[2]}")

    print("\n=== 3. 拼音反查测试 ===")
    t0 = time.perf_counter()
    cursor.execute("SELECT char, pinyin FROM characters WHERE pinyin_raw = ? LIMIT 5", ("bai",))
    rows = cursor.fetchall()
    t1 = time.perf_counter()
    print(f"拼音 'bai' -> {[f'{r[0]}({r[1]})' for r in rows]} (耗时: {(t1-t0)*1000:.2f}ms)")

    conn.close()

if __name__ == "__main__":
    test_queries()

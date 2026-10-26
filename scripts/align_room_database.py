import sqlite3
import os
import shutil

DB_PATH = "/Users/zhangshuai/PycharmProjects/senior-dict/app/src/main/assets/senior_dict.db"
TEMP_DB_PATH = "/Users/zhangshuai/PycharmProjects/senior-dict/app/src/main/assets/senior_dict_temp.db"

def align_database():
    if os.path.exists(TEMP_DB_PATH):
        os.remove(TEMP_DB_PATH)

    src_conn = sqlite3.connect(DB_PATH)
    src_c = src_conn.cursor()

    dst_conn = sqlite3.connect(TEMP_DB_PATH)
    dst_c = dst_conn.cursor()

    # Exact Room DDL schemas matching SeniorDictDatabase_Impl.java
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

    # Exact hash from SeniorDictDatabase_Impl.java
    ROOM_IDENTITY_HASH = "e61ffa3dad5472092184123bdd1f5837"
    dst_c.execute(f"INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '{ROOM_IDENTITY_HASH}');")

    # Copy characters
    print("Copying characters...")
    src_c.execute("SELECT id, char, pinyin, pinyin_raw, audio_key, radicals, COALESCE(stroke_count, 0), definition, examples FROM characters")
    char_rows = src_c.fetchall()
    dst_c.executemany("""
        INSERT INTO characters (id, char, pinyin, pinyin_raw, audio_key, radicals, stroke_count, definition, examples)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, char_rows)
    print(f"Copied {len(char_rows)} characters.")

    # Copy components
    print("Copying char_components...")
    src_c.execute("SELECT id, char, parts, display_parts FROM char_components")
    comp_rows = src_c.fetchall()
    dst_c.executemany("""
        INSERT INTO char_components (id, char, parts, display_parts)
        VALUES (?, ?, ?, ?)
    """, comp_rows)
    print(f"Copied {len(comp_rows)} components.")

    dst_conn.commit()
    src_conn.close()
    dst_conn.close()

    # Overwrite original database
    shutil.move(TEMP_DB_PATH, DB_PATH)
    print("Successfully aligned senior_dict.db with Room schema and identity hash!")

if __name__ == "__main__":
    align_database()

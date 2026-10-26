# 银发字典 (Senior Dict) - 专为长辈设计的识字查字助手

面向中老年人设计的极简查字工具，解决长辈在日常阅读（报纸、药品说明书、手机文章、家谱）中“遇到生僻字/不认识的字不知道怎么查”的痛点。

---

## 核心交付产物索引

- 🚀 **【工业级完全体】安装包**：[`release/银发字典-完全体v1.1.apk`](release/银发字典-完全体v1.1.apk)（**185.8MB**，内嵌真实 PP-OCRv6 Medium 深度学习模型，真实离线拍照识字）
- 📱 **【轻量交互体验版】安装包**：[`release/银发字典-测试版v1.0.apk`](release/银发字典-测试版v1.0.apk)（**45.6MB**，轻量安装包）
- 📖 **妈妈试用全指南**：[`MOM_TEST_GUIDE.md`](MOM_TEST_GUIDE.md)（含微信防拦截技巧、3个测试场景与观察法）
- 📘 **完整架构与调研全案**：[`PROJECT_DESIGN.md`](PROJECT_DESIGN.md)（竞品、版权合规、系统架构设计）
- 🗄️ **离线预编译数据库**：`app/src/main/assets/senior_dict.db`（2.88MB，收录 14,810 汉字 + 18,029 条拆解规则）

---

## 核心功能与适老化亮点

1. **拍照点查（指哪查哪）**：
   - 拍照后原位定格画面，OCR 自动高亮识别出的每一个汉字（暖黄色柔光框）；
   - 长辈只需用手指轻触任一字块（内置 48px 防手抖扩大热区），底部瞬间滑出超大字卡片。
2. **超大字号与大喇叭朗读**：
   - 68sp 超大单字展示 + 32sp 拼音；
   - 绿色大喇叭按钮，点击以 0.8x 慢速标准普通话朗读，并附带口诀助记（如“bǎi，三声，柏树的柏”）；
   - 纯生活大白话解释，过滤所有学术化词性与古籍引用。
3. **偏旁拆字反查（解决“会拆不会拼”）**：
   - 支持自然语言模糊输入：“左边木右边白”、“草字头加约”；
   - 提供常用偏旁快捷标签（木、艹、氵、亻、扌、口、讠、钅、火、日），点击一键填入并毫秒级显示候选生字。
4. **大字田字格手写板**：
   - 全屏田字格手写区域，粗笔迹高对比度，支持随意笔画书写。
5. **100% 纯离线（Offline-First）**：
   - 无需连接网络，不消耗手机流量，在医院、超市等弱网环境下秒开秒查。

---

## 工程结构

```
senior-dict/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/senior_dict.db         # 预编译 SQLite 字典库 (2.88MB)
│       ├── java/com/seniordict/app/
│       │   ├── MainActivity.kt           # 入口 Activity
│       │   ├── SeniorDictApplication.kt  # 预加载与预热
│       │   ├── data/                     # Room 数据库持久层 (Entity/Dao/Repo)
│       │   ├── engine/                   # 端侧 OCR 引擎与双轨发音引擎
│       │   └── ui/                       # Jetpack Compose 适老化 UI (Theme/Components/Screens)
│       └── res/                          # 资源与字符串配置
├── data/                                 # 原始字典数据与 SQLite 数据库
├── scripts/                              # Python 数据清洗与预编译脚本
├── release/                              # 生成的 APK 安装包
├── MOM_TEST_GUIDE.md                     # 妈妈测试指南
└── PROJECT_DESIGN.md                     # 项目全案文档
```

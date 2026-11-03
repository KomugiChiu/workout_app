# Komugi Workout（Android App）

把 workout 訓練菜單變成手機 App：**菜單 / 紀錄 / 動作說明 / 選項** 四個分頁，離線可用。

## 功能

- **菜單**：9 份內建菜單（恢復日、A/B/C 循環、TRX、居家輕裝、全啞鈴、彈力帶＋壺鈴、手臂＋握力），每份附動作組次建議、**訓練部位**、影片連結、動作說明入口
- **一鍵開始紀錄**：菜單底部「開始紀錄（今天）」→ 自動建立當日訓練並預載全部動作
- **紀錄（月曆式）**：
  - 上方月曆選擇日期 → 下方即時列出該日訓練；**左右滑動可換月**，有紀錄的日期下方顯示綠點
  - 清單每筆可單獨刪除（🗑️＋二次確認）；點列進入詳細頁
  - **複製到別天**：清單列「複製」或詳細頁右上「複製」→ 選日期 → 完整複製動作與組數
  - 「新紀錄」建立在目前選取的日期
  - 每個動作可記錄 **重量 / 次數 / 組數 / 時間 / 器材**（Add 下拉選單）
  - **自重選項**：重量欄可勾選「自重（不計重量）」，明細顯示「自重」並計入組數統計
  - **建議值自動預填**：新增時自動帶入上一組數值；首次則從菜單建議解析（器材 10kg×2 → 10、次數 8–10 → 8、30 秒 → 30、組數 3–4 → 3）
  - **部位圖示**：每個動作顯示**主要 2–3 個部位的 3D 解剖局部圖**（例：臥推→胸／肩／三頭；深蹲→股四／臀／核心），灰色身體＋**主題色目標肌肉**（森林綠／海洋藍／暖陽橘自動切換）＋前後標籤
  - 總計（組數／總次數／最高重量／自重組數／總時間）＋逐組明細
  - 日期預設當天，可點右上日曆修改；垃圾桶可刪除整筆紀錄
  - 點任一組列可編輯重量／次數／時間／自重；「＋組」快速複製上一組
  - **新增動作**：可搜尋下拉選單（輸入關鍵字過濾）＋**依部位篩選**；手動輸入的新動作與部位會自動加入動作庫（持久化）
  - **訓練總結**：紀錄詳細頁右上「總結」→ 完成度圓環、動作／組數（目標）、總次數、訓練量、訓練部位分佈，以及每個動作的完成度與明細；超額組數保留但不計入完成度
- **動作說明**：11 份完整拆解（**肌肉部位說明全圖鑑**／划船／RDL／保加利亞／壺鈴擺盪／飛鳥／農夫行走／三停系列／單手擺盪／錘式彎舉／過頭三頭伸展）；支援**名稱／內文搜尋**；詳情顯示訓練部位＋局部圖；Markdown 內文的**背景、標題、連結、表格、引言區塊全部跟隨主題色與深色模式**，影片連結可點開
- **選項**：三種主題顏色（森林綠／海洋藍／暖陽橘）× 三種外觀模式（跟隨系統／淺色／深色），每種主題都有專屬深色配色；**頂部標題列、狀態列、底部導覽列、系統導覽列、動作說明內文與肌肉部位圖都使用主題色**；即時切換並持久保存
- **本機儲存**：Room（SQLite），完全離線

## 技術規格

| 項目 | 內容 |
|------|------|
| 語言 / UI | Kotlin 2.0.21 + Jetpack Compose (Material 3) |
| minSdk / targetSdk | 26（Android 8.0）/ 34 |
| 資料庫 | Room 2.6.1（KSP） |
| 建置 | Gradle 8.7 + AGP 8.5.2（wrapper 已附） |

## 圖示來源

肌肉解剖圖取自 [js-rich-body-highlighter](https://github.com/crmapache/js-rich-body-highlighter)（MIT License）的人體正／背面插圖與肌群遮罩，於建置前離線裁切並依主題（綠／藍／橘）著色，App 本身完全離線、不載入任何網路圖片。

## 建置

```bash
export ANDROID_HOME=/home/user/starboard-toolchains/AndroidSdk
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

已驗證產出：`KomugiWorkout-debug.apk`（約 10 MB，debug 簽章，可直接安裝側載）。

## 專案結構

```
app/src/main/
├── assets/
│   ├── menus.json          # 8 份菜單（首次啟動種子資料）
│   ├── guides.json         # 動作說明索引（11 份）
│   └── exercises/*.md      # 11 份動作詳解
└── java/com/komugi/workout/
    ├── MainActivity.kt
    ├── data/               # Room 實體、DAO、Repository、菜單模型
    ├── ui/
    │   ├── AppRoot.kt      # 三頁導航＋底部列
    │   ├── menu/           # 菜單列表／詳情
    │   ├── log/            # 紀錄月曆／詳情／訓練總結／Add 下拉
    │   ├── guide/          # 動作說明
    │   ├── settings/       # 選項（主題切換）
    │   └── theme/          # 三種主題與 ThemeManager
    └── util/               # 日期格式、Markdown→HTML、WebView
```

## 資料模型（v5 正規化）

```
exercises（動作主檔）
  id / name / parts / videoUrl / guideId / isCustom

menus ──< menu_exercises >── exercises（多對多）
             └ 器材、建議組數/次數、休息（菜單專屬設定）

sessions ──< entries ──< sets
             └ 訓練紀錄快照（含 exerciseId 供追溯；事後改菜單不影響歷史）
```

- 首次啟動／升級後由 `assets/menus.json` 自動種子匯入 DB
- **種子版本機制**：`app_meta.seed_version` 與程式內 `SEED_VERSION` 比對；修改菜單後遞增版本，已安裝的 App 會重建菜單（保留自訂動作與訓練紀錄）
- 自訂動作寫入 `exercises`（`isCustom = true`），可重複選用
- 舊版 `custom_exercises` 於 migration 4→5 併入 `exercises` 後移除

## 後續可加

- Google Drive AppDataFolder 自動備份（需 Google Cloud OAuth Client ID＋SHA-1 註冊）
- 匯出／匯入 JSON（換裝置還原）
- 訓練歷史圖表（容量／重量趨勢）
- 計時器（組間休息倒數）

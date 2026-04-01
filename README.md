# ♍ Virgo - 領域驅動 AI 助手 (Enterprise Domain-Driven AI Assistant)

Virgo 是一個基於 本地端大型語言模型 (Local LLM) 與 檢索增強生成 (RAG) 技術所打造的智能助手，此專案採用嚴謹的 六角架構 (Hexagonal Architecture) 與 領域驅動設計 (DDD) 開發，旨在提供一個高隱私、高穩定且具備多模式切換能力的 AI 基礎設施。

**為什麼取名Virgo? 主要是因為協助我建置的 AI 是 Gemini (雙子座)，所以想說取個星座名，再加上我是處女座 (Virgo)，就這樣取囉**



## 核心特性 (Core Features)

**1. 多重人格切換與記憶物理隔離 (Multi-Mode & Memory Isolation)**

>* 雲地協同策略： 支援本地端 DeepSeek-R1 (高隱私、低成本) 與雲端 Gemini 2.0 Flash (高邏輯推理、超大上下文) 的無縫切換。
>* 模式動態切換：支援「通用 AI 助手」與特定領域專家（如「DDD 架構專家」）無縫切換。
>* 模式動態路由： 透過 **策略模式 (Strategy Pattern)** 實作 ChatStrategyFactory，確保系統能在不改動核心邏輯的情況下，擴充多種 AI 專家模式。
>* 複合記憶體架構：獨創 conversationId:mode 複合鍵值設計，確保不同 AI 模式的對話上下文在 Redis 中絕對物理隔離，，解決雲端與本地記憶「串供」問題。
>* F5 重新整理防禦：前端狀態卸載後，可自動透過持久化 ID 向後端 API 拉取 Redis 歷史紀錄，實現企業級的 F5 防禦與跨裝置對話接續連貫體驗。

**2. 高可靠性 RAG 知識餵養 (Idempotent RAG Ingestion)**

>* 雙重檢查鎖 (Double-Check Locking)：在知識庫進食 (Ingestion) 過程中，利用檔案 SHA-256 雜湊值配合分散式鎖，確保高併發下大檔案解析與 Embedding API 呼叫的絕對冪等性。
>* 交易邊界優化：將耗時的 Tika 文本解析與 Token 拆分剝離於資料庫交易之外，僅在寫入向量庫時開啟事務，最大化資料庫連線效率。

**3. 執行緒安全的 Reactive WebSocket 串流**

>* Reactive Streaming： 整合 Spring AI 與 Project Reactor (Flux)，提供毫秒級的 Token 串流輸出。
>* 並行安全防護：導入 ConcurrentWebSocketSessionDecorator，徹底解決高頻率 Chunk 推播導致的 WebSocket Session Crash 問題。
>* 資源自動回收：嚴密的連線生命週期管理。當使用者斷線或關閉分頁時，系統會自動 dispose() 進行中的 AI 推理任務，防止孤兒執行緒 (Orphan Threads) 耗盡伺服器算力或 API 額度。
>* 深度模型解析：專屬防腐層 (ACL) 自動攔截並解析 DeepSeek 的 <think> 思維鏈與 Gemini 的多模態標籤。

註. 本地模型會即時結算 Token 消耗統計。

**4. 個人對話知識盒 (Knowledge Base & DDD)**

>* 收藏與標籤管理：支援將高價值的 QA 對話納入個人收藏，並以多標籤 (Tags) 進行分類。
>* 嚴格的領域封裝：FavoriteMessage 聚合根採用靜態工廠方法與私有建構子，確保標籤數量與不變量 (Invariants) 的絕對安全，並避開 Hibernate 不變集合 (Immutable Collection) 的持久化陷阱，落實不同 AI 模式間的收藏資料物理隔離。

**5. 敏捷的對話體驗優化 (Agile Dialogue UX Optimization)**

>**智慧脈絡回溯演算法 (Context-Aware Backtracking Algorithm)：**
>* 多功能驅動：此演算法同時驅動「重新生成 (Resend)」與「知識收藏 (Favorite)」兩大核心互動。
>* 精準問答對齊 (QA Pair Alignment)：透過回溯對話流，系統能精準抓取觸發當前回答的原始提問。這確保了所有沉澱至資料庫的知識皆以完整的「問答對」形式儲存，避免了傳統 AI 助手僅能收藏孤立文本而失去上下文脈絡的痛點。

>**零摩擦知識沉澱 (Zero-Friction Ingestion)：**
>* 追加式重新生成 (Append Regenerate)：支援在不破壞現有對話流的前提下，快速發起二次推理，完整保留架構師與 AI 的邏輯推演軌跡。
>* 一鍵式快速收藏：結合 RxJS 的非同步處理技術，使用者可針對特定高品質回答進行「閃電收藏」，系統會自動處理標籤關聯與模式隔離，極大化知識採集的效率。

>**高效能前端響應：**
>* RxJS 防抖與狀態鎖定：在搜尋、重新生成與分頁切換過程中，導入嚴密的防抖 (Debounce) 與 Loading 狀態控制，確保在高併發串流環境下，UI 依然能保持絕對的穩定與流暢。

## 系統架構 (Architecture)

本專案嚴格遵守 Port-Adapter (六角架構)，確保核心業務邏輯不受外部框架污染：

>* Presentation/Interface Layer (展示層)：Angular 17+, PrimeNG, WebSocket Handler, REST Controller。
>* Application Layer (應用服務層)：負責對話編排 (Orchestration)、RAG 進食邏輯與分散式鎖控管。完全不依賴特定 AI 框架。
>* Domain Layer (領域層)：定義 FavoriteMessage、ProcessedDocument 等核心業務實體與領域視圖 (Domain View)。
>* Infrastructure Layer (基礎設施層)：

**外部依賴 :**
* AI 引擎：Ollama (DeepSeek-R1 等本地模型) 透過 Spring AI 橋接。
* 記憶體管理：Redis (實作滑動視窗歷史管理與自動 TTL)。
* 關聯式資料庫：PostgreSQL (儲存收藏與文檔元數據)。
* 向量資料庫：Vector Store (用於 RAG 語義搜尋)。


## 技術棧 (Tech Stack)

## Backend: 
>* Java
>* JDK 21 
>* Spring Boot 4.0.4
>* Spring AI 2.0.0-M3
>* Project Reactor
>* MapStruct 1.6.3
>* Spring Data JPA
>* Spring Data Redis

## Frontend: 
>* Angular 18.2.0+
>* PrimeNG 18.0.2-patch.1
>* PrimeFlex 4.0.0
>* RxJS 7.8.0
>* WebSocket API

## AI & Data: 
>* Local Brain: Ollama (DeepSeek-R1:7b)
>* Cloud Brain: Google Gemini 2.0 Flash (Tier 1 Membership)
>* Vector DB: PgVector (PostgreSQL)
>* Document Analysis: Apache Tika

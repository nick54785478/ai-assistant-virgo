/**
 * 核心領域數據：代表一則訊息的本質內容
 */
export interface ChatMessage {
  sender: 'user' | 'bot' | 'system';
  rawText: string; // 原始文本
  thinkingText?: string; // 思考過程
  outputText?: string; // 最終回答
}

/**
 * UI 狀態與元數據：負責存放與內容無關的技術/狀態資訊
 */
export interface ChatMessageMeta {
  isThinking: boolean; // 是否正在思考 (UI 狀態)
  favoriteId?: number; // PostgreSQL 的主鍵 (持久化狀態)
  isFavorited?: boolean; // 新增：純粹用來控制前端星星是否亮起的 UI 狀態
  isFavoriteLoading?: boolean; // 新增：防止連點的非同步鎖

  // 未來可擴充：
  // error?: string;       // 錯誤訊息
  // isTranslated?: boolean; // 是否已翻譯
}

/**
 * 組合式物件：對接 UI 的最小單元
 */
export interface ChatCell {
  data: ChatMessage; // 組合內容
  meta: ChatMessageMeta; // 組合狀態
}

// export interface ChatMeta {
//   isThinking: boolean;
//   favoriteId?: number; // 未來由查詢 API 賦予的 DB ID
//   isFavorited?: boolean; // 🚀 新增：純粹用來控制前端星星是否亮起的 UI 狀態
//   isFavoriteLoading?: boolean; // 新增：防止連點的非同步鎖
// }

import { PagedQueriedView } from '../../../shared/models/paged-queried.model';

// 定義收藏項目的介面 (方便我們做強型別管理)
export interface FavoriteItem {
  id: number; // 對應後端的 Long id (前端通常轉為 string 處理)
  question: string; // 使用者問了什麼
  answer: string; // AI 答了什麼
  tags: string[]; // 標籤存儲
  chatId: string; // 關聯的對話 ID
  createdAt: string; // 建立時間
}

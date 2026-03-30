import { ChatCell } from "./chat-message.model";

export interface ChatHistoryGettenResource {
  code: string;
  message: string;
  data: ChatCell[]; // 後端的 ChatHistoryDto 結構剛好 100% 吻合前端的 ChatCell
}

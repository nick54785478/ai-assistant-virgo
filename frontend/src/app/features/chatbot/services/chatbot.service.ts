import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { ChatCell, ChatMessage } from '../models/chat-message.model';
import { BehaviorSubject } from 'rxjs/internal/BehaviorSubject';
import { ChatHistoryGettenResource } from '../models/chat-history.model';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class ChatbotService {
  private readonly baseUrl = `${environment.apiEndpoint}/chat`;
  private ws!: WebSocket;
  /**
   * 持久化會話 ID，綁定於瀏覽器 LocalStorage，用於跨連線的記憶接續
   * */
  private conversationId: string;

  /**
   * 核心狀態：利用 Map 實現不同 AI 模式間的畫面物理隔離
   **/
  private chatHistories = new Map<string, ChatCell[]>();

  /**
   * 目前畫面上正在使用的 AI 模式，預設是 "GENERAL"
   **/
  private activeMode: string = 'GENERAL';

  /**
   * 響應式資料流，UI 元件僅需訂閱此 Subject 即可實現畫面驅動
   **/
  private cellsSubject = new BehaviorSubject<ChatCell[]>([]);
  public cells$ = this.cellsSubject.asObservable();
  /**
   * 紀錄當前正在接收串流的 AI 訊息區塊，以便將 Token 附加進去
   **/
  private currentBotCell: ChatCell | null = null;

  constructor(private http: HttpClient) {
    // 初始化持久化 ID：若無則產生一組標準 UUID
    const storedId = localStorage.getItem('v_conversation_id');
    if (storedId) {
      this.conversationId = storedId;
    } else {
      this.conversationId = crypto.randomUUID();
      localStorage.setItem('v_conversation_id', this.conversationId);
    }
  }

  /**
   * 取得當前模式的對話陣列 (Lazy Loading 初始化)
   */
  private get currentCells(): ChatCell[] {
    if (!this.chatHistories.has(this.activeMode)) {
      this.chatHistories.set(this.activeMode, []);
    }
    return this.chatHistories.get(this.activeMode)!;
  }

  /**
   * <b>切換 AI 模式 (支援 F5 記憶回溯)</b>
   * <p>
   * 當使用者點擊左側清單切換模式時呼叫。
   * 若發現該模式在前端記憶體 (Map) 中無資料，會主動向後端發起 GET 請求撈取歷史紀錄。
   * </p>
   * @param mode 目標 AI 模式 (e.g., 'GENERAL', 'EXPERT_DDD')
   */
  public switchMode(mode: string) {
    this.activeMode = mode;
    this.currentBotCell = null; // 切換時重置當前輸入狀態

    // 判斷：如果 Map 裡沒有這個模式的紀錄 (例如剛按下 F5 重整，或是第一次點開)
    if (
      !this.chatHistories.has(mode) ||
      this.chatHistories.get(mode)!.length === 0
    ) {
      // 先給一個空陣列，避免畫面報錯
      this.chatHistories.set(mode, []);

      //  修正：加入完整的模式名稱對應，避免系統訊息誤報
      let modeName = '通用 AI 助手';
      if (mode === 'EXPERT_DDD') {
        modeName = 'DDD 架構專家 - Virgo';
      }
      if (mode === 'EXPERT_GEMINI') {
        modeName = '雲端架構大腦 - Gemini';
      }

      // 呼叫後端 API 撈取歷史紀錄
      this.http
        .get<ChatHistoryGettenResource>(
          `${this.baseUrl}/history?conversationId=${this.conversationId}&mode=${mode}`,
        )
        .subscribe({
          next: (res) => {
            if (res.data && res.data.length > 0) {
              // 把後端洗好的乾淨 DTO 直接塞進 Map，因為結構一樣，一行代碼搞定！
              this.chatHistories.set(mode, res.data);
            } else {
              // 如果後端資料庫也說沒有紀錄，才顯示全新的歡迎語
              const modeName =
                mode === 'EXPERT_DDD' ? 'DDD 架構專家 - Virgo' : '通用 AI 助手';
              this.addSystemMessage(
                `🟢 已切換至 [${modeName}]，記憶空間已獨立。`,
              );
            }
            // 資料就緒後，更新畫面
            this.updateSubject();
          },
          error: (err) => {
            console.error('獲取歷史紀錄失敗', err);
            // const modeName =
            //   mode === 'EXPERT_DDD' ? 'DDD 架構專家 - Virgo' : '通用 AI 助手';
            this.addSystemMessage(
              `🟢 已切換至 [${modeName}] (歷史紀錄讀取失敗，請檢查連線)。`,
            );
            this.updateSubject();
          },
        });
    } else {
      // 如果 Map 裡面已經有紀錄了 (單純在左側選單兩個模式之間切換)，直接更新畫面
      this.updateSubject();
    }
  }

  // --- WebSocket 的相關方法 ---

  /**
   * 建立 WebSocket 連線，並註冊各類事件監聽器。
   */
  connect() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    this.ws = new WebSocket(`${protocol}//${host}/chat-ws`);

    this.ws.onopen = () => {
      // 這裡可以選擇不發送系統訊息，避免每次重整都多一條「連線成功」，或者維持原樣
      // this.addSystemMessage('🟢 連線成功！AI 助手已就緒。');
    };

    this.ws.onmessage = (event) => {
      const token = event.data;
      // 攔截系統控制訊號：清除記憶
      if (token === '[CLEARED]') {
        this.chatHistories.set(this.activeMode, []);
        this.currentBotCell = null;
        this.addSystemMessage(' 對話記憶已清除，準備開始新對話。');
        return;
      }

      // 攔截系統控制訊號：AI 推理結束
      if (token === '[DONE]') {
        if (this.currentBotCell) {
          this.currentBotCell.meta.isThinking = false; // 解除 UI 上的 Loading 動畫
        }
        this.currentBotCell = null;
        this.updateSubject();
        return;
      }

      // 一般 Token 串流處理
      if (!this.currentBotCell) {
        // 初始化新的 AI 回覆區塊
        this.currentBotCell = {
          data: {
            sender: 'bot',
            rawText: '',
            thinkingText: '',
            outputText: '',
          },
          meta: { isThinking: true },
        };
        this.currentCells.push(this.currentBotCell);
      }
      // 將收到的 Token 累加，並即時解析出思考過程與最終答案
      this.currentBotCell.data.rawText += token;
      this.parseStreamedText(this.currentBotCell);
      this.updateSubject();
    };

    this.ws.onclose = (event) => {
      console.warn('WebSocket 連線已關閉', event);
      if (this.currentBotCell) {
        this.currentBotCell.meta.isThinking = false;
        this.currentBotCell.data.rawText += '\n\n🔴 [系統提示] 連線已中斷。';
        this.parseStreamedText(this.currentBotCell);
        this.currentBotCell = null;
        this.updateSubject();
      } else {
        this.addSystemMessage('🔴 WebSocket 連線已關閉。');
      }
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket 發生錯誤', error);
      if (this.currentBotCell) {
        this.currentBotCell.meta.isThinking = false;
        this.currentBotCell = null;
        this.updateSubject();
      }
      this.addSystemMessage('🔴 連線發生錯誤，請檢查伺服器狀態。');
    };
  }

  disconnect() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.close();
    }
  }

  /**
   * 發送使用者訊息至後端 (打包為 JSON 格式)
   */
  sendMessage(text: string) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.currentCells.push({
        data: { sender: 'user', rawText: text, outputText: text },
        meta: { isThinking: false },
      });
      this.updateSubject();

      const payload = {
        mode: this.activeMode,
        text: text,
        conversationId: this.conversationId,
      };
      this.ws.send(JSON.stringify(payload));
    } else {
      this.addSystemMessage('🔴 無法發送訊息，目前處於斷線狀態。');
    }
  }

  /**
   * 發送清除記憶體的控制指令
   */
  clearChatMemoryViaWs() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const payload = {
        mode: this.activeMode,
        text: '/clear',
        conversationId: this.conversationId,
      };
      this.ws.send(JSON.stringify(payload));
    }
  }

  copyToClipboard(text: string): Promise<void> {
    return navigator.clipboard.writeText(text);
  }

  private addSystemMessage(text: string) {
    this.currentCells.push({
      data: { sender: 'system', rawText: text, outputText: text },
      meta: { isThinking: false },
    });
    this.updateSubject();
  }

  private updateSubject() {
    this.cellsSubject.next([...this.currentCells]);
  }

  /**
   * <b>前端防腐層：解析 DeepSeek 特定標籤</b>
   * <p>將混在一起的 rawText，切割出 `<think>` 區塊與正式的 `outputText`。</p>
   */
  private parseStreamedText(cell: ChatCell) {
    const raw = cell.data.rawText;
    const thinkStart = raw.indexOf('<think>');
    const thinkEnd = raw.indexOf('</think>');

    if (thinkStart !== -1) {
      if (thinkEnd !== -1) {
        // 思考已結束，有最終輸出
        cell.data.thinkingText = raw.substring(thinkStart + 7, thinkEnd).trim();
        cell.data.outputText = raw.substring(thinkEnd + 8).trim();
        cell.meta.isThinking = raw.substring(thinkEnd + 8).trim() === '';
      } else {
        // 正在思考中，尚未遇到閉合標籤
        cell.data.thinkingText = raw.substring(thinkStart + 7).trim();
        cell.data.outputText = '';
        cell.meta.isThinking = true;
      }
    } else {
      // 模型不支援或未輸出 think 標籤
      cell.data.thinkingText = '';
      cell.data.outputText = raw.trim();
      cell.meta.isThinking = false;
    }
  }
}

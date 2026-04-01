import {
  Component,
  OnInit,
  ChangeDetectorRef,
  ViewChild,
  ElementRef,
  OnDestroy,
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PRIME_COMPONENTS } from '../../../shared/shared-primeng';
import { ChatbotService } from '../services/chatbot.service';
import { SystemMessageService } from '../../../shared/service/system-message.service';
import { Subscription } from 'rxjs/internal/Subscription';
import { ChatLayoutComponent } from '../../layout/pages/chat-layout/chat-layout.component';
import { KnowledgeIngestionComponent } from './knowledge-ingestion/knowledge-ingestion.component';
import { ChatCell } from '../models/chat-message.model';

import { FavoriteMessageService } from '../services/favorite-message.service';
import { ChipModule } from 'primeng/chip';
import { FavoriteTagsComponent } from './favorite-tags/favorite-tags.component';
import { AiModelCardComponent } from '../../aimodel/pages/ai-model-card/ai-model-card.component';
import { FavoriteManagerComponent } from './favorite-manager/favorite-manager.component';

/**
 * <b>[Frontend Smart Component] Virgo 智能助手主畫面</b>
 * <p>
 * 職責：作為系統的主控台 (Container Component)。
 * 1. 負責訂閱 `ChatbotService` 的資料流，將狀態渲染至 UI。
 * 2. 處理使用者的所有互動行為 (發送訊息、切換模式、開啟對話框)。
 * 3. 協調 `FavoriteMessageService` 完成個人知識庫的存取。
 * </p>
 * <p><b>架構亮點：</b> 完全解耦了 WebSocket 的實作細節，畫面僅依賴 `cells$` 觀察者模式驅動。</p>
 */
@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ChipModule,
    PRIME_COMPONENTS,
    ChatLayoutComponent,
    KnowledgeIngestionComponent,
    FavoriteTagsComponent,
    AiModelCardComponent,
    FavoriteManagerComponent,
  ],
  templateUrl: './chatbot.component.html',
  styleUrl: './chatbot.component.scss',
})
export class ChatbotComponent implements OnInit, OnDestroy {
  currentExpertName: string = 'DDD 架構專家 - Virgo';
  currentExpertDesc: string = '已載入核心領域規範 (基於 DeepSeek-R1)';

  /**
   * 核心聊天數據源 (由 Service 同步過來)
   **/
  cells: ChatCell[] = [];

  userInput: string = '';
  /**
   * 控制送出按鈕與 Loading 動畫的狀態
   **/
  isLoading: boolean = false;
  /**
   * 當前選擇的 AI 模式，預設為 GENERAL
   **/
  selectedMode: string = 'GENERAL';

  // --- 彈出視窗 (Dialog) 控制區 ---
  showIngestionDialog: boolean = false;
  displayTagDialog: boolean = false;
  displayFavoritesManager: boolean = false;

  /**
   * 靜態專家資訊配置，用於切換模式時更新 UI 視覺
   **/
  expertInfo = {
    GENERAL: {
      name: '通用 AI 助手',
      desc: '通用模式',
      icon: 'pi pi-box',
      hasMenu: false, // 通用助手不需要進階選單 (例如沒有知識庫可以 Ingest)
    },
    EXPERT_DDD: {
      name: 'DDD 架構專家 - Virgo',
      desc: '已載入核心領域規範 (RAG)',
      icon: 'pi pi-sitemap',
      hasMenu: true, // 需要進階選單
    },
    EXPERT_GEMINI: {
      name: '雲端架構大腦 - Gemini',
      desc: '高邏輯推理與超大文本分析',
      icon: 'pi pi-cloud',
      hasMenu: true, // Gemini 也需要進階選單 (查看收藏等)
    },
  };

  /**
   * 準備寫入資料庫的暫存標籤陣列
   **/
  pendingTags: string[] = [];
  /**
   * 當前正在被操作 (準備加入收藏) 的對話區塊
   **/
  currentTaggingCell: ChatCell | null = null;
  /**
   * 使用者在 Input 框輸入的單一標籤暫存
   **/
  currentTag: string = '';

  /**
   * 聊天室滾動視窗的 DOM 參考，用於自動滾動到底部
   **/
  @ViewChild('scroller') scroller!: any;
  private chatSub!: Subscription;

  constructor(
    private cd: ChangeDetectorRef,
    private sysMsgService: SystemMessageService,
    private chatbotService: ChatbotService,
    private favoriteService: FavoriteMessageService,
  ) {}

  /**
   * <b>元件初始化生命週期</b>
   * <p>啟動 WS 連線，並開始監聽 Service 的對話狀態流。</p>
   */
  ngOnInit() {
    this.chatbotService.connect();

    // 初始化時，確保 Service 的模式與 Component 同步
    this.chatbotService.switchMode(this.selectedMode);

    // 訂閱資料流：任何對話更新都會觸發此回呼
    this.chatSub = this.chatbotService.cells$.subscribe((cellsData) => {
      this.cells = cellsData;

      // 判斷是否處於 AI 思考中狀態 (用於鎖定 UI)
      const lastCell = cellsData[cellsData.length - 1];
      this.isLoading =
        lastCell?.data.sender === 'bot' && !!lastCell?.meta.isThinking;

      // 強制觸發變更偵測，確保 WebSocket 高頻推播時畫面能即時刷新
      this.cd.detectChanges();
      this.scrollToBottom();
    });
  }

  /**
   * <b>元件銷毀生命週期</b>
   * <p>防禦性釋放資源，斷開 WS 連線與取消 RxJS 訂閱，防止記憶體洩漏 (Memory Leak)。</p>
   */
  ngOnDestroy() {
    if (this.chatSub) {
      this.chatSub.unsubscribe();
    }
    this.chatbotService.disconnect();
  }

  /**
   * 將 expertInfo 物件轉換為陣列，供 HTML 的 @for 迴圈使用
   */
  get expertList() {
    return Object.entries(this.expertInfo).map(([key, value]) => ({
      modeId: key,
      ...value,
    }));
  }

  /**
   * <b>切換 AI 模式</b>
   * <p>通知底層 Service 切換物理記憶體 Map，實現不同 AI 間的狀態隔離。</p>
   * @param mode 目標模式 (e.g., 'GENERAL', 'EXPERT_DDD')
   */
  switchMode(mode: string) {
    this.selectedMode = mode;
    console.log(`已切換至: ${mode}`);

    // 通知 Service 切換背後的 Map 陣列
    this.chatbotService.switchMode(mode);

    // 切換後確保畫面滾到最下面
    this.scrollToBottom();
  }

  /**
   * 取得當前模式對應的靜態視覺資訊
   **/
  get currentInfo() {
    return this.expertInfo[this.selectedMode as keyof typeof this.expertInfo];
  }

  /**
   * <b>發送使用者訊息</b>
   * <p>過濾空白輸入後，委派給 Service 進行 WS 傳輸。</p>
   */
  sendMessage() {
    if (this.userInput.trim()) {
      // 因為 Service 現在內部會自己抓取 activeMode，我們只需要傳文字即可
      this.chatbotService.sendMessage(this.userInput);

      this.userInput = '';
      this.isLoading = true;

      this.scrollToBottom();
    }
  }

  /**
   * 支援 Enter 鍵快捷發送
   **/
  handleKeyPress(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      this.sendMessage();
    }
  }

  /**
   * 開啟加入收藏的 Dialog，並記錄當前要收藏的是哪一則回答。
   */
  openTagDialog(cell: ChatCell) {
    this.currentTaggingCell = cell;
    this.pendingTags = [];
    this.displayTagDialog = true;
  }

  /**
   * <b>確認儲存收藏</b>
   * <p>
   * 維運筆記：此處實作了「脈絡回溯」演算法。
   * 由於使用者點擊的是 AI 的回答，我們必須往前尋找最近一次的「使用者提問」，
   * 才能組合成有意義的「問答對 (QA Pair)」存入資料庫。
   * </p>
   */
  confirmSaveFavorite() {
    if (!this.currentTaggingCell) return;

    const cell = this.currentTaggingCell;
    const answer = cell.data.outputText || cell.data.rawText;
    const question = this.findLastUserQuestion(cell);

    cell.meta.isFavoriteLoading = true;
    this.displayTagDialog = false;

    // 呼叫 Service 寫入後端 DB
    this.favoriteService
      .saveFavorite(question, answer, this.pendingTags, this.selectedMode)
      .subscribe({
        next: (res) => {
          // 更新 UI 狀態：點亮星星、綁定 DB 主鍵 ID
          cell.meta.isFavorited = true;
          cell.meta.favoriteId = res.id;
          cell.meta.isFavoriteLoading = false;

          this.sysMsgService.showSuccess(
            '已加入收藏',
            `已成功存檔，標籤：${this.pendingTags.join(', ') || '無'}`,
          );
          this.cd.detectChanges();
        },
        error: () => {
          cell.meta.isFavoriteLoading = false;
          this.sysMsgService.showError(
            '收藏失敗',
            '伺服器忙碌中，請稍後再試。',
          );
          this.cd.detectChanges();
        },
      });
  }

  /**
   * <b>內部尋找提問脈絡演算法</b>
   * <p>從當前 AI 回答所在的陣列位置，反向遍歷尋找最近的一筆 User 訊息。</p>
   * @param currentCell 當前的 AI 回答區塊
   * @returns 使用者的原始提問文字
   */
  private findLastUserQuestion(currentCell: ChatCell): string {
    const currentIndex = this.cells.indexOf(currentCell);
    if (currentIndex === -1) return '';

    for (let i = currentIndex - 1; i >= 0; i--) {
      const cell = this.cells[i];
      if (cell.data.sender === 'user') {
        return cell.data.outputText || cell.data.rawText || '';
      }
    }
    return '無提問脈絡';
  }

  /**
   * <b>清空當前對話</b>
   * <p>發送 /clear 指令至後端，觸發 Redis 記憶體清除機制。</p>
   */
  clearChat() {
    this.sysMsgService.confirmAction(
      '重置對話',
      '您確定要清空目前的記憶嗎？這將無法復原。',
      () => {
        // 自動處理清空「當前模式」的記憶
        this.chatbotService.clearChatMemoryViaWs();
        this.sysMsgService.showSuccess('指令已送出', '正在重置對話...');
      },
    );
  }

  /**
   * <b>安全複製至剪貼簿</b>
   * <p>支援「部分選取複製」與「全文字複製」的智慧判斷。</p>
   */
  copyToClipboard(fullText: string) {
    const selection = window.getSelection()?.toString();
    const textToCopy =
      selection && selection.trim().length > 0 ? selection : fullText;

    this.chatbotService
      .copyToClipboard(textToCopy)
      .then(() => {
        const isPartial = selection && selection.trim().length > 0;
        const summary = isPartial ? '選取內容已複製' : '全文字複製成功';
        this.sysMsgService.showSuccess(summary, '已安全存入剪貼簿');
      })
      .catch(() => {
        this.sysMsgService.showError('複製失敗', '瀏覽器權限或連線異常');
      });
  }

  /**
   * <b>一鍵快速加入收藏 (不帶自定義標籤)</b>
   */
  addFavorite(cell: ChatCell) {
    const contentToSave = cell.data.outputText || cell.data.rawText;
    if (!contentToSave || cell.meta.isFavoriteLoading) return;

    // 🚀 修正 1：利用我們寫好的演算法，往前找尋真正的「使用者提問」，而不是存入 'bot'
    const question = this.findLastUserQuestion(cell);

    cell.meta.isFavoriteLoading = true;
    this.cd.detectChanges();

    // 🚀 修正 2：對齊 API 規格 (問題, 回答, 空標籤陣列, 當前模式)
    this.favoriteService
      .saveFavorite(question, contentToSave, [], this.selectedMode)
      .subscribe({
        next: (res) => {
          cell.meta.isFavorited = true;
          cell.meta.favoriteId = res.id;
          cell.meta.isFavoriteLoading = false;

          this.sysMsgService.showSuccess(
            '已加入收藏',
            `這則訊息已安全存檔 (ID: #${res.id})。`,
          );
          this.cd.detectChanges();
        },
        error: () => {
          cell.meta.isFavoriteLoading = false;
          this.sysMsgService.showError('收藏失敗', '目前無法處理您的請求');
          this.cd.detectChanges();
        },
      });
  }

  removeFavorite(cell: ChatCell) {
    if (!cell.meta.favoriteId || cell.meta.isFavoriteLoading) return;

    cell.meta.isFavoriteLoading = true;
    this.cd.detectChanges();

    this.favoriteService.removeFavorite(cell.meta.favoriteId).subscribe({
      next: (res) => {
        cell.meta.isFavorited = false;
        cell.meta.favoriteId = undefined;
        cell.meta.isFavoriteLoading = false;
        this.sysMsgService.showSuccess(
          '已取消收藏',
          res.message || `紀錄已移除。`,
        );
        this.cd.detectChanges();
      },
      error: () => {
        cell.meta.isFavoriteLoading = false;
        this.sysMsgService.showError('取消失敗', '目前無法與伺服器連線');
        this.cd.detectChanges();
      },
    });
  }

  /**
   * <b>平滑滾動至對話底部</b>
   * <p>使用 setTimeout 避開 Angular 生命週期更新的空窗期，確保 DOM 渲染完畢後再滾動。</p>
   */
  private scrollToBottom() {
    setTimeout(() => {
      if (this.scroller && this.scroller.contentViewChild) {
        const el = this.scroller.contentViewChild.nativeElement;
        el.scrollTop = el.scrollHeight;
      }
    }, 50);
  }

  // --- 標籤暫存操作 ---
  addPendingTag() {
    const tag = this.currentTag.trim();
    if (tag && !this.pendingTags.includes(tag)) {
      this.pendingTags.push(tag);
      this.currentTag = '';
    }
  }

  // removePendingTag(tagToRemove: string) {
  //   this.pendingTags = this.pendingTags.filter((t) => t !== tagToRemove);
  // }

  /**
   * <b>處理專家模型卡片的動作菜單 (Action Menu)</b>
   */
  handleModelAction(action: 'INGEST' | 'FAVORITES' | 'CLEAR') {
    switch (action) {
      case 'INGEST':
        this.showIngestionDialog = true;
        break;
      case 'FAVORITES':
        this.displayFavoritesManager = true;
        break;
      case 'CLEAR':
        this.clearChat();
        break;
      default:
        console.warn('【系統警告】未知的模型操作指令:', action);
    }
  }

  /**
   * <b>重新生成回答 (Resend)</b>
   * <p>找出觸發此回答的原始提問，並再次發送給 AI 進行推論。</p>
   */
  resendMessage(cell: ChatCell) {
    if (this.isLoading) return; // 如果 AI 正在思考，鎖定按鈕避免重複觸發

    const question = this.findLastUserQuestion(cell);
    
    if (question && question !== '無提問脈絡') {
      // 🚀 直接委派給 Service 再次發送問題
      this.chatbotService.sendMessage(question);
      this.isLoading = true;
      this.scrollToBottom();
      
      this.sysMsgService.showSuccess('重新生成中', '已重新發送您的提問...');
    } else {
      this.sysMsgService.showError('無法重新生成', '找不到對應的歷史提問脈絡');
    }
  }
}

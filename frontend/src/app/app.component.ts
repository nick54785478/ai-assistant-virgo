import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { PRIME_COMPONENTS } from './shared/shared-primeng';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, PRIME_COMPONENTS, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  title = 'chatbot-ui';

  constructor(
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
  ) {}

  /**
   * 💡 當對話框關閉時觸發 (對應 HTML 的 onHide)
   * 處女座維運建議：可用來重置特定操作狀態或記錄 Log
   */
  confirmDialogHide() {
    console.log('全域確認對話框已隱藏');
  }

  // --- 維運工具方法：供未來呼叫 ---

  /**
   * 測試用的通知方法
   */
  showTestToast() {
    this.messageService.add({
      key: 'msg', // 必須與 HTML 裡的 key="msg" 一致
      severity: 'success',
      summary: '系統通知',
      detail: '處女座 AI 助手已成功掛載！',
      icon: 'pi-check',
    });
  }
}

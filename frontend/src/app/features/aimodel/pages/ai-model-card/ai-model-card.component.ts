import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MenuItem } from 'primeng/api';
import { SpeedDialModule } from 'primeng/speeddial';
import { FormsModule } from '@angular/forms';
import { PRIME_COMPONENTS } from '../../../../shared/shared-primeng';
import { PopoverModule } from 'primeng/popover';

@Component({
  selector: 'app-ai-model-card',
  standalone: true,
  imports: [
    CommonModule,
    SpeedDialModule,
    FormsModule,
    PRIME_COMPONENTS,
    PopoverModule,
  ],
  templateUrl: './ai-model-card.component.html',
  styleUrls: ['./ai-model-card.component.scss'],
})
export class AiModelCardComponent {
  @Input() modeId!: string;
  @Input() cardTitle: string = '預設標題';
  @Input() cardSubtitle?: string = '';
  @Input() isSelected: boolean = false;
  @Input() showSpeedDial: boolean = false;

  @Output() onSelect = new EventEmitter<string>();
  @Output() onAction = new EventEmitter<'INGEST' | 'FAVORITES' | 'CLEAR'>();

  handleClick() {
    this.onSelect.emit(this.modeId);
  }

  // 🚀 新增：負責處理選單點擊、關閉 Popover，並通知父元件
  executeAction(
    action: 'INGEST' | 'FAVORITES' | 'CLEAR',
    popover: any,
    event: Event,
  ) {
    event.stopPropagation(); // 阻止事件冒泡，避免點到卡片觸發 switchMode
    popover.hide(); // 自動收起懸浮選單
    this.onAction.emit(action); // 將動作傳給 chatbot.component
  }
}

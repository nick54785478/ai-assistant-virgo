import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

@Component({
  selector: 'app-chat-layout',
  standalone: true,
  // 確保有引入 Button 和 Tooltip，漢堡選單才出得來
  imports: [CommonModule, ButtonModule, TooltipModule],
  templateUrl: './chat-layout.component.html',
  styleUrl: './chat-layout.component.scss',
})
export class ChatLayoutComponent {
  // 控制側邊欄開關的狀態 (預設展開)
  isSidebarVisible: boolean = true;

  toggleSidebar() {
    this.isSidebarVisible = !this.isSidebarVisible;
  }
}

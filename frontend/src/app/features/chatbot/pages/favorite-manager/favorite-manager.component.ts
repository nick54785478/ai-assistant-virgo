import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
// 記得確認 PRIME_COMPONENTS 裡有 DialogModule, InputTextModule, ButtonModule, ChipModule
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { FavoriteItem } from '../../models/favorite-item.model';
import { PaginatorModule } from 'primeng/paginator';
import { Subscription } from 'rxjs/internal/Subscription';
import { Subject } from 'rxjs/internal/Subject';
import { debounceTime } from 'rxjs/internal/operators/debounceTime';
import { distinctUntilChanged } from 'rxjs/internal/operators/distinctUntilChanged';
import { FavoriteMessageService } from '../../services/favorite-message.service';
import { FavoriteTagsComponent } from '../favorite-tags/favorite-tags.component';
import { FavoriteTagEditorComponent } from './favorite-tag-editor/favorite-tag-editor.component';

@Component({
  selector: 'app-favorite-manager',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DialogModule,
    InputTextModule,
    ButtonModule,
    ChipModule,
    PaginatorModule,
    FavoriteTagEditorComponent,
  ],
  templateUrl: './favorite-manager.component.html',
  styleUrls: ['./favorite-manager.component.scss'],
})
export class FavoriteManagerComponent implements OnInit, OnDestroy {
  // 攔截 visible，確保每次打開 Dialog 時載入最新資料
  private _visible = false;

  @Input()
  set visible(val: boolean) {
    this._visible = val;
    if (val) this.loadFavorites();
  }
  get visible() {
    return this._visible;
  }

  showTagDialog = false;
  tempTags: string[] = []; // 編輯中的暫存標籤
  editingId: number | null = null; // 紀錄正在改哪一筆

  @Output() visibleChange = new EventEmitter<boolean>();

  searchQuery: string = '';
  searchSubject = new Subject<string>(); // 用於 RxJS 防抖
  private sub?: Subscription;

  // 分頁與狀態
  first: number = 0;
  rows: number = 5;
  totalRecords: number = 0;
  isLoading: boolean = false; // 載入狀態
  displayFavorites: FavoriteItem[] = []; // 實際顯示資料

  constructor(private favoriteService: FavoriteMessageService) {}

  ngOnInit() {
    // RxJS 魔法：延遲 500ms 且字串有變動才發送 API 請求
    this.sub = this.searchSubject
      .pipe(debounceTime(500), distinctUntilChanged())
      .subscribe(() => {
        this.first = 0; // 搜尋條件改變，重置回第一頁
        this.loadFavorites();
      });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe(); // 避免 Memory Leak
  }

  // 核心：向後端請求資料
  loadFavorites() {
    this.isLoading = true;
    // PrimeNG 的 first 是「第幾筆」，Spring Boot 的 page 是「第幾頁」，需要轉換
    const page = Math.floor(this.first / this.rows);

    this.favoriteService
      .getFavorites(this.searchQuery, '', page, this.rows)
      .subscribe({
        next: (res) => {
          this.displayFavorites = res.data.content;
          this.totalRecords = res.data.totalElements;
          this.isLoading = false;
        },
        error: (err) => {
          console.error('讀取收藏失敗', err);
          this.isLoading = false;
        },
      });
  }

  onSearchChange() {
    this.searchSubject.next(this.searchQuery); // 觸發 RxJS 流
  }

  onPageChange(event: any) {
    this.first = event.first;
    this.rows = event.rows;
    this.loadFavorites(); // 換頁時重新請求
  }

  closeDialog() {
    this.visible = false;
    this.visibleChange.emit(this.visible);
    this.searchQuery = ''; // 關閉時清空搜尋
  }

  removeFavorite(id: number) {
    // 呼叫後端刪除 API，成功後重新整理當前頁面
    this.favoriteService.removeFavorite(id).subscribe(() => {
      this.loadFavorites();
    });
  }

  goToMessage(chatId: string) {
    console.log('跳轉到對話 ID:', chatId);
    this.closeDialog();
  }

  // 打開編輯標籤彈窗
  openEditTags(item: FavoriteItem) {
    this.editingId = item.id;
    this.tempTags = [...item.tags]; // 深拷貝，避免直接動到列表資料
    this.showTagDialog = true;
  }

  // 接收標籤元件的「儲存」事件
  handleTagsUpdate() {
    if (this.editingId === null) return;

    this.favoriteService.updateTags(this.editingId, this.tempTags).subscribe({
      next: () => {
        this.showTagDialog = false;
        this.loadFavorites(); // 重新整理清單
        // 可以加上一個 Toast 通知「更新成功」
      },
      error: (err) => console.error('更新標籤失敗', err),
    });
  }
}

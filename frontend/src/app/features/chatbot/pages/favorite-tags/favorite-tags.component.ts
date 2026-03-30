import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PRIME_COMPONENTS } from '../../../../shared/shared-primeng';
import { ChipModule } from 'primeng/chip';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-favorite-tags',
  standalone: true,
  imports: [CommonModule, FormsModule, ChipModule, PRIME_COMPONENTS],
  templateUrl: './favorite-tags.component.html',
  styleUrl: './favorite-tags.component.scss',
})
export class FavoriteTagsComponent {
  // 雙向繫結控制彈窗顯示
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  // 雙向繫結標籤陣列
  @Input() tags: string[] = [];
  @Output() tagsChange = new EventEmitter<string[]>();

  // 確認儲存事件
  @Output() onConfirm = new EventEmitter<void>();

  currentTag: string = '';

  addTag() {
    const tag = this.currentTag.trim();
    if (tag && !this.tags.includes(tag)) {
      this.tags = [...this.tags, tag];
      this.tagsChange.emit(this.tags);
      this.currentTag = '';
    }
  }

  removeTag(tagToRemove: string) {
    const newTags = this.tags.filter((t) => t !== tagToRemove);
    this.tagsChange.emit(newTags);
  }

  handleCancel() {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  handleConfirm() {
    this.onConfirm.emit();
  }
}

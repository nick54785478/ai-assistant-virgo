import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChipModule } from 'primeng/chip';
import { PRIME_COMPONENTS } from '../../../../../shared/shared-primeng';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-favorite-tag-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, ChipModule, PRIME_COMPONENTS],
  templateUrl: './favorite-tag-editor.component.html',
  styleUrl: './favorite-tag-editor.component.scss',
})
export class FavoriteTagEditorComponent {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  @Input() tags: string[] = [];
  @Output() tagsChange = new EventEmitter<string[]>();

  @Output() onSave = new EventEmitter<string[]>(); // 🚀 語意更明確的 Save 事件

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
    this.tags = newTags;
    this.tagsChange.emit(newTags);
  }

  handleCancel() {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  handleConfirm() {
    this.onSave.emit(this.tags); // 傳出最終修改後的標籤陣列
  }
}

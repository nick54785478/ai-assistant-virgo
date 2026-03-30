import { Component, EventEmitter, Input, Output } from '@angular/core';
import { environment } from '../../../../../environments/environment';
import { SystemMessageService } from '../../../../shared/service/system-message.service';
import { PRIME_COMPONENTS } from '../../../../shared/shared-primeng';
import { CommonModule } from '@angular/common';
import { FileUploadModule } from 'primeng/fileupload';
import { DialogModule } from 'primeng/dialog';
import { IngestionService } from '../../services/ingestion.service';

@Component({
  selector: 'app-knowledge-ingestion',
  standalone: true,
  imports: [CommonModule, PRIME_COMPONENTS, FileUploadModule, DialogModule],
  templateUrl: './knowledge-ingestion.component.html',
  styleUrl: './knowledge-ingestion.component.scss',
})
export class KnowledgeIngestionComponent {
  @Input() visible: boolean = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  isIngesting: boolean = false;

  constructor(
    public ingestionService: IngestionService,
    private sysMsgService: SystemMessageService,
  ) {}

  // 現在 HTML 可以直接透過 ingestionService.uploadUrl 取得路徑

  onUploadSuccess(event: any) {
    this.isIngesting = false;
    // 解析後端回傳的 DTO
    const res = event.originalEvent?.body;
    const msg = res?.message || '知識已成功注入 Virgo！';

    this.sysMsgService.showSuccess('注入成功', msg);
    this.close();
  }

  onUploadError(event: any) {
    this.isIngesting = false;
    this.sysMsgService.showError('注入失敗', '請檢查文件格式或伺服器連線。');
  }

  onBeforeSend() {
    this.isIngesting = true;
  }

  close() {
    this.visible = false;
    this.visibleChange.emit(false);
  }
}

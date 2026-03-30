import {
  Component,
  OnInit,
  ChangeDetectorRef,
  ViewChild,
  ElementRef,
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
// 1. 引入 PrimeNG Standalone 組件 (注意：不再是 Module)
import { Button, ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { ScrollPanel } from 'primeng/scrollpanel';
import { ProgressSpinner } from 'primeng/progressspinner';
import { PRIME_COMPONENTS } from '../../../shared/shared-primeng';
import { ChatMessage } from '../../chatbot/models/chat-message.model';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, PRIME_COMPONENTS],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {}

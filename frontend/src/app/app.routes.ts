import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/pages/home.component';
import { ChatbotComponent } from './features/chatbot/pages/chatbot.component';

/**
 * url 路徑 Component 導向配置
 */
export const routes: Routes = [
  {
    path: '',
    component: ChatbotComponent,
  },
];

import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/pages/home.component';
import { ChatbotComponent } from './features/chatbot/pages/chatbot.component';

export const routes: Routes = [
  {
    path: '',
    component: ChatbotComponent,
  },
];

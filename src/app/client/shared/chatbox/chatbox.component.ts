import { Component } from '@angular/core';
import { ChatboxService } from '../../service/chatbox.service';

@Component({
  selector: 'app-chatbox',
  templateUrl: './chatbox.component.html',
  styleUrl: './chatbox.component.scss'
})
export class ChatboxComponent {
  isChatboxVisible = false;
  userMessage: string = '';
  assistantMessage: string = '';
  constructor(private chatboxService: ChatboxService) { }


  sendMessage() {
    const message = this.userMessage;
    this.assistantMessage = 'Answering...';
    this.chatboxService.sendMessage(message).subscribe({
      next: (res) => {
        if (res.code = 1000) {
          this.assistantMessage = res.result;
        } else {
          this.assistantMessage = 'Something wrong, please ask again';
          console.error('Failed to fetch message:', res.message);
        }
      },
      error: (err) => {
        this.assistantMessage = 'Something wrong, please ask again';
        console.error(err);
      }
    })
  }

  toggleChatbox() {
    this.isChatboxVisible = !this.isChatboxVisible;
  }

  handleKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      if (event.shiftKey) {
        return;
      } else {
        this.sendMessage();
        event.preventDefault();
        this.userMessage = '';
      }
    }
  }
}

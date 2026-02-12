import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import SockJS from 'sockjs-client';
import { Client, Stomp } from '@stomp/stompjs';

export interface ChatMessage {
  type: 'JOIN' | 'LEAVE' | 'CHAT';
  content: string;
  sender: string;
  videoId: number;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private stompClient: Client | null = null;
  private messageSubject = new Subject<ChatMessage>();
  private connected = false;

  constructor() {}

  /**
   * Konekcija na WebSocket server
   */
  connect(videoId: number, username: string): Observable<ChatMessage> {
    if (this.connected) {
      console.warn('Already connected to WebSocket');
      return this.messageSubject.asObservable();
    }

    // Kreiranje STOMP klijenta sa SockJS
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws-chat'),
      debug: (str) => {
        // console.log(str); // Isključi debug logove (opciono)
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // Callback kada se konektuje
    this.stompClient.onConnect = () => {
      this.connected = true;
      console.log('WebSocket connected for video:', videoId);

      // Subscribe na topic za ovaj video
      this.stompClient?.subscribe(`/topic/chat/${videoId}`, (message) => {
        const chatMessage: ChatMessage = JSON.parse(message.body);
        this.messageSubject.next(chatMessage);
      });

      // Pošalji JOIN poruku
      this.sendJoinMessage(videoId, username);
    };

    // Callback za greške
    this.stompClient.onStompError = (frame) => {
      console.error('WebSocket error:', frame);
      this.connected = false;
    };

    // Aktiviraj konekciju
    this.stompClient.activate();

    return this.messageSubject.asObservable();
  }

  /**
   * Pošalji chat poruku
   */
  sendMessage(videoId: number, content: string, sender: string) {
    if (!this.connected || !this.stompClient) {
      console.error('Not connected to WebSocket');
      return;
    }

    const chatMessage: ChatMessage = {
      type: 'CHAT',
      content: content,
      sender: sender,
      videoId: videoId,
      timestamp: new Date().toISOString()
    };

    this.stompClient.publish({
      destination: `/app/chat/${videoId}/sendMessage`,
      body: JSON.stringify(chatMessage)
    });
  }

  /**
   * Pošalji JOIN poruku kada se korisnik pridruži
   */
  private sendJoinMessage(videoId: number, username: string) {
    if (!this.connected || !this.stompClient) {
      return;
    }

    const chatMessage: ChatMessage = {
      type: 'JOIN',
      content: '',
      sender: username,
      videoId: videoId,
      timestamp: new Date().toISOString()
    };

    this.stompClient.publish({
      destination: `/app/chat/${videoId}/addUser`,
      body: JSON.stringify(chatMessage)
    });
  }

  /**
   * Diskonektuj se sa WebSocket-a
   */
  disconnect() {
    if (this.stompClient && this.connected) {
      this.stompClient.deactivate();
      console.log('WebSocket disconnected');
      this.connected = false;
    }
  }

  /**
   * Da li je konekcija aktivna
   */
  isConnected(): boolean {
    return this.connected;
  }
}
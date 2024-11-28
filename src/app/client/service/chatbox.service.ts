import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { enviroment } from '../../enviroment/enviroment';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../dto/ApiResponse';

@Injectable({
  providedIn: 'root'
})
export class ChatboxService {
  constructor(private http: HttpClient) { }
  private url = enviroment.apiUrl;


  sendMessage(message: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.url}/chat`, { "message": message });
  }

}

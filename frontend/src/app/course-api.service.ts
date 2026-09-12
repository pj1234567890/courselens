import { Injectable } from '@angular/core';
export interface DocumentView { id:string; filename:string; type:string; status:string; pageCount:number; createdAt:string; error?:string; }
export interface Source { documentId:string; documentName:string; pageNumber:number; chunkId:string; evidence:string; handwritten:boolean; sourceType:string; }
export interface AskResponse { status:'ANSWERED'|'NOT_COVERED'; answer:string; sources:Source[]; conversationId:string; }
export interface ChatMessage { id:string; role:'USER'|'ASSISTANT'; content:string; createdAt:string; }
@Injectable({providedIn:'root'}) export class CourseApiService {
 private async request<T>(url:string, init?:RequestInit):Promise<T>{const response=await fetch(url,init);const data=await response.json().catch(()=>({}));if(!response.ok)throw new Error(data.error||'Request failed. Please try again.');return data as T;}
 documents(){return this.request<DocumentView[]>('/api/documents');} messages(id:string){return this.request<ChatMessage[]>(`/api/conversations/${id}/messages`);}
 ask(question:string,conversationId?:string){return this.request<AskResponse>('/api/questions',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({question,conversationId})});}
 upload(file:File){const form=new FormData();form.append('file',file);return this.request<DocumentView>('/api/documents',{method:'POST',body:form});} sourceUrl(source:Source){return `/api/documents/${source.documentId}/pages/${source.pageNumber}/source`;}
}

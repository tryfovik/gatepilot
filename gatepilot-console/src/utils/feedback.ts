import { reactive } from 'vue';

export type ToastTone = 'success' | 'warning' | 'danger' | 'info';

export interface ToastMessage {
  id: number;
  tone: ToastTone;
  title: string;
  message?: string;
}

export const toastMessages = reactive<ToastMessage[]>([]);

let toastId = 1;
let lastToastKey = '';
let lastToastAt = 0;

export function notifySuccess(title: string, message?: string) {
  pushToast('success', title, message);
}

export function notifyInfo(title: string, message?: string) {
  // 轻量交互不弹 toast，避免展开、切换、导航时打扰用户
  void title;
  void message;
}

export function notifyWarning(title: string, message?: string) {
  pushToast('warning', title, message);
}

export function notifyError(title: string, message?: string) {
  pushToast('danger', title, message);
}

export function dismissToast(id: number) {
  const index = toastMessages.findIndex((item) => item.id === id);
  if (index >= 0) {
    toastMessages.splice(index, 1);
  }
}

function pushToast(tone: ToastTone, title: string, message?: string) {
  const key = [tone, title, message || ''].join('|');
  const now = Date.now();
  if (key === lastToastKey && now - lastToastAt < 1500) {
    return;
  }
  lastToastKey = key;
  lastToastAt = now;
  const id = toastId;
  toastId += 1;
  if (toastMessages.length >= 3) {
    toastMessages.splice(0, toastMessages.length - 2);
  }
  toastMessages.push({ id, tone, title, message });
  window.setTimeout(() => dismissToast(id), tone === 'danger' ? 4200 : 2600);
}

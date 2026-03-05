import api from './client';
import type {
  ServiceFeedbackRequest,
  FeedbackResponse,
  UnreadFeedbackCountResponse,
} from '../types/api';

export const submitServiceFeedback = (payload: ServiceFeedbackRequest) => {
  return api.post<FeedbackResponse>('/feedback/service', payload);
};

export const fetchMyFeedback = () => {
  return api.get<FeedbackResponse[]>('/feedback/me');
};

export const fetchUnreadFeedbackCount = () => {
  return api.get<UnreadFeedbackCountResponse>('/feedback/me/unread-count');
};

export const markFeedbackRepliesAsRead = () => {
  return api.post<void>('/feedback/me/mark-read');
};

import api from './client';
import type { PriceImportResponse, FeedbackResponse, AdminReplyRequest } from '../types/api';

export const exportPrices = (pizzeriaCode: string) => {
  return api.get<string>(`/admin/pizzerias/${pizzeriaCode}/prices/export`, {
    responseType: 'blob',
  });
};

export const importPrices = (
  pizzeriaCode: string,
  file: File,
  dryRun: boolean = false
) => {
  const formData = new FormData();
  formData.append('file', file);

  // Explicitly remove Content-Type so Axios sets it automatically with boundary
  // The default 'application/json' from the client config must be removed
  return api.post<PriceImportResponse>(
    `/admin/pizzerias/${pizzeriaCode}/prices/import?dryRun=${dryRun}`,
    formData,
    {
      headers: {
        'Content-Type': undefined,
      },
    }
  );
};

export const fetchAdminFeedback = (pizzeriaCode: string) => {
  return api.get<FeedbackResponse[]>(
    `/admin/pizzerias/${pizzeriaCode}/feedback`
  );
};

export const replyToFeedback = (
  pizzeriaCode: string,
  feedbackId: string,
  request: AdminReplyRequest
) => {
  return api.post<FeedbackResponse>(
    `/admin/pizzerias/${pizzeriaCode}/feedback/${feedbackId}/reply`,
    request
  );
};

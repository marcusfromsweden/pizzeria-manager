import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { fetchAdminFeedback, replyToFeedback } from '../../api/admin';
import { Card } from '../../components/ui/Card';
import { Alert } from '../../components/ui/Alert';
import { Spinner } from '../../components/ui/Spinner';
import { Button } from '../../components/ui/Button';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { useAuth } from '../../hooks/useAuth';
import type { FeedbackResponse } from '../../types/api';

export const AdminFeedbackPage = () => {
  const { t } = useTranslation('common');
  const pizzeriaCode = usePizzeriaCode();
  const { profile } = useAuth();
  const queryClient = useQueryClient();

  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [replyText, setReplyText] = useState<string>('');
  const [replyError, setReplyError] = useState<string | null>(null);

  const isAdmin = profile?.pizzeriaAdmin === pizzeriaCode;

  const {
    data: feedbackList,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['admin-feedback', pizzeriaCode],
    queryFn: () => fetchAdminFeedback(pizzeriaCode).then((res) => res.data),
    enabled: isAdmin,
    staleTime: 0, // Always consider data stale
    refetchOnMount: 'always', // Always refetch when component mounts
  });

  const replyMutation = useMutation({
    mutationFn: ({
      feedbackId,
      reply,
    }: {
      feedbackId: string;
      reply: string;
    }) => replyToFeedback(pizzeriaCode, feedbackId, { reply }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-feedback', pizzeriaCode] });
      setExpandedId(null);
      setReplyText('');
      setReplyError(null);
    },
    onError: () => {
      setReplyError(t('admin.feedback.replyError', 'Failed to send reply. Please try again.'));
    },
  });

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleToggleExpand = (id: string) => {
    if (expandedId === id) {
      setExpandedId(null);
      setReplyText('');
      setReplyError(null);
    } else {
      setExpandedId(id);
      setReplyText('');
      setReplyError(null);
    }
  };

  const handleSubmitReply = (feedbackId: string) => {
    if (!replyText.trim()) {
      setReplyError(t('admin.feedback.replyRequired', 'Please enter a reply message.'));
      return;
    }
    replyMutation.mutate({ feedbackId, reply: replyText.trim() });
  };

  if (!isAdmin) {
    return (
      <div className="mx-auto max-w-2xl">
        <Alert variant="error">
          {t(
            'admin.feedback.noAccess',
            'You do not have admin access to view customer feedback.'
          )}
        </Alert>
      </div>
    );
  }

  const renderFeedbackRow = (item: FeedbackResponse) => {
    const isExpanded = expandedId === item.id;
    const hasReply = !!item.adminReply;

    return (
      <tbody key={item.id} className="divide-y divide-slate-200">
        <tr
          className={`cursor-pointer hover:bg-slate-50 ${isExpanded ? 'bg-slate-50' : ''}`}
          onClick={() => handleToggleExpand(item.id)}
        >
          <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600">
            {formatDate(item.createdAt)}
          </td>
          <td className="px-4 py-3 text-sm text-slate-900">
            <div className="max-w-md truncate">{item.message}</div>
          </td>
          <td className="px-4 py-3 text-sm">
            {item.category ? (
              <span className="rounded bg-slate-100 px-2 py-1 text-slate-600">
                {item.category}
              </span>
            ) : (
              <span className="text-slate-400">-</span>
            )}
          </td>
          <td className="whitespace-nowrap px-4 py-3 text-center">
            {item.rating ? (
              <div className="flex items-center justify-center">
                {[1, 2, 3, 4, 5].map((num) => (
                  <span
                    key={num}
                    className={`text-sm ${
                      num <= item.rating! ? 'text-yellow-500' : 'text-slate-300'
                    }`}
                  >
                    ★
                  </span>
                ))}
              </div>
            ) : (
              <span className="text-slate-400">-</span>
            )}
          </td>
          <td className="whitespace-nowrap px-4 py-3 text-center">
            {hasReply ? (
              <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
                {t('admin.feedback.statusReplied', 'Replied')}
              </span>
            ) : (
              <span className="inline-flex items-center rounded-full bg-yellow-100 px-2.5 py-0.5 text-xs font-medium text-yellow-800">
                {t('admin.feedback.statusOpen', 'Open')}
              </span>
            )}
          </td>
          <td className="whitespace-nowrap px-4 py-3 text-center">
            <span className={`text-sm ${isExpanded ? 'rotate-180' : ''} inline-block transition-transform`}>
              ▼
            </span>
          </td>
        </tr>
        {isExpanded && (
          <tr>
            <td colSpan={6} className="bg-slate-50 px-4 py-4">
              <div className="space-y-4">
                {/* Full message */}
                <div>
                  <h4 className="text-sm font-medium text-slate-700">
                    {t('admin.feedback.fullMessage', 'Full message')}
                  </h4>
                  <p className="mt-1 whitespace-pre-wrap text-sm text-slate-900">
                    {item.message}
                  </p>
                </div>

                {/* Existing reply */}
                {hasReply && (
                  <div className="rounded-lg border border-green-200 bg-green-50 p-3">
                    <h4 className="text-sm font-medium text-green-800">
                      {t('admin.feedback.yourReply', 'Your reply')}
                      <span className="ml-2 font-normal text-green-600">
                        ({formatDate(item.adminRepliedAt!)})
                      </span>
                    </h4>
                    <p className="mt-1 whitespace-pre-wrap text-sm text-green-900">
                      {item.adminReply}
                    </p>
                  </div>
                )}

                {/* Reply form (only show if no reply yet) */}
                {!hasReply && (
                  <div className="space-y-3">
                    <h4 className="text-sm font-medium text-slate-700">
                      {t('admin.feedback.writeReply', 'Write a reply')}
                    </h4>
                    <textarea
                      value={replyText}
                      onChange={(e) => {
                        setReplyText(e.target.value);
                        setReplyError(null);
                      }}
                      onClick={(e) => e.stopPropagation()}
                      placeholder={t(
                        'admin.feedback.replyPlaceholder',
                        'Enter your reply to the customer...'
                      )}
                      className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                      rows={3}
                      maxLength={2000}
                    />
                    {replyError && (
                      <p className="text-sm text-red-600">{replyError}</p>
                    )}
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-slate-500">
                        {replyText.length}/2000
                      </span>
                      <div className="flex gap-2">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleToggleExpand(item.id);
                          }}
                        >
                          {t('admin.feedback.cancel', 'Cancel')}
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleSubmitReply(item.id);
                          }}
                          disabled={replyMutation.isPending}
                        >
                          {replyMutation.isPending ? (
                            <Spinner size="sm" />
                          ) : (
                            t('admin.feedback.sendReply', 'Send reply')
                          )}
                        </Button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </td>
          </tr>
        )}
      </tbody>
    );
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Card padding="lg">
        <h1 className="text-2xl font-bold text-slate-900">
          {t('admin.feedback.title', 'Customer feedback')}
        </h1>
        <p className="mt-2 text-slate-600">
          {t(
            'admin.feedback.subtitle',
            'View and respond to feedback submitted by your customers.'
          )}
        </p>
      </Card>

      {error && (
        <Alert variant="error">
          {t('admin.feedback.error', 'Failed to load feedback.')}
        </Alert>
      )}

      <Card padding="lg">
        {isLoading ? (
          <div className="flex justify-center py-8">
            <Spinner size="md" />
          </div>
        ) : feedbackList && feedbackList.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">
                    {t('admin.feedback.date', 'Date')}
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">
                    {t('admin.feedback.message', 'Message')}
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">
                    {t('admin.feedback.category', 'Category')}
                  </th>
                  <th className="px-4 py-3 text-center text-xs font-medium uppercase text-slate-500">
                    {t('admin.feedback.rating', 'Rating')}
                  </th>
                  <th className="px-4 py-3 text-center text-xs font-medium uppercase text-slate-500">
                    {t('admin.feedback.status', 'Status')}
                  </th>
                  <th className="px-4 py-3 text-center text-xs font-medium uppercase text-slate-500">
                    <span className="sr-only">{t('admin.feedback.expand', 'Expand')}</span>
                  </th>
                </tr>
              </thead>
              {feedbackList.map(renderFeedbackRow)}
            </table>
          </div>
        ) : (
          <p className="py-8 text-center text-sm text-slate-500">
            {t('admin.feedback.noFeedback', 'No customer feedback yet.')}
          </p>
        )}
      </Card>
    </div>
  );
};

export default AdminFeedbackPage;

import { useState, useEffect, useRef, type FormEvent } from 'react';
import { useLocation } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  submitServiceFeedback,
  fetchMyFeedback,
  markFeedbackRepliesAsRead,
} from '../../api/feedback';
import { getApiErrorMessage } from '../../api/client';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Alert } from '../../components/ui/Alert';
import { Spinner } from '../../components/ui/Spinner';
import { Badge } from '../../components/ui/Badge';

export const FeedbackPage = () => {
  const { t } = useTranslation('menu');
  const { t: tCommon } = useTranslation('common');
  const queryClient = useQueryClient();
  const location = useLocation();
  const hasMarkedAsRead = useRef(false);

  const [message, setMessage] = useState('');
  const [rating, setRating] = useState<number | null>(null);
  const [category, setCategory] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [unreadIds, setUnreadIds] = useState<Set<string>>(new Set());

  const { data: feedbackList, isLoading: isFeedbackLoading } = useQuery({
    queryKey: ['my-feedback'],
    queryFn: () => fetchMyFeedback().then((res) => res.data),
    staleTime: 0, // Always consider data stale
    refetchOnMount: 'always', // Always refetch when component mounts
  });

  // Reset state when navigating to this page (location.key changes on each navigation)
  useEffect(() => {
    hasMarkedAsRead.current = false;
    setUnreadIds(new Set());
  }, [location.key]);

  // Mark unread replies as read when page loads
  useEffect(() => {
    if (!feedbackList || hasMarkedAsRead.current) return;

    // Find items with unread replies (has adminReply but no adminReplyReadAt)
    const unreadItems = feedbackList.filter(
      (item) => item.adminReply && !item.adminReplyReadAt
    );

    if (unreadItems.length > 0) {
      // Store unread IDs for highlighting
      setUnreadIds(new Set(unreadItems.map((item) => item.id)));

      // Mark as read
      hasMarkedAsRead.current = true;
      markFeedbackRepliesAsRead()
        .then(() => {
          // Invalidate the unread count query
          void queryClient.invalidateQueries({
            queryKey: ['unread-feedback-count'],
          });
        })
        .catch(() => {
          // Silently fail - not critical
        });
    }
  }, [feedbackList, queryClient]);

  const feedbackMutation = useMutation({
    mutationFn: () =>
      submitServiceFeedback({
        message,
        rating,
        category: category || null,
      }),
    onSuccess: () => {
      setMessage('');
      setRating(null);
      setCategory('');
      setSuccess(true);
      setError(null);
      void queryClient.invalidateQueries({ queryKey: ['my-feedback'] });
    },
    onError: (err) => {
      setError(getApiErrorMessage(err));
      setSuccess(false);
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    feedbackMutation.mutate();
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <Card padding="lg">
        <h1 className="text-2xl font-bold text-slate-900">
          {t('feedback.title')}
        </h1>
        <p className="mt-2 text-slate-600">{t('feedback.subtitle')}</p>

        {error && (
          <Alert variant="error" className="mt-4" onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert
            variant="success"
            className="mt-4"
            onClose={() => setSuccess(false)}
          >
            {t('feedback.success')}
          </Alert>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">
              {t('feedback.message')}
            </label>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder={t('feedback.messagePlaceholder')}
              required
              rows={5}
              className="block w-full rounded-md border border-slate-300 px-3 py-2 text-sm placeholder-slate-400 shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">
              {t('feedback.rating')}
            </label>
            <div className="flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((num) => (
                <button
                  key={num}
                  type="button"
                  onClick={() => setRating(rating === num ? null : num)}
                  className={`text-2xl transition-colors ${
                    rating !== null && rating >= num
                      ? 'text-yellow-500'
                      : 'text-slate-300 hover:text-yellow-400'
                  }`}
                >
                  ★
                </button>
              ))}
              {rating && (
                <span className="ml-2 text-sm text-slate-600">{rating}/5</span>
              )}
            </div>
          </div>

          <Input
            label={t('feedback.category')}
            type="text"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder={t('feedback.categoryPlaceholder')}
          />

          <Button
            type="submit"
            disabled={!message || feedbackMutation.isPending}
            isLoading={feedbackMutation.isPending}
          >
            {feedbackMutation.isPending
              ? t('feedback.submitting')
              : t('feedback.submit')}
          </Button>
        </form>
      </Card>

      {/* My Feedback History */}
      <Card padding="lg">
        <h2 className="text-lg font-semibold text-slate-900">
          {t('feedback.history')}
        </h2>

        {isFeedbackLoading ? (
          <div className="flex justify-center py-8">
            <Spinner size="md" />
          </div>
        ) : feedbackList && feedbackList.length > 0 ? (
          <ul className="mt-4 divide-y divide-slate-200">
            {feedbackList.map((item) => (
              <li key={item.id} className="py-4">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <p className="text-sm text-slate-900">{item.message}</p>
                    <div className="mt-1 flex items-center gap-3 text-xs text-slate-500">
                      <span>{formatDate(item.createdAt)}</span>
                      {item.category && (
                        <span className="rounded bg-slate-100 px-2 py-0.5 text-slate-600">
                          {item.category}
                        </span>
                      )}
                    </div>
                  </div>
                  {item.rating && (
                    <div className="ml-4 flex items-center">
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
                  )}
                </div>
                {/* Admin reply */}
                {item.adminReply && (
                  <div
                    className={`mt-3 rounded-lg border p-3 ${
                      unreadIds.has(item.id)
                        ? 'border-blue-300 bg-blue-50 ring-2 ring-blue-200'
                        : 'border-green-200 bg-green-50'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <span
                        className={`text-xs font-medium ${
                          unreadIds.has(item.id) ? 'text-blue-800' : 'text-green-800'
                        }`}
                      >
                        {t('feedback.adminReply')}
                      </span>
                      {unreadIds.has(item.id) && (
                        <Badge variant="info" size="sm">
                          {t('feedback.new')}
                        </Badge>
                      )}
                      <span
                        className={`text-xs ${
                          unreadIds.has(item.id) ? 'text-blue-600' : 'text-green-600'
                        }`}
                      >
                        {formatDate(item.adminRepliedAt!)}
                      </span>
                    </div>
                    <p
                      className={`mt-1 whitespace-pre-wrap text-sm ${
                        unreadIds.has(item.id) ? 'text-blue-900' : 'text-green-900'
                      }`}
                    >
                      {item.adminReply}
                    </p>
                  </div>
                )}
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-4 text-sm text-slate-500">{t('feedback.noFeedback')}</p>
        )}
      </Card>
    </div>
  );
};

export default FeedbackPage;

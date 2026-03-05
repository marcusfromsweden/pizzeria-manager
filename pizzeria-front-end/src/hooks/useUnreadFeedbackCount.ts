import { useQuery } from '@tanstack/react-query';
import { fetchUnreadFeedbackCount } from '../api/feedback';
import { useAuth } from './useAuth';

export const useUnreadFeedbackCount = () => {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: ['unread-feedback-count'],
    queryFn: () => fetchUnreadFeedbackCount().then((res) => res.data),
    enabled: isAuthenticated,
    refetchInterval: 30000, // Poll every 30 seconds
    staleTime: 0, // Always consider data stale
    refetchOnWindowFocus: true, // Refetch when window regains focus
  });
};

export default useUnreadFeedbackCount;

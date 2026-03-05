import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { resetPassword } from '../../api/auth';
import { getApiErrorMessage } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Alert } from '../../components/ui/Alert';
import { Card } from '../../components/ui/Card';

export const ResetPasswordPage = () => {
  const { t } = useTranslation('auth');
  const pizzeriaCode = usePizzeriaCode();
  const [searchParams] = useSearchParams();

  const [token, setToken] = useState(searchParams.get('token') ?? '');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (newPassword !== confirmPassword) {
      setError(t('resetPassword.error.passwordMismatch'));
      return;
    }

    setIsSubmitting(true);

    try {
      await resetPassword(pizzeriaCode, { token, newPassword });
      setIsSuccess(true);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSuccess) {
    return (
      <div className="mx-auto max-w-md">
        <Card padding="lg">
          <Alert variant="success" className="mb-4">
            <h3 className="font-medium">{t('resetPassword.success.title')}</h3>
            <p className="mt-1">{t('resetPassword.success.message')}</p>
          </Alert>

          <Link to={`/${pizzeriaCode}/login`}>
            <Button variant="primary" className="w-full">
              {t('login.title')}
            </Button>
          </Link>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md">
      <Card padding="lg">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-slate-900">
            {t('resetPassword.title')}
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            {t('resetPassword.subtitle')}
          </p>
        </div>

        {error && (
          <Alert variant="error" className="mb-4" onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4">
          <Input
            label={t('resetPassword.token')}
            type="text"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder={t('resetPassword.tokenPlaceholder')}
            required
          />

          <Input
            label={t('resetPassword.newPassword')}
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder={t('resetPassword.newPasswordPlaceholder')}
            required
            minLength={8}
            maxLength={100}
            autoComplete="new-password"
          />

          <Input
            label={t('resetPassword.confirmPassword')}
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder={t('resetPassword.confirmPasswordPlaceholder')}
            required
            minLength={8}
            maxLength={100}
            autoComplete="new-password"
          />

          <Button
            type="submit"
            className="w-full"
            isLoading={isSubmitting}
            disabled={isSubmitting}
          >
            {isSubmitting
              ? t('resetPassword.submitting')
              : t('resetPassword.submit')}
          </Button>
        </form>

        <div className="mt-6 text-center text-sm text-slate-600">
          <Link
            to={`/${pizzeriaCode}/login`}
            className="font-medium text-primary-600 hover:text-primary-500"
          >
            {t('login.title')}
          </Link>
        </div>
      </Card>
    </div>
  );
};

export default ResetPasswordPage;

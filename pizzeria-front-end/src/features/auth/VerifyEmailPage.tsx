import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { verifyEmail } from '../../api/auth';
import { getApiErrorMessage } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Alert } from '../../components/ui/Alert';
import { Card } from '../../components/ui/Card';

export const VerifyEmailPage = () => {
  const { t } = useTranslation('auth');
  const pizzeriaCode = usePizzeriaCode();
  const [searchParams] = useSearchParams();

  const [token, setToken] = useState(searchParams.get('token') ?? '');
  const [error, setError] = useState<string | null>(null);
  const [isVerified, setIsVerified] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await verifyEmail(pizzeriaCode, { token });
      setIsVerified(true);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isVerified) {
    return (
      <div className="mx-auto max-w-md">
        <Card padding="lg">
          <Alert variant="success" className="mb-4">
            <h3 className="font-medium">{t('verifyEmail.success.title')}</h3>
            <p className="mt-1">{t('verifyEmail.success.message')}</p>
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
            {t('verifyEmail.title')}
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            {t('verifyEmail.subtitle')}
          </p>
        </div>

        {error && (
          <Alert variant="error" className="mb-4" onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4">
          <Input
            label={t('verifyEmail.token')}
            type="text"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder={t('verifyEmail.tokenPlaceholder')}
            required
          />

          <Button
            type="submit"
            className="w-full"
            isLoading={isSubmitting}
            disabled={isSubmitting}
          >
            {isSubmitting ? t('verifyEmail.submitting') : t('verifyEmail.submit')}
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

export default VerifyEmailPage;

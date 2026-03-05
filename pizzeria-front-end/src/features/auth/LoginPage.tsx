import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../hooks/useAuth';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { getApiErrorMessage } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Alert } from '../../components/ui/Alert';
import { Card } from '../../components/ui/Card';

export const LoginPage = () => {
  const { t } = useTranslation('auth');
  const { login } = useAuth();
  const pizzeriaCode = usePizzeriaCode();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const from = (location.state as { from?: { pathname: string } })?.from
    ?.pathname;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await login(pizzeriaCode, { email, password });
      navigate(from ?? `/${pizzeriaCode}`, { replace: true });
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-md">
      <Card padding="lg">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-slate-900">
            {t('login.title')}
          </h1>
          <p className="mt-2 text-sm text-slate-600">{t('login.subtitle')}</p>
        </div>

        {error && (
          <Alert variant="error" className="mb-4" onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4">
          <Input
            label={t('login.email')}
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={t('login.emailPlaceholder')}
            required
            autoComplete="email"
          />

          <Input
            label={t('login.password')}
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder={t('login.passwordPlaceholder')}
            required
            autoComplete="current-password"
          />

          <div className="text-right">
            <Link
              to={`/${pizzeriaCode}/forgot-password`}
              className="text-sm font-medium text-primary-600 hover:text-primary-500"
            >
              {t('login.forgotPassword')}
            </Link>
          </div>

          <Button
            type="submit"
            className="w-full"
            isLoading={isSubmitting}
            disabled={isSubmitting}
          >
            {isSubmitting ? t('login.submitting') : t('login.submit')}
          </Button>
        </form>

        <div className="mt-6 text-center text-sm text-slate-600">
          {t('login.noAccount')}{' '}
          <Link
            to={`/${pizzeriaCode}/register`}
            className="font-medium text-primary-600 hover:text-primary-500"
          >
            {t('login.registerLink')}
          </Link>
        </div>
      </Card>
    </div>
  );
};

export default LoginPage;

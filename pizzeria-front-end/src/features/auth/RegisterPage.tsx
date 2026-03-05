import { useState, type FormEvent, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../hooks/useAuth';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { getApiErrorMessage } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Alert } from '../../components/ui/Alert';
import { Card } from '../../components/ui/Card';

export const RegisterPage = () => {
  const { t } = useTranslation('auth');
  const { register } = useAuth();
  const pizzeriaCode = usePizzeriaCode();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [verificationToken, setVerificationToken] = useState<string | null>(
    null
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [copied, setCopied] = useState(false);

  const curlCommand = verificationToken
    ? `curl -X POST http://localhost:9900/api/v1/pizzerias/${pizzeriaCode}/users/verify-email \\
  -H "Content-Type: application/json" \\
  -d '{"token":"${verificationToken}"}'`
    : '';

  const handleCopy = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(curlCommand);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  }, [curlCommand]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const result = await register(pizzeriaCode, { name, email, password });
      setVerificationToken(result.verificationToken);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (verificationToken) {
    return (
      <div className="mx-auto max-w-md">
        <Card padding="lg">
          <Alert variant="success" className="mb-4">
            <h3 className="font-medium">{t('register.success.title')}</h3>
            <p className="mt-1">{t('register.success.message')}</p>
          </Alert>

          <div className="mt-4 rounded-lg bg-slate-100 p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm text-slate-600">
                {t('register.success.curlCommand')}
              </p>
              <button
                type="button"
                onClick={() => void handleCopy()}
                className="flex items-center gap-1 rounded px-2 py-1 text-xs text-slate-500 hover:bg-slate-200 hover:text-slate-700"
                title={copied ? 'Copied!' : 'Copy to clipboard'}
              >
                {copied ? (
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-4 w-4 text-green-600"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path
                      fillRule="evenodd"
                      d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                      clipRule="evenodd"
                    />
                  </svg>
                ) : (
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-4 w-4"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                    <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                  </svg>
                )}
                <span>{copied ? 'Copied!' : 'Copy'}</span>
              </button>
            </div>
            <pre className="mt-2 overflow-x-auto text-xs text-slate-700">
              <code>{curlCommand}</code>
            </pre>
          </div>

          <div className="mt-6 flex flex-col gap-3">
            <Link to={`/${pizzeriaCode}/verify-email`}>
              <Button variant="primary" className="w-full">
                {t('verifyEmail.title')}
              </Button>
            </Link>
            <Link to={`/${pizzeriaCode}/login`}>
              <Button variant="secondary" className="w-full">
                {t('login.title')}
              </Button>
            </Link>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md">
      <Card padding="lg">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-slate-900">
            {t('register.title')}
          </h1>
          <p className="mt-2 text-sm text-slate-600">{t('register.subtitle')}</p>
        </div>

        {error && (
          <Alert variant="error" className="mb-4" onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4">
          <Input
            label={t('register.name')}
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={t('register.namePlaceholder')}
            required
            maxLength={100}
            autoComplete="name"
          />

          <Input
            label={t('register.email')}
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={t('register.emailPlaceholder')}
            required
            autoComplete="email"
          />

          <Input
            label={t('register.password')}
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder={t('register.passwordPlaceholder')}
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
            {isSubmitting ? t('register.submitting') : t('register.submit')}
          </Button>
        </form>

        <div className="mt-6 text-center text-sm text-slate-600">
          {t('register.hasAccount')}{' '}
          <Link
            to={`/${pizzeriaCode}/login`}
            className="font-medium text-primary-600 hover:text-primary-500"
          >
            {t('register.loginLink')}
          </Link>
        </div>
      </Card>
    </div>
  );
};

export default RegisterPage;

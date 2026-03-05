import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Button } from '../../components/ui/Button';

export const NotFoundPage = () => {
  const { t } = useTranslation('common');
  const location = useLocation();

  // Try to extract pizzeriaCode from current path
  const pathParts = location.pathname.split('/').filter(Boolean);
  const pizzeriaCode = pathParts[0] || 'kingspizza';

  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center text-center">
      <span className="text-6xl">🍕</span>
      <h1 className="mt-4 text-4xl font-bold text-slate-900">404</h1>
      <p className="mt-2 text-xl text-slate-600">{t('status.notFound')}</p>
      <p className="mt-1 text-sm text-slate-500">
        {t('error.notFound.message')}
      </p>
      <div className="mt-6 flex gap-4">
        <Link to={`/${pizzeriaCode}`}>
          <Button>{t('nav.home')}</Button>
        </Link>
        <Link to={`/${pizzeriaCode}/menu`}>
          <Button variant="secondary">{t('nav.menu')}</Button>
        </Link>
      </div>
    </div>
  );
};

export default NotFoundPage;

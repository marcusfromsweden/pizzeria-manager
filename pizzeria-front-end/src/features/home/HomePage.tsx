import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { usePizzeriaContext } from '../../routes/PizzeriaProvider';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';

export const HomePage = () => {
  const { t } = useTranslation('common');
  const { t: tMenu } = useTranslation('menu');
  const { pizzeriaCode, pizzeriaName } = usePizzeriaContext();
  const { isAuthenticated } = useAuth();

  return (
    <div className="space-y-12">
      {/* Hero Section */}
      <div className="text-center">
        <span className="text-6xl">🍕</span>
        <h1 className="mt-4 text-4xl font-bold text-slate-900">
          {pizzeriaName ?? pizzeriaCode}
        </h1>
        <p className="mt-2 text-xl text-slate-600">{tMenu('subtitle')}</p>
        <div className="mt-6 flex justify-center gap-4">
          <Link to={`/${pizzeriaCode}/menu`}>
            <Button size="lg">{t('nav.menu')}</Button>
          </Link>
          <Link to={`/${pizzeriaCode}/pizzas`}>
            <Button size="lg">{t('nav.pizzas')}</Button>
          </Link>
        </div>
      </div>

      {/* Features */}
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        <Link to={`/${pizzeriaCode}/menu`} className="block">
          <Card padding="lg" className="h-full cursor-pointer transition-shadow hover:shadow-md">
            <div className="text-center">
              <span className="text-4xl">📋</span>
              <h3 className="mt-3 text-lg font-semibold text-slate-900">
                {t('home.features.menu.title')}
              </h3>
              <p className="mt-2 text-sm text-slate-600">
                {t('home.features.menu.description')}
              </p>
              <span className="mt-4 inline-block text-sm font-medium text-primary-600">
                {t('home.features.menu.link')} →
              </span>
            </div>
          </Card>
        </Link>

        <Link
          to={isAuthenticated ? `/${pizzeriaCode}/preferences` : `/${pizzeriaCode}/login`}
          className="block"
        >
          <Card padding="lg" className="h-full cursor-pointer transition-shadow hover:shadow-md">
            <div className="text-center">
              <span className="text-4xl">🥗</span>
              <h3 className="mt-3 text-lg font-semibold text-slate-900">
                {t('home.features.dietary.title')}
              </h3>
              <p className="mt-2 text-sm text-slate-600">
                {t('home.features.dietary.description')}
              </p>
              <span className="mt-4 inline-block text-sm font-medium text-primary-600">
                {isAuthenticated ? t('home.features.dietary.link') : t('home.features.signInLink')} →
              </span>
            </div>
          </Card>
        </Link>

        <Link
          to={isAuthenticated ? `/${pizzeriaCode}/scores` : `/${pizzeriaCode}/login`}
          className="block"
        >
          <Card padding="lg" className="h-full cursor-pointer transition-shadow hover:shadow-md">
            <div className="text-center">
              <span className="text-4xl">⭐</span>
              <h3 className="mt-3 text-lg font-semibold text-slate-900">
                {t('home.features.scores.title')}
              </h3>
              <p className="mt-2 text-sm text-slate-600">
                {t('home.features.scores.description')}
              </p>
              <span className="mt-4 inline-block text-sm font-medium text-primary-600">
                {isAuthenticated ? t('home.features.scores.link') : t('home.features.signInLink')} →
              </span>
            </div>
          </Card>
        </Link>
      </div>

      {/* CTA for non-authenticated users */}
      {!isAuthenticated && (
        <Card padding="lg" className="bg-primary-50 border-primary-200">
          <div className="text-center">
            <h2 className="text-xl font-semibold text-slate-900">
              {t('home.cta.title')}
            </h2>
            <p className="mt-2 text-slate-600">
              {t('home.cta.description')}
            </p>
            <div className="mt-4 flex justify-center gap-4">
              <Link to={`/${pizzeriaCode}/register`}>
                <Button>{t('nav.register')}</Button>
              </Link>
              <Link to={`/${pizzeriaCode}/login`}>
                <Button variant="secondary">{t('nav.signIn')}</Button>
              </Link>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
};

export default HomePage;

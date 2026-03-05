import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { useTranslateKey } from '../../hooks/useTranslateKey';
import { fetchPizzas } from '../../api/pizzas';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Spinner } from '../../components/ui/Spinner';
import { Alert } from '../../components/ui/Alert';
import { getDietaryBadgeVariant } from '../../utils/dietaryUtils';

export const PizzaListPage = () => {
  const { t } = useTranslation('menu');
  const { t: tCommon } = useTranslation('common');
  const { translateKey } = useTranslateKey();
  const pizzeriaCode = usePizzeriaCode();

  const { data: pizzas, isLoading, error } = useQuery({
    queryKey: ['pizzas', pizzeriaCode],
    queryFn: () => fetchPizzas(pizzeriaCode).then((res) => res.data),
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="error">
        {tCommon('status.error')}
      </Alert>
    );
  }

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-slate-900">
          {tCommon('nav.pizzas')}
        </h1>
        <p className="mt-2 text-slate-600">
          {t('subtitle')}
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {pizzas?.map((pizza) => (
          <Link
            key={pizza.id}
            to={`/${pizzeriaCode}/pizzas/${pizza.id}`}
            className="group"
          >
            <Card className="h-full transition-shadow group-hover:shadow-md">
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-sm text-slate-500">
                    #{pizza.dishNumber}
                  </span>
                  <h3 className="text-lg font-semibold text-slate-900 group-hover:text-primary-600">
                    {translateKey(pizza.nameKey)}
                  </h3>
                  <Badge
                    variant={getDietaryBadgeVariant(pizza.overallDietaryType)}
                    size="sm"
                  >
                    {t(`item.dietary.${pizza.overallDietaryType}`)}
                  </Badge>
                </div>
              </div>

              <div className="mt-4 flex items-end justify-between">
                <div>
                  <div className="text-lg font-bold text-primary-600">
                    {pizza.priceInSek} {tCommon('currency.sek')}
                  </div>
                  {pizza.familySizePriceInSek && (
                    <div className="text-sm text-slate-500">
                      {t('item.familySize')}: {pizza.familySizePriceInSek}{' '}
                      {tCommon('currency.sek')}
                    </div>
                  )}
                </div>
                <svg
                  className="h-5 w-5 text-slate-400 transition-transform group-hover:translate-x-1"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 5l7 7-7 7"
                  />
                </svg>
              </div>
            </Card>
          </Link>
        ))}
      </div>

      {pizzas?.length === 0 && (
        <div className="py-12 text-center text-slate-500">
          {tCommon('pizzas.empty')}
        </div>
      )}
    </div>
  );
};

export default PizzaListPage;

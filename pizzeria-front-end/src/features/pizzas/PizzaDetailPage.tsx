import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { useTranslateKey } from '../../hooks/useTranslateKey';
import { useAuth } from '../../hooks/useAuth';
import { fetchPizza, checkSuitability } from '../../api/pizzas';
import { fetchMenu } from '../../api/menu';
import { getApiErrorMessage } from '../../api/client';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Spinner } from '../../components/ui/Spinner';
import { Alert } from '../../components/ui/Alert';
import { getDietaryBadgeVariant } from '../../utils/dietaryUtils';
import { AddToCartModal } from '../cart/AddToCartModal';
import type { PizzaSuitabilityResponse } from '../../types/api';

export const PizzaDetailPage = () => {
  const { t } = useTranslation('menu');
  const { t: tCommon } = useTranslation('common');
  const { translateKey } = useTranslateKey();
  const pizzeriaCode = usePizzeriaCode();
  const { pizzaId } = useParams<{ pizzaId: string }>();
  const { isAuthenticated } = useAuth();

  const [suitabilityResult, setSuitabilityResult] =
    useState<PizzaSuitabilityResponse | null>(null);
  const [suitabilityError, setSuitabilityError] = useState<string | null>(null);
  const [isAddToCartOpen, setIsAddToCartOpen] = useState(false);

  const { data: pizza, isLoading, error } = useQuery({
    queryKey: ['pizza', pizzeriaCode, pizzaId],
    queryFn: () => fetchPizza(pizzeriaCode, pizzaId!).then((res) => res.data),
    enabled: !!pizzaId,
  });

  const { data: menu } = useQuery({
    queryKey: ['menu', pizzeriaCode],
    queryFn: () => fetchMenu(pizzeriaCode).then((res) => res.data),
  });

  const customisations = menu?.pizzaCustomisations ?? [];

  const suitabilityMutation = useMutation({
    mutationFn: () =>
      checkSuitability({
        pizzaId: pizzaId!,
        additionalIngredientIds: null,
        removedIngredientIds: null,
      }).then((res) => res.data),
    onSuccess: (data) => {
      setSuitabilityResult(data);
      setSuitabilityError(null);
    },
    onError: (err) => {
      setSuitabilityError(getApiErrorMessage(err));
      setSuitabilityResult(null);
    },
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  if (error || !pizza) {
    return (
      <Alert variant="error">
        {tCommon('status.error')}
      </Alert>
    );
  }

  return (
    <div className="space-y-6">
      <Link
        to={`/${pizzeriaCode}/pizzas`}
        className="inline-flex items-center text-sm text-slate-600 hover:text-slate-900"
      >
        <svg
          className="mr-1 h-4 w-4"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 19l-7-7 7-7"
          />
        </svg>
        {tCommon('actions.back')}
      </Link>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Pizza Info */}
        <Card padding="lg">
          <div className="mb-4">
            <span className="text-sm text-slate-500">#{pizza.dishNumber}</span>
            <h1 className="text-2xl font-bold text-slate-900">
              {translateKey(pizza.nameKey)}
            </h1>
          </div>

          <p className="text-slate-600">
            {translateKey(pizza.descriptionKey)}
          </p>

          <div className="mt-4">
            <Badge variant={getDietaryBadgeVariant(pizza.overallDietaryType)}>
              {t(`item.dietary.${pizza.overallDietaryType}`)}
            </Badge>
          </div>

          <div className="mt-6 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-slate-600">{t('item.regularSize')}</span>
              <span className="text-xl font-bold text-primary-600">
                {pizza.priceInSek} {tCommon('currency.sek')}
              </span>
            </div>
            {pizza.familySizePriceInSek && (
              <div className="flex items-center justify-between">
                <span className="text-slate-600">{t('item.familySize')}</span>
                <span className="text-xl font-bold text-primary-600">
                  {pizza.familySizePriceInSek} {tCommon('currency.sek')}
                </span>
              </div>
            )}
          </div>

          <div className="mt-6">
            <Button
              size="lg"
              className="w-full"
              onClick={() => setIsAddToCartOpen(true)}
            >
              {t('cart.addToCart')}
            </Button>
          </div>

          {/* Ingredients */}
          <div className="mt-6">
            <h3 className="mb-2 font-medium text-slate-900">
              {t('item.ingredients')}
            </h3>
            <div className="flex flex-wrap gap-2">
              {pizza.ingredients.map((ingredient, index) => (
                <Badge key={index} variant="default">
                  {translateKey(ingredient.ingredientKey)}
                </Badge>
              ))}
            </div>
          </div>
        </Card>

        {/* Suitability Check */}
        <Card padding="lg">
          <h2 className="mb-2 text-lg font-semibold text-slate-900">
            {t('suitability.title')}
          </h2>
          <p className="mb-4 text-sm text-slate-600">
            {t('suitability.subtitle')}
          </p>

          {!isAuthenticated ? (
            <Alert variant="info">
              <p>{t('suitability.loginRequired')}</p>
              <Link
                to={`/${pizzeriaCode}/login`}
                className="mt-2 inline-block font-medium text-primary-600 hover:text-primary-500"
              >
                {tCommon('nav.signIn')}
              </Link>
            </Alert>
          ) : (
            <>
              <Button
                onClick={() => suitabilityMutation.mutate()}
                isLoading={suitabilityMutation.isPending}
                disabled={suitabilityMutation.isPending}
              >
                {suitabilityMutation.isPending
                  ? t('suitability.checking')
                  : t('suitability.check')}
              </Button>

              {suitabilityError && (
                <Alert variant="error" className="mt-4">
                  {suitabilityError}
                </Alert>
              )}

              {suitabilityResult && (
                <div className="mt-4 space-y-3">
                  <Alert
                    variant={suitabilityResult.suitable ? 'success' : 'warning'}
                  >
                    {suitabilityResult.suitable
                      ? t('suitability.suitable')
                      : t('suitability.notSuitable')}
                  </Alert>

                  {suitabilityResult.violations.length > 0 && (
                    <div>
                      <h4 className="font-medium text-slate-900">
                        {t('suitability.violations')}
                      </h4>
                      <ul className="mt-1 list-inside list-disc text-sm text-slate-600">
                        {suitabilityResult.violations.map((v, i) => {
                          // Extract and translate ingredient keys from violation text
                          const translatedViolation = v.replace(
                            /translation\.key\.ingredient\.(\w+)/g,
                            (match) => translateKey(match)
                          );
                          return <li key={i}>{translatedViolation}</li>;
                        })}
                      </ul>
                    </div>
                  )}

                  {suitabilityResult.suggestions.length > 0 && (
                    <div>
                      <h4 className="font-medium text-slate-900">
                        {t('suitability.suggestions')}
                      </h4>
                      <ul className="mt-1 list-inside list-disc text-sm text-slate-600">
                        {suitabilityResult.suggestions.map((s, i) => {
                          // Extract and translate ingredient keys from suggestion text
                          const translatedSuggestion = s.replace(
                            /translation\.key\.ingredient\.(\w+)/g,
                            (match) => translateKey(match)
                          );
                          return <li key={i}>{translatedSuggestion}</li>;
                        })}
                      </ul>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </Card>
      </div>

      <AddToCartModal
        isOpen={isAddToCartOpen}
        onClose={() => setIsAddToCartOpen(false)}
        menuItemId={pizza.id}
        menuItemNameKey={pizza.nameKey}
        priceRegular={parseFloat(pizza.priceInSek)}
        priceFamily={pizza.familySizePriceInSek ? parseFloat(pizza.familySizePriceInSek) : null}
        customisations={customisations}
      />
    </div>
  );
};

export default PizzaDetailPage;

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { useTranslateKey } from '../../hooks/useTranslateKey';
import { fetchMenu } from '../../api/menu';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Spinner } from '../../components/ui/Spinner';
import { Alert } from '../../components/ui/Alert';
import { getDietaryBadgeVariant } from '../../utils/dietaryUtils';
import type { MenuSectionResponse, MenuItemResponse } from '../../types/api';

export const MenuPage = () => {
  const { t } = useTranslation('menu');
  const { t: tCommon } = useTranslation('common');
  const { translateKey } = useTranslateKey();
  const pizzeriaCode = usePizzeriaCode();
  const [expandedSections, setExpandedSections] = useState<Set<string>>(
    new Set()
  );

  const { data: menu, isLoading, error } = useQuery({
    queryKey: ['menu', pizzeriaCode],
    queryFn: () => fetchMenu(pizzeriaCode).then((res) => res.data),
  });

  const toggleSection = (sectionId: string) => {
    setExpandedSections((prev) => {
      const next = new Set(prev);
      if (next.has(sectionId)) {
        next.delete(sectionId);
      } else {
        next.add(sectionId);
      }
      return next;
    });
  };


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
    <div className="space-y-8">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-slate-900">{t('title')}</h1>
        <p className="mt-2 text-slate-600">{t('subtitle')}</p>
      </div>

      {/* Menu Sections */}
      <div className="space-y-6">
        {menu?.sections.map((section: MenuSectionResponse) => (
          <Card key={section.id} padding="none">
            <button
              onClick={() => toggleSection(section.id)}
              className="flex w-full items-center justify-between p-4 text-left hover:bg-slate-50"
            >
              <h2 className="text-xl font-semibold text-slate-900">
                {translateKey(section.translationKey)}
              </h2>
              <svg
                className={`h-5 w-5 text-slate-400 transition-transform ${
                  expandedSections.has(section.id) ? 'rotate-180' : ''
                }`}
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M19 9l-7 7-7-7"
                />
              </svg>
            </button>

            {expandedSections.has(section.id) && (
              <div className="border-t border-slate-200">
                <div className="grid gap-4 p-4 sm:grid-cols-2 lg:grid-cols-3">
                  {section.items.map((item: MenuItemResponse) => (
                    <MenuItemCard
                      key={item.id}
                      item={item}
                      pizzeriaCode={pizzeriaCode}
                      translateKey={translateKey}
                    />
                  ))}
                </div>
              </div>
            )}
          </Card>
        ))}
      </div>

      {/* Customisations */}
      {menu?.pizzaCustomisations && menu.pizzaCustomisations.length > 0 && (
        <Card padding="none">
          <div className="border-b border-slate-200 p-4">
            <h2 className="text-xl font-semibold text-slate-900">
              {t('customisations.title')}
            </h2>
            <p className="text-sm text-slate-600">
              {t('customisations.subtitle')}
            </p>
          </div>
          <div className="grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-4">
            {menu.pizzaCustomisations.map((customisation) => (
              <div
                key={customisation.id}
                className="flex items-center justify-between rounded-lg border border-slate-200 p-3"
              >
                <span className="text-sm font-medium text-slate-700">
                  {translateKey(customisation.nameKey)}
                </span>
                <span className="text-sm text-slate-500">
                  +{customisation.priceInSek} {tCommon('currency.sek')}
                </span>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
};

interface MenuItemCardProps {
  item: MenuItemResponse;
  pizzeriaCode: string;
  translateKey: (key: string) => string;
}

const MenuItemCard = ({
  item,
  pizzeriaCode,
  translateKey,
}: MenuItemCardProps) => {
  const { t } = useTranslation('menu');
  const { t: tCommon } = useTranslation('common');

  // Get unique allergens from ingredients
  const allergens = [
    ...new Set(item.ingredients.flatMap((i) => i.allergenTags)),
  ];

  return (
    <Link
      to={`/${pizzeriaCode}/pizzas/${item.id}`}
      className="block rounded-lg border border-slate-200 p-4 transition-shadow hover:shadow-md"
    >
      <div className="flex items-start justify-between">
        <div>
          <span className="text-sm text-slate-500">#{item.dishNumber}</span>
          <h3 className="font-semibold text-slate-900">
            {translateKey(item.nameKey)}
          </h3>
        </div>
        <div className="text-right">
          <div className="font-medium text-primary-600">
            {item.priceInSek} {tCommon('currency.sek')}
          </div>
          {item.familySizePriceInSek && (
            <div className="text-xs text-slate-500">
              {t('item.familySize')}: {item.familySizePriceInSek}{' '}
              {tCommon('currency.sek')}
            </div>
          )}
        </div>
      </div>

      <p className="mt-2 text-sm text-slate-600 line-clamp-2">
        {translateKey(item.descriptionKey)}
      </p>

      <div className="mt-3 flex flex-wrap gap-1">
        <Badge
          variant={getDietaryBadgeVariant(item.overallDietaryType)}
          size="sm"
        >
          {t(`item.dietary.${item.overallDietaryType}`)}
        </Badge>
      </div>

      {allergens.length > 0 && (
        <div className="mt-2 text-xs text-slate-500">
          <span className="font-medium">{t('item.allergens')}:</span>{' '}
          {allergens.join(', ')}
        </div>
      )}
    </Link>
  );
};

export default MenuPage;

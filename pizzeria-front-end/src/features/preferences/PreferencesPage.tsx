import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  fetchDiet,
  updateDiet,
  fetchPreferredIngredients,
  addPreferredIngredient,
  removePreferredIngredient,
} from '../../api/preferences';
import { fetchMenu } from '../../api/menu';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { useTranslateKey } from '../../hooks/useTranslateKey';
import { getApiErrorMessage } from '../../api/client';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Select } from '../../components/ui/Select';
import { Alert } from '../../components/ui/Alert';
import { Spinner } from '../../components/ui/Spinner';
import type { DietType } from '../../types/api';

const DIET_OPTIONS: { value: DietType; label: string }[] = [
  { value: 'NONE', label: 'preferences.diet.NONE' },
  { value: 'VEGAN', label: 'preferences.diet.VEGAN' },
  { value: 'VEGETARIAN', label: 'preferences.diet.VEGETARIAN' },
  { value: 'CARNIVORE', label: 'preferences.diet.CARNIVORE' },
];

export const PreferencesPage = () => {
  const { t } = useTranslation('menu');
  const { t: tCommon } = useTranslation('common');
  const { translateKey } = useTranslateKey();
  const pizzeriaCode = usePizzeriaCode();
  const queryClient = useQueryClient();

  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [selectedIngredient, setSelectedIngredient] = useState('');

  // Fetch diet preference
  const { data: dietData, isLoading: isDietLoading } = useQuery({
    queryKey: ['diet'],
    queryFn: () => fetchDiet().then((res) => res.data),
  });

  // Fetch preferred ingredients
  const { data: preferredIngredients, isLoading: isIngredientsLoading } =
    useQuery({
      queryKey: ['preferred-ingredients'],
      queryFn: () => fetchPreferredIngredients().then((res) => res.data),
    });

  // Fetch menu to get available ingredients
  const { data: menu } = useQuery({
    queryKey: ['menu', pizzeriaCode],
    queryFn: () => fetchMenu(pizzeriaCode).then((res) => res.data),
  });

  // Get all unique ingredients from menu
  const allIngredients = menu?.sections.flatMap((section) =>
    section.items.flatMap((item) => item.ingredients)
  ) ?? [];

  const uniqueIngredients = Array.from(
    new Map(allIngredients.map((i) => [i.id, i])).values()
  );

  const preferredIds = new Set(preferredIngredients?.map((p) => p.ingredientId));

  const availableIngredients = uniqueIngredients.filter(
    (i) => !preferredIds.has(i.id)
  );

  // Update diet mutation
  const dietMutation = useMutation({
    mutationFn: (diet: DietType) => updateDiet({ diet }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['diet'] });
      setSuccess(t('preferences.diet.updateSuccess'));
      setError(null);
    },
    onError: (err) => {
      setError(getApiErrorMessage(err));
      setSuccess(null);
    },
  });

  // Add ingredient mutation
  const addIngredientMutation = useMutation({
    mutationFn: (ingredientId: string) =>
      addPreferredIngredient({ ingredientId }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['preferred-ingredients'] });
      setSelectedIngredient('');
      setSuccess(t('preferences.ingredients.addSuccess'));
      setError(null);
    },
    onError: (err) => {
      setError(getApiErrorMessage(err));
      setSuccess(null);
    },
  });

  // Remove ingredient mutation
  const removeIngredientMutation = useMutation({
    mutationFn: (ingredientId: string) => removePreferredIngredient(ingredientId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['preferred-ingredients'] });
      setSuccess(t('preferences.ingredients.removeSuccess'));
      setError(null);
    },
    onError: (err) => {
      setError(getApiErrorMessage(err));
      setSuccess(null);
    },
  });

  const handleDietChange = (diet: string) => {
    dietMutation.mutate(diet as DietType);
  };

  const handleAddIngredient = () => {
    if (selectedIngredient) {
      addIngredientMutation.mutate(selectedIngredient);
    }
  };

  if (isDietLoading || isIngredientsLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">
        {t('preferences.title')}
      </h1>

      {error && (
        <Alert variant="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert variant="success" onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {/* Diet Preference */}
      <Card padding="lg">
        <h2 className="text-lg font-semibold text-slate-900">
          {t('preferences.diet.title')}
        </h2>
        <p className="mt-1 text-sm text-slate-600">
          {t('preferences.diet.subtitle')}
        </p>

        <p className="mt-3 text-sm text-slate-700">
          <span className="font-medium">{t('preferences.diet.current')}:</span>{' '}
          {t(`preferences.diet.${dietData?.diet ?? 'NONE'}`)}
        </p>

        <div className="mt-3">
          <Select
            options={DIET_OPTIONS.map((opt) => ({
              value: opt.value,
              label: t(opt.label),
            }))}
            value={dietData?.diet ?? 'NONE'}
            onChange={(e) => handleDietChange(e.target.value)}
            disabled={dietMutation.isPending}
          />
        </div>
      </Card>

      {/* Preferred Ingredients */}
      <Card padding="lg">
        <h2 className="text-lg font-semibold text-slate-900">
          {t('preferences.ingredients.title')}
        </h2>
        <p className="mt-1 text-sm text-slate-600">
          {t('preferences.ingredients.subtitle')}
        </p>

        {/* Add ingredient */}
        <div className="mt-4 flex gap-2">
          <Select
            options={availableIngredients.map((i) => ({
              value: i.id,
              label: translateKey(i.ingredientKey),
            }))}
            value={selectedIngredient}
            onChange={(e) => setSelectedIngredient(e.target.value)}
            placeholder={t('preferences.ingredients.add')}
            className="flex-1"
          />
          <Button
            onClick={handleAddIngredient}
            disabled={!selectedIngredient || addIngredientMutation.isPending}
            isLoading={addIngredientMutation.isPending}
          >
            {tCommon('actions.add')}
          </Button>
        </div>

        {/* List of preferred ingredients */}
        <div className="mt-4">
          <h3 className="text-sm font-medium text-slate-700 mb-2">
            {t('preferences.ingredients.selected')}
          </h3>
          {preferredIngredients && preferredIngredients.length > 0 ? (
            <ul className="divide-y divide-slate-200 border rounded-md">
              {preferredIngredients.map((pref) => {
                const ingredient = uniqueIngredients.find(
                  (i) => i.id === pref.ingredientId
                );
                return (
                  <li
                    key={pref.ingredientId}
                    className="flex items-center justify-between py-3 px-3"
                  >
                    <span className="text-slate-900">
                      {ingredient
                        ? translateKey(ingredient.ingredientKey)
                        : pref.ingredientId}
                    </span>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() =>
                        removeIngredientMutation.mutate(pref.ingredientId)
                      }
                      disabled={removeIngredientMutation.isPending}
                    >
                      {t('preferences.ingredients.remove')}
                    </Button>
                  </li>
                );
              })}
            </ul>
          ) : (
            <p className="text-sm text-slate-500">
              {t('preferences.ingredients.noPreferences')}
            </p>
          )}
        </div>
      </Card>
    </div>
  );
};

export default PreferencesPage;

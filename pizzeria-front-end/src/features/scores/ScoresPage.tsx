import { useState, type FormEvent } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { useTranslateKey } from '../../hooks/useTranslateKey';
import { createScore, fetchMyScores } from '../../api/scores';
import { fetchPizzas } from '../../api/pizzas';
import { getApiErrorMessage } from '../../api/client';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Select } from '../../components/ui/Select';
import { Input } from '../../components/ui/Input';
import { Alert } from '../../components/ui/Alert';
import { Spinner } from '../../components/ui/Spinner';
import { Badge } from '../../components/ui/Badge';
import type { PizzaType } from '../../types/api';

export const ScoresPage = () => {
  const { t } = useTranslation('menu');
  const { t: tCommon } = useTranslation('common');
  const { translateKey } = useTranslateKey();
  const pizzeriaCode = usePizzeriaCode();
  const queryClient = useQueryClient();

  const [selectedPizza, setSelectedPizza] = useState('');
  const [pizzaType, setPizzaType] = useState<PizzaType>('TEMPLATE');
  const [score, setScore] = useState('5');
  const [comment, setComment] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Fetch pizzas for selection
  const { data: pizzas } = useQuery({
    queryKey: ['pizzas', pizzeriaCode],
    queryFn: () => fetchPizzas(pizzeriaCode).then((res) => res.data),
  });

  // Fetch user's scores
  const { data: scores, isLoading } = useQuery({
    queryKey: ['pizza-scores'],
    queryFn: () => fetchMyScores().then((res) => res.data),
  });

  // Create score mutation
  const scoreMutation = useMutation({
    mutationFn: () =>
      createScore({
        pizzaId: selectedPizza,
        pizzaType,
        score: parseInt(score),
        comment: comment || null,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['pizza-scores'] });
      setSelectedPizza('');
      setScore('5');
      setComment('');
      setSuccess(t('scores.success'));
      setError(null);
    },
    onError: (err) => {
      setError(getApiErrorMessage(err));
      setSuccess(null);
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    scoreMutation.mutate();
  };

  const renderStars = (rating: number) => {
    return (
      <span className="text-yellow-500">
        {'★'.repeat(rating)}
        {'☆'.repeat(5 - rating)}
      </span>
    );
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">{t('scores.title')}</h1>
      <p className="text-slate-600">{t('scores.subtitle')}</p>

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

      {/* Add Score Form */}
      <Card padding="lg">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">
          {t('scores.addScore')}
        </h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Select
            label={t('scores.pizza')}
            options={
              pizzas?.map((p) => ({
                value: p.id,
                label: `#${p.dishNumber} - ${translateKey(p.nameKey)}`,
              })) ?? []
            }
            value={selectedPizza}
            onChange={(e) => setSelectedPizza(e.target.value)}
            placeholder={t('scores.selectPizza')}
            required
          />

          <Select
            label={t('scores.pizzaType.TEMPLATE')}
            options={[
              { value: 'TEMPLATE', label: t('scores.pizzaType.TEMPLATE') },
              { value: 'CUSTOM', label: t('scores.pizzaType.CUSTOM') },
            ]}
            value={pizzaType}
            onChange={(e) => setPizzaType(e.target.value as PizzaType)}
          />

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">
              {t('scores.score')}
            </label>
            <div className="flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((num) => (
                <button
                  key={num}
                  type="button"
                  onClick={() => setScore(num.toString())}
                  className={`text-2xl transition-colors ${
                    parseInt(score) >= num
                      ? 'text-yellow-500'
                      : 'text-slate-300 hover:text-yellow-400'
                  }`}
                >
                  ★
                </button>
              ))}
              <span className="ml-2 text-sm text-slate-600">{score}/5</span>
            </div>
          </div>

          <Input
            label={t('scores.comment')}
            type="text"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder={t('scores.commentPlaceholder')}
          />

          <Button
            type="submit"
            disabled={!selectedPizza || scoreMutation.isPending}
            isLoading={scoreMutation.isPending}
          >
            {scoreMutation.isPending
              ? t('scores.submitting')
              : t('scores.submit')}
          </Button>
        </form>
      </Card>

      {/* Scores List */}
      <Card padding="lg">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">
          {t('scores.title')}
        </h2>

        {scores && scores.length > 0 ? (
          <ul className="divide-y divide-slate-200">
            {scores.map((s) => {
              const pizza = pizzas?.find((p) => p.id === s.pizzaId);
              return (
                <li key={s.id} className="py-4">
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="font-medium text-slate-900">
                        {pizza
                          ? `#${pizza.dishNumber} - ${translateKey(pizza.nameKey)}`
                          : tCommon('scores.unknownPizza')}
                      </div>
                      <div className="mt-1">{renderStars(s.score)}</div>
                      {s.comment && (
                        <p className="mt-1 text-sm text-slate-600">
                          {s.comment}
                        </p>
                      )}
                    </div>
                    <div className="text-right">
                      <Badge
                        variant={s.pizzaType === 'TEMPLATE' ? 'info' : 'default'}
                        size="sm"
                      >
                        {t(`scores.pizzaType.${s.pizzaType}`)}
                      </Badge>
                      <p className="mt-1 text-xs text-slate-500">
                        {new Date(s.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        ) : (
          <p className="text-sm text-slate-500">{t('scores.noScores')}</p>
        )}
      </Card>
    </div>
  );
};

export default ScoresPage;

import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import type { PizzaSize, PizzaCustomisationResponse } from '../../types/api';
import { useCart } from './CartProvider';
import { useTranslateKey } from '../../hooks/useTranslateKey';

interface AddToCartModalProps {
  isOpen: boolean;
  onClose: () => void;
  menuItemId: string;
  menuItemNameKey: string;
  priceRegular: number;
  priceFamily: number | null;
  customisations: PizzaCustomisationResponse[];
}

export const AddToCartModal = ({
  isOpen,
  onClose,
  menuItemId,
  menuItemNameKey,
  priceRegular,
  priceFamily,
  customisations,
}: AddToCartModalProps) => {
  const { t } = useTranslation('menu');
  const { t: tCommon } = useTranslation('common');
  const { translateKey } = useTranslateKey();
  const { addItem } = useCart();

  const [size, setSize] = useState<PizzaSize>('REGULAR');
  const [quantity, setQuantity] = useState(1);
  const [selectedCustomisations, setSelectedCustomisations] = useState<Set<string>>(new Set());
  const [specialInstructions, setSpecialInstructions] = useState('');

  const hasFamilySize = priceFamily !== null;
  const basePrice = size === 'FAMILY' && priceFamily !== null ? priceFamily : priceRegular;

  const customisationDetails = useMemo(() => {
    return customisations
      .filter((c) => selectedCustomisations.has(c.id))
      .map((c) => ({
        customisationId: c.id,
        customisationNameKey: c.nameKey,
        price: size === 'FAMILY' && c.familySizePriceInSek ? parseFloat(c.familySizePriceInSek) : parseFloat(c.priceInSek),
      }));
  }, [customisations, selectedCustomisations, size]);

  const customisationsTotal = customisationDetails.reduce((sum, c) => sum + c.price, 0);
  const itemTotal = (basePrice + customisationsTotal) * quantity;

  const handleToggleCustomisation = (custId: string) => {
    setSelectedCustomisations((prev) => {
      const next = new Set(prev);
      if (next.has(custId)) {
        next.delete(custId);
      } else {
        next.add(custId);
      }
      return next;
    });
  };

  const handleAddToCart = () => {
    addItem({
      menuItemId,
      menuItemNameKey,
      size,
      quantity,
      basePrice,
      customisations: customisationDetails,
      specialInstructions: specialInstructions.trim() || undefined,
    });
    onClose();
    // Reset for next time
    setSize('REGULAR');
    setQuantity(1);
    setSelectedCustomisations(new Set());
    setSpecialInstructions('');
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <Card className="w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-bold text-slate-900">
              {translateKey(menuItemNameKey)}
            </h2>
            <button
              onClick={onClose}
              className="text-slate-400 hover:text-slate-600 text-2xl leading-none"
            >
              &times;
            </button>
          </div>

          {/* Size Selection */}
          {hasFamilySize && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-slate-700 mb-2">{t('cart.selectSize')}</h3>
              <div className="flex gap-3">
                <button
                  onClick={() => setSize('REGULAR')}
                  className={`flex-1 py-3 px-4 rounded-lg border-2 transition-colors ${
                    size === 'REGULAR'
                      ? 'border-primary-600 bg-primary-50 text-primary-700'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <div className="font-medium">{t('cart.sizeRegular')}</div>
                  <div className="text-sm text-slate-600">{priceRegular.toFixed(0)} kr</div>
                </button>
                <button
                  onClick={() => setSize('FAMILY')}
                  className={`flex-1 py-3 px-4 rounded-lg border-2 transition-colors ${
                    size === 'FAMILY'
                      ? 'border-primary-600 bg-primary-50 text-primary-700'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <div className="font-medium">{t('cart.sizeFamily')}</div>
                  <div className="text-sm text-slate-600">{priceFamily.toFixed(0)} kr</div>
                </button>
              </div>
            </div>
          )}

          {/* Customisations */}
          {customisations.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-slate-700 mb-2">{t('cart.addExtras')}</h3>
              <div className="grid grid-cols-2 gap-2">
                {customisations.map((cust) => {
                  const custPrice =
                    size === 'FAMILY' && cust.familySizePriceInSek
                      ? parseFloat(cust.familySizePriceInSek)
                      : parseFloat(cust.priceInSek);
                  const isSelected = selectedCustomisations.has(cust.id);
                  return (
                    <button
                      key={cust.id}
                      onClick={() => handleToggleCustomisation(cust.id)}
                      className={`p-2 rounded-lg border text-left transition-colors ${
                        isSelected
                          ? 'border-primary-600 bg-primary-50'
                          : 'border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleToggleCustomisation(cust.id)}
                          className="h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
                        />
                        <div className="flex-1 min-w-0">
                          <div className="text-sm font-medium text-slate-700 truncate">
                            {translateKey(cust.nameKey)}
                          </div>
                          <div className="text-xs text-slate-500">+{custPrice.toFixed(0)} kr</div>
                        </div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Quantity */}
          <div className="mb-6">
            <h3 className="text-sm font-semibold text-slate-700 mb-2">{t('cart.quantity')}</h3>
            <div className="flex items-center gap-4">
              <button
                onClick={() => setQuantity(Math.max(1, quantity - 1))}
                className="h-10 w-10 rounded-lg border border-slate-200 text-lg font-bold text-slate-600 hover:bg-slate-50"
              >
                -
              </button>
              <span className="text-xl font-bold w-8 text-center">{quantity}</span>
              <button
                onClick={() => setQuantity(quantity + 1)}
                className="h-10 w-10 rounded-lg border border-slate-200 text-lg font-bold text-slate-600 hover:bg-slate-50"
              >
                +
              </button>
            </div>
          </div>

          {/* Special Instructions */}
          <div className="mb-6">
            <h3 className="text-sm font-semibold text-slate-700 mb-2">
              {t('cart.specialInstructions')}
            </h3>
            <textarea
              value={specialInstructions}
              onChange={(e) => setSpecialInstructions(e.target.value)}
              placeholder={t('cart.specialInstructionsPlaceholder')}
              className="w-full rounded-lg border border-slate-200 p-3 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
              rows={2}
            />
          </div>

          {/* Total */}
          <div className="flex items-center justify-between py-4 border-t border-slate-200 mb-4">
            <span className="text-lg font-semibold text-slate-700">{t('cart.total')}</span>
            <span className="text-2xl font-bold text-slate-900">{itemTotal.toFixed(0)} kr</span>
          </div>

          {/* Actions */}
          <div className="flex gap-3">
            <Button variant="secondary" onClick={onClose} className="flex-1">
              {tCommon('actions.cancel')}
            </Button>
            <Button onClick={handleAddToCart} className="flex-1">
              {t('cart.addToCart')}
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default AddToCartModal;

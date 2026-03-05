import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { useCart } from './CartProvider';
import { usePizzeriaContext } from '../../routes/PizzeriaProvider';
import { useTranslateKey } from '../../hooks/useTranslateKey';

export const CartPage = () => {
  const { t } = useTranslation('menu');
  const { translateKey } = useTranslateKey();
  const { pizzeriaCode } = usePizzeriaContext();
  const { items, itemCount, subtotal, updateQuantity, removeItem, clearCart } = useCart();

  if (items.length === 0) {
    return (
      <div className="text-center py-12">
        <span className="text-6xl block mb-4">🛒</span>
        <h1 className="text-2xl font-bold text-slate-900 mb-2">{t('cart.emptyTitle')}</h1>
        <p className="text-slate-600 mb-6">{t('cart.emptyMessage')}</p>
        <Link to={`/${pizzeriaCode}/menu`}>
          <Button>{t('cart.browseMenu')}</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">{t('cart.title')}</h1>
        <button
          onClick={clearCart}
          className="text-sm text-red-600 hover:text-red-700 font-medium"
        >
          {t('cart.clearAll')}
        </button>
      </div>

      <div className="space-y-4">
        {items.map((item) => {
          const customisationsTotal = item.customisations.reduce((sum, c) => sum + c.price, 0);
          const itemTotal = (item.basePrice + customisationsTotal) * item.quantity;

          return (
            <Card key={item.id} padding="md">
              <div className="flex gap-4">
                <div className="flex-1">
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="font-semibold text-slate-900">
                        {translateKey(item.menuItemNameKey)}
                      </h3>
                      <p className="text-sm text-slate-500">
                        {item.size === 'FAMILY' ? t('cart.sizeFamily') : t('cart.sizeRegular')}
                        {' - '}{item.basePrice.toFixed(0)} kr
                      </p>
                    </div>
                    <button
                      onClick={() => removeItem(item.id)}
                      className="text-slate-400 hover:text-red-600 text-xl leading-none"
                    >
                      &times;
                    </button>
                  </div>

                  {item.customisations.length > 0 && (
                    <div className="mt-2 space-y-1">
                      {item.customisations.map((cust) => (
                        <div key={cust.customisationId} className="text-sm text-slate-600">
                          + {translateKey(cust.customisationNameKey)} ({cust.price.toFixed(0)} kr)
                        </div>
                      ))}
                    </div>
                  )}

                  {item.specialInstructions && (
                    <p className="mt-2 text-sm text-slate-500 italic">
                      &ldquo;{item.specialInstructions}&rdquo;
                    </p>
                  )}

                  <div className="mt-3 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => updateQuantity(item.id, item.quantity - 1)}
                        className="h-8 w-8 rounded border border-slate-200 text-slate-600 hover:bg-slate-50"
                      >
                        -
                      </button>
                      <span className="font-medium w-6 text-center">{item.quantity}</span>
                      <button
                        onClick={() => updateQuantity(item.id, item.quantity + 1)}
                        className="h-8 w-8 rounded border border-slate-200 text-slate-600 hover:bg-slate-50"
                      >
                        +
                      </button>
                    </div>
                    <span className="font-bold text-slate-900">{itemTotal.toFixed(0)} kr</span>
                  </div>
                </div>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Summary */}
      <Card padding="lg" className="bg-slate-50">
        <div className="space-y-3">
          <div className="flex justify-between text-slate-600">
            <span>
              {t('cart.items')} ({itemCount})
            </span>
            <span>{subtotal.toFixed(0)} kr</span>
          </div>
          <div className="flex justify-between text-lg font-bold text-slate-900 pt-3 border-t border-slate-200">
            <span>{t('cart.subtotal')}</span>
            <span>{subtotal.toFixed(0)} kr</span>
          </div>
        </div>

        <div className="mt-6">
          <Link to={`/${pizzeriaCode}/checkout`}>
            <Button size="lg" className="w-full">
              {t('cart.proceedToCheckout')}
            </Button>
          </Link>
        </div>
      </Card>

      <div className="text-center">
        <Link
          to={`/${pizzeriaCode}/menu`}
          className="text-primary-600 hover:text-primary-700 font-medium"
        >
          {t('cart.continueShopping')}
        </Link>
      </div>
    </div>
  );
};

export default CartPage;

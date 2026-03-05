import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Alert } from '../../components/ui/Alert';
import { Spinner } from '../../components/ui/Spinner';
import { useCart } from '../cart/CartProvider';
import { usePizzeriaContext } from '../../routes/PizzeriaProvider';
import { useTranslateKey } from '../../hooks/useTranslateKey';
import { fetchAddresses } from '../../api/addresses';
import { createOrder } from '../../api/orders';
import type { FulfillmentType, CreateOrderRequest } from '../../types/api';

const DELIVERY_FEE = 49;

export const CheckoutPage = () => {
  const { t } = useTranslation('menu');
  const { t: tCommon } = useTranslation('common');
  const { translateKey } = useTranslateKey();
  const navigate = useNavigate();
  const { pizzeriaCode } = usePizzeriaContext();
  const { items, subtotal, clearCart } = useCart();

  const [fulfillmentType, setFulfillmentType] = useState<FulfillmentType>('PICKUP');
  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null);
  const [useNewAddress, setUseNewAddress] = useState(false);
  const [newAddress, setNewAddress] = useState({
    street: '',
    postalCode: '',
    city: '',
    phone: '',
    instructions: '',
  });
  const [customerNotes, setCustomerNotes] = useState('');
  const [error, setError] = useState<string | null>(null);

  const { data: addresses = [], isLoading: addressesLoading } = useQuery({
    queryKey: ['addresses'],
    queryFn: async () => {
      const response = await fetchAddresses();
      return response.data;
    },
  });

  const orderMutation = useMutation({
    mutationFn: (request: CreateOrderRequest) => createOrder(request),
    onSuccess: (response) => {
      clearCart();
      navigate(`/${pizzeriaCode}/orders/${response.data.id}`);
    },
    onError: () => {
      setError(t('checkout.orderError'));
    },
  });

  // Set default address when addresses load
  useEffect(() => {
    if (addresses.length > 0 && !selectedAddressId) {
      const defaultAddr = addresses.find((a) => a.isDefault);
      setSelectedAddressId(defaultAddr?.id ?? addresses[0].id);
    }
  }, [addresses, selectedAddressId]);

  const deliveryFee = fulfillmentType === 'DELIVERY' ? DELIVERY_FEE : 0;
  const total = subtotal + deliveryFee;

  const handlePlaceOrder = () => {
    setError(null);

    const orderItems = items.map((item) => ({
      menuItemId: item.menuItemId,
      size: item.size,
      quantity: item.quantity,
      customisationIds: item.customisations.map((c) => c.customisationId),
      specialInstructions: item.specialInstructions,
    }));

    const request: CreateOrderRequest = {
      fulfillmentType,
      customerNotes: customerNotes.trim() || undefined,
      items: orderItems,
    };

    if (fulfillmentType === 'DELIVERY') {
      if (useNewAddress || addresses.length === 0) {
        if (!newAddress.street || !newAddress.postalCode || !newAddress.city) {
          setError(t('checkout.addressRequired'));
          return;
        }
        request.deliveryStreet = newAddress.street;
        request.deliveryPostalCode = newAddress.postalCode;
        request.deliveryCity = newAddress.city;
        request.deliveryPhone = newAddress.phone || undefined;
        request.deliveryInstructions = newAddress.instructions || undefined;
      } else if (selectedAddressId) {
        const addr = addresses.find((a) => a.id === selectedAddressId);
        if (addr) {
          request.deliveryAddressId = addr.id;
          request.deliveryStreet = addr.street;
          request.deliveryPostalCode = addr.postalCode;
          request.deliveryCity = addr.city;
          request.deliveryPhone = addr.phone || undefined;
          request.deliveryInstructions = addr.instructions || undefined;
        }
      }
    }

    orderMutation.mutate(request);
  };

  if (items.length === 0) {
    return (
      <div className="text-center py-12">
        <h1 className="text-2xl font-bold text-slate-900 mb-2">{t('checkout.emptyCart')}</h1>
        <p className="text-slate-600 mb-6">{t('checkout.emptyCartMessage')}</p>
        <Link to={`/${pizzeriaCode}/menu`}>
          <Button>{t('cart.browseMenu')}</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">{t('checkout.title')}</h1>

      {error && <Alert variant="error">{error}</Alert>}

      {/* Fulfillment Type */}
      <Card padding="lg">
        <h2 className="text-lg font-semibold text-slate-900 mb-4">{t('checkout.fulfillmentType')}</h2>
        <div className="flex gap-4">
          <button
            onClick={() => setFulfillmentType('PICKUP')}
            className={`flex-1 py-4 px-4 rounded-lg border-2 transition-colors ${
              fulfillmentType === 'PICKUP'
                ? 'border-primary-600 bg-primary-50'
                : 'border-slate-200 hover:border-slate-300'
            }`}
          >
            <div className="text-2xl mb-1">🏪</div>
            <div className="font-semibold">{t('checkout.pickup')}</div>
            <div className="text-sm text-slate-500">{t('checkout.pickupDescription')}</div>
          </button>
          <button
            onClick={() => setFulfillmentType('DELIVERY')}
            className={`flex-1 py-4 px-4 rounded-lg border-2 transition-colors ${
              fulfillmentType === 'DELIVERY'
                ? 'border-primary-600 bg-primary-50'
                : 'border-slate-200 hover:border-slate-300'
            }`}
          >
            <div className="text-2xl mb-1">🚗</div>
            <div className="font-semibold">{t('checkout.delivery')}</div>
            <div className="text-sm text-slate-500">+{DELIVERY_FEE} kr</div>
          </button>
        </div>
      </Card>

      {/* Delivery Address */}
      {fulfillmentType === 'DELIVERY' && (
        <Card padding="lg">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">{t('checkout.deliveryAddress')}</h2>

          {addressesLoading ? (
            <Spinner />
          ) : addresses.length > 0 && !useNewAddress ? (
            <div className="space-y-3">
              {addresses.map((addr) => (
                <button
                  key={addr.id}
                  onClick={() => setSelectedAddressId(addr.id)}
                  className={`w-full text-left p-4 rounded-lg border-2 transition-colors ${
                    selectedAddressId === addr.id
                      ? 'border-primary-600 bg-primary-50'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <input
                      type="radio"
                      checked={selectedAddressId === addr.id}
                      onChange={() => setSelectedAddressId(addr.id)}
                      className="h-4 w-4 text-primary-600"
                    />
                    <div>
                      {addr.label && <span className="font-medium">{addr.label}: </span>}
                      <span>{addr.street}, {addr.postalCode} {addr.city}</span>
                      {addr.isDefault && (
                        <span className="ml-2 text-xs bg-primary-100 text-primary-700 px-2 py-0.5 rounded">
                          {t('checkout.defaultAddress')}
                        </span>
                      )}
                    </div>
                  </div>
                </button>
              ))}
              <button
                onClick={() => setUseNewAddress(true)}
                className="text-primary-600 hover:text-primary-700 text-sm font-medium"
              >
                + {t('checkout.useNewAddress')}
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {addresses.length > 0 && (
                <button
                  onClick={() => setUseNewAddress(false)}
                  className="text-primary-600 hover:text-primary-700 text-sm font-medium"
                >
                  ← {t('checkout.useSavedAddress')}
                </button>
              )}
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="sm:col-span-2">
                  <Input
                    label={t('checkout.street')}
                    value={newAddress.street}
                    onChange={(e) => setNewAddress({ ...newAddress, street: e.target.value })}
                    required
                  />
                </div>
                <Input
                  label={t('checkout.postalCode')}
                  value={newAddress.postalCode}
                  onChange={(e) => setNewAddress({ ...newAddress, postalCode: e.target.value })}
                  required
                />
                <Input
                  label={t('checkout.city')}
                  value={newAddress.city}
                  onChange={(e) => setNewAddress({ ...newAddress, city: e.target.value })}
                  required
                />
                <Input
                  label={t('checkout.phone')}
                  value={newAddress.phone}
                  onChange={(e) => setNewAddress({ ...newAddress, phone: e.target.value })}
                />
                <div className="sm:col-span-2">
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    {t('checkout.deliveryInstructions')}
                  </label>
                  <textarea
                    value={newAddress.instructions}
                    onChange={(e) => setNewAddress({ ...newAddress, instructions: e.target.value })}
                    className="w-full rounded-lg border border-slate-300 p-3 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    rows={2}
                    placeholder={t('checkout.deliveryInstructionsPlaceholder')}
                  />
                </div>
              </div>
            </div>
          )}
        </Card>
      )}

      {/* Order Notes */}
      <Card padding="lg">
        <h2 className="text-lg font-semibold text-slate-900 mb-4">{t('checkout.orderNotes')}</h2>
        <textarea
          value={customerNotes}
          onChange={(e) => setCustomerNotes(e.target.value)}
          className="w-full rounded-lg border border-slate-300 p-3 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
          rows={2}
          placeholder={t('checkout.orderNotesPlaceholder')}
        />
      </Card>

      {/* Order Summary */}
      <Card padding="lg" className="bg-slate-50">
        <h2 className="text-lg font-semibold text-slate-900 mb-4">{t('checkout.orderSummary')}</h2>
        <div className="space-y-2 mb-4">
          {items.map((item) => {
            const customisationsTotal = item.customisations.reduce((sum, c) => sum + c.price, 0);
            const itemTotal = (item.basePrice + customisationsTotal) * item.quantity;
            return (
              <div key={item.id} className="flex justify-between text-sm">
                <span>
                  {item.quantity}x {translateKey(item.menuItemNameKey)}
                  <span className="text-slate-500">
                    {' '}({item.size === 'FAMILY' ? t('cart.sizeFamily') : t('cart.sizeRegular')})
                  </span>
                </span>
                <span>{itemTotal.toFixed(0)} kr</span>
              </div>
            );
          })}
        </div>
        <div className="space-y-2 pt-4 border-t border-slate-200">
          <div className="flex justify-between">
            <span>{t('checkout.subtotal')}</span>
            <span>{subtotal.toFixed(0)} kr</span>
          </div>
          {fulfillmentType === 'DELIVERY' && (
            <div className="flex justify-between">
              <span>{t('checkout.deliveryFee')}</span>
              <span>{deliveryFee.toFixed(0)} kr</span>
            </div>
          )}
          <div className="flex justify-between text-lg font-bold pt-2 border-t border-slate-300">
            <span>{t('checkout.total')}</span>
            <span>{total.toFixed(0)} kr</span>
          </div>
        </div>
      </Card>

      {/* Place Order */}
      <div className="flex gap-4">
        <Link to={`/${pizzeriaCode}/cart`} className="flex-1">
          <Button variant="secondary" className="w-full">
            {tCommon('actions.back')}
          </Button>
        </Link>
        <Button
          onClick={handlePlaceOrder}
          isLoading={orderMutation.isPending}
          className="flex-1"
          size="lg"
        >
          {t('checkout.placeOrder')}
        </Button>
      </div>
    </div>
  );
};

export default CheckoutPage;

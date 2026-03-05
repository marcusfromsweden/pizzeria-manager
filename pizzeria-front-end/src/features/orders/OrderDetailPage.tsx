import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Spinner } from '../../components/ui/Spinner';
import { Alert } from '../../components/ui/Alert';
import { usePizzeriaContext } from '../../routes/PizzeriaProvider';
import { useTranslateKey } from '../../hooks/useTranslateKey';
import { fetchOrder, cancelOrder } from '../../api/orders';
import type { OrderStatus } from '../../types/api';

const statusColors: Record<OrderStatus, 'info' | 'success' | 'warning' | 'danger'> = {
  PENDING: 'warning',
  CONFIRMED: 'info',
  PREPARING: 'info',
  READY: 'success',
  OUT_FOR_DELIVERY: 'info',
  DELIVERED: 'success',
  PICKED_UP: 'success',
  CANCELLED: 'danger',
};

export const OrderDetailPage = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const { t } = useTranslation('menu');
  const { translateKey } = useTranslateKey();
  const queryClient = useQueryClient();
  const { pizzeriaCode } = usePizzeriaContext();

  const {
    data: order,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['order', orderId],
    queryFn: async () => {
      if (!orderId) throw new Error('No order ID');
      const response = await fetchOrder(orderId);
      return response.data;
    },
    enabled: !!orderId,
  });

  const cancelMutation = useMutation({
    mutationFn: () => {
      if (!orderId) throw new Error('No order ID');
      return cancelOrder(orderId);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['order', orderId] });
      void queryClient.invalidateQueries({ queryKey: ['orders'] });
    },
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="space-y-4">
        <Alert variant="error">{t('orders.notFound')}</Alert>
        <Link to={`/${pizzeriaCode}/orders`}>
          <Button variant="secondary">{t('orders.backToOrders')}</Button>
        </Link>
      </div>
    );
  }

  const canCancel = order.status === 'PENDING' || order.status === 'CONFIRMED';

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <Link
            to={`/${pizzeriaCode}/orders`}
            className="text-sm text-primary-600 hover:text-primary-700 font-medium"
          >
            ← {t('orders.backToOrders')}
          </Link>
          <h1 className="text-2xl font-bold text-slate-900 mt-2">
            {t('orders.orderNumber')} {order.orderNumber}
          </h1>
        </div>
        <Badge variant={statusColors[order.status]} size="md">
          {t(`orders.status.${order.status}`)}
        </Badge>
      </div>

      {/* Order Info */}
      <Card padding="lg">
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <div className="text-sm text-slate-500">{t('orders.fulfillmentType')}</div>
            <div className="font-medium">
              {order.fulfillmentType === 'DELIVERY' ? '🚗 ' : '🏪 '}
              {t(`orders.fulfillment.${order.fulfillmentType}`)}
            </div>
          </div>
          <div>
            <div className="text-sm text-slate-500">{t('orders.placedAt')}</div>
            <div className="font-medium">
              {new Date(order.createdAt).toLocaleString()}
            </div>
          </div>
          {order.fulfillmentType === 'DELIVERY' && order.deliveryStreet && (
            <div className="sm:col-span-2">
              <div className="text-sm text-slate-500">{t('orders.deliveryAddress')}</div>
              <div className="font-medium">
                {order.deliveryStreet}, {order.deliveryPostalCode} {order.deliveryCity}
              </div>
              {order.deliveryPhone && (
                <div className="text-sm text-slate-600">{order.deliveryPhone}</div>
              )}
              {order.deliveryInstructions && (
                <div className="text-sm text-slate-500 italic mt-1">
                  &ldquo;{order.deliveryInstructions}&rdquo;
                </div>
              )}
            </div>
          )}
          {order.customerNotes && (
            <div className="sm:col-span-2">
              <div className="text-sm text-slate-500">{t('orders.notes')}</div>
              <div className="font-medium">{order.customerNotes}</div>
            </div>
          )}
        </div>
      </Card>

      {/* Order Items */}
      <Card padding="lg">
        <h2 className="text-lg font-semibold text-slate-900 mb-4">{t('orders.orderItems')}</h2>
        <div className="space-y-4">
          {order.items.map((item) => (
            <div key={item.id} className="flex justify-between pb-4 border-b border-slate-100 last:border-0 last:pb-0">
              <div>
                <div className="font-medium text-slate-900">
                  {item.quantity}x {translateKey(item.menuItemNameKey)}
                </div>
                <div className="text-sm text-slate-500">
                  {item.size === 'FAMILY' ? t('cart.sizeFamily') : t('cart.sizeRegular')}
                  {' - '}{parseFloat(item.basePrice).toFixed(0)} kr
                </div>
                {item.customisations.length > 0 && (
                  <div className="mt-1 space-y-0.5">
                    {item.customisations.map((cust) => (
                      <div key={cust.id} className="text-sm text-slate-600">
                        + {translateKey(cust.customisationNameKey)} ({parseFloat(cust.price).toFixed(0)} kr)
                      </div>
                    ))}
                  </div>
                )}
                {item.specialInstructions && (
                  <div className="text-sm text-slate-500 italic mt-1">
                    &ldquo;{item.specialInstructions}&rdquo;
                  </div>
                )}
              </div>
              <div className="font-bold text-slate-900">
                {parseFloat(item.itemTotal).toFixed(0)} kr
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Order Total */}
      <Card padding="lg" className="bg-slate-50">
        <div className="space-y-2">
          <div className="flex justify-between">
            <span>{t('checkout.subtotal')}</span>
            <span>{parseFloat(order.subtotal).toFixed(0)} kr</span>
          </div>
          {parseFloat(order.deliveryFee) > 0 && (
            <div className="flex justify-between">
              <span>{t('checkout.deliveryFee')}</span>
              <span>{parseFloat(order.deliveryFee).toFixed(0)} kr</span>
            </div>
          )}
          <div className="flex justify-between text-lg font-bold pt-2 border-t border-slate-300">
            <span>{t('checkout.total')}</span>
            <span>{parseFloat(order.total).toFixed(0)} kr</span>
          </div>
        </div>
      </Card>

      {/* Actions */}
      {canCancel && (
        <Card padding="lg">
          <div className="flex items-center justify-between">
            <div>
              <div className="font-medium text-slate-900">{t('orders.cancelOrder')}</div>
              <div className="text-sm text-slate-500">{t('orders.cancelOrderDescription')}</div>
            </div>
            <Button
              variant="danger"
              onClick={() => cancelMutation.mutate()}
              isLoading={cancelMutation.isPending}
            >
              {t('orders.cancelButton')}
            </Button>
          </div>
        </Card>
      )}
    </div>
  );
};

export default OrderDetailPage;

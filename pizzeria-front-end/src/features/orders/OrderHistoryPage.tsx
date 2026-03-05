import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Spinner } from '../../components/ui/Spinner';
import { Alert } from '../../components/ui/Alert';
import { usePizzeriaContext } from '../../routes/PizzeriaProvider';
import { fetchOrderHistory, fetchActiveOrders } from '../../api/orders';
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

export const OrderHistoryPage = () => {
  const { t } = useTranslation('menu');
  const { pizzeriaCode } = usePizzeriaContext();

  const {
    data: activeOrders = [],
    isLoading: activeLoading,
    error: activeError,
  } = useQuery({
    queryKey: ['orders', 'active'],
    queryFn: async () => {
      const response = await fetchActiveOrders();
      return response.data;
    },
  });

  const {
    data: allOrders = [],
    isLoading: historyLoading,
    error: historyError,
  } = useQuery({
    queryKey: ['orders', 'history'],
    queryFn: async () => {
      const response = await fetchOrderHistory();
      return response.data;
    },
  });

  // Filter out active orders and cancelled orders from history
  const pastOrders = allOrders.filter(
    (order) =>
      !activeOrders.some((active) => active.id === order.id) && order.status !== 'CANCELLED'
  );

  if (activeLoading || historyLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  if (activeError || historyError) {
    return <Alert variant="error">{t('orders.loadError')}</Alert>;
  }

  if (activeOrders.length === 0 && pastOrders.length === 0) {
    return (
      <div className="text-center py-12">
        <span className="text-6xl block mb-4">📋</span>
        <h1 className="text-2xl font-bold text-slate-900 mb-2">{t('orders.noOrdersTitle')}</h1>
        <p className="text-slate-600 mb-6">{t('orders.noOrdersMessage')}</p>
        <Link to={`/${pizzeriaCode}/menu`}>
          <Button>{t('orders.startOrdering')}</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">{t('orders.title')}</h1>

      {/* Active Orders */}
      {activeOrders.length > 0 && (
        <div>
          <h2 className="text-lg font-semibold text-slate-700 mb-3">{t('orders.activeOrders')}</h2>
          <div className="space-y-3">
            {activeOrders.map((order) => (
              <Link key={order.id} to={`/${pizzeriaCode}/orders/${order.id}`}>
                <Card
                  padding="md"
                  className="cursor-pointer transition-shadow hover:shadow-md border-l-4 border-l-primary-500"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-3">
                        <span className="font-bold text-slate-900">{order.orderNumber}</span>
                        <Badge variant={statusColors[order.status]}>{t(`orders.status.${order.status}`)}</Badge>
                      </div>
                      <div className="text-sm text-slate-500 mt-1">
                        {order.fulfillmentType === 'DELIVERY' ? '🚗 ' : '🏪 '}
                        {t(`orders.fulfillment.${order.fulfillmentType}`)}
                        {' · '}
                        {order.itemCount} {order.itemCount === 1 ? t('orders.item') : t('orders.items')}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-bold text-slate-900">{parseFloat(order.total).toFixed(0)} kr</div>
                      <div className="text-sm text-slate-500">
                        {new Date(order.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Past Orders */}
      {pastOrders.length > 0 && (
        <div>
          <h2 className="text-lg font-semibold text-slate-700 mb-3">{t('orders.pastOrders')}</h2>
          <div className="space-y-3">
            {pastOrders.map((order) => (
              <Link key={order.id} to={`/${pizzeriaCode}/orders/${order.id}`}>
                <Card padding="md" className="cursor-pointer transition-shadow hover:shadow-md">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-3">
                        <span className="font-bold text-slate-900">{order.orderNumber}</span>
                        <Badge variant={statusColors[order.status]}>{t(`orders.status.${order.status}`)}</Badge>
                      </div>
                      <div className="text-sm text-slate-500 mt-1">
                        {order.fulfillmentType === 'DELIVERY' ? '🚗 ' : '🏪 '}
                        {t(`orders.fulfillment.${order.fulfillmentType}`)}
                        {' · '}
                        {order.itemCount} {order.itemCount === 1 ? t('orders.item') : t('orders.items')}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-bold text-slate-900">{parseFloat(order.total).toFixed(0)} kr</div>
                      <div className="text-sm text-slate-500">
                        {new Date(order.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default OrderHistoryPage;

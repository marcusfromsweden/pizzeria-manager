import { createContext, useContext, useMemo, type ReactNode } from 'react';
import { useParams, Outlet, Navigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AuthProvider } from '../features/auth/AuthProvider';
import { CartProvider } from '../features/cart/CartProvider';
import { fetchPizzeriaInfo } from '../api/pizzeria';
import type {
  OpeningHoursResponse,
  PhoneNumberResponse,
  AddressResponse,
} from '../types/api';
import { calculateIsOpen } from '../utils/openingHours';

interface PizzeriaContextValue {
  pizzeriaCode: string;
  pizzeriaName: string | null;
  timezone: string | null;
  address: AddressResponse | null;
  openingHours: OpeningHoursResponse | null;
  phoneNumbers: PhoneNumberResponse[];
  isOpenNow: boolean;
  isLoading: boolean;
}

export const PizzeriaContext = createContext<PizzeriaContextValue | null>(null);

export const usePizzeriaContext = () => {
  const context = useContext(PizzeriaContext);
  if (!context) {
    throw new Error(
      'usePizzeriaContext must be used within a PizzeriaProvider'
    );
  }
  return context;
};

interface PizzeriaProviderProps {
  children?: ReactNode;
}

export const PizzeriaProvider = ({ children }: PizzeriaProviderProps) => {
  const { pizzeriaCode } = useParams<{ pizzeriaCode: string }>();

  const { data: pizzeriaInfo, isLoading } = useQuery({
    queryKey: ['pizzeria', pizzeriaCode],
    queryFn: () => fetchPizzeriaInfo(pizzeriaCode!),
    enabled: !!pizzeriaCode,
    staleTime: Infinity,
  });

  const isOpenNow = useMemo(() => {
    const openingHours = pizzeriaInfo?.data?.openingHours;
    const timezone = pizzeriaInfo?.data?.timezone;
    if (!openingHours || !timezone) {
      return false;
    }
    return calculateIsOpen(openingHours, timezone);
  }, [pizzeriaInfo?.data?.openingHours, pizzeriaInfo?.data?.timezone]);

  if (!pizzeriaCode) {
    return <Navigate to="/not-found" replace />;
  }

  return (
    <PizzeriaContext.Provider
      value={{
        pizzeriaCode,
        pizzeriaName: pizzeriaInfo?.data?.name ?? null,
        timezone: pizzeriaInfo?.data?.timezone ?? null,
        address: pizzeriaInfo?.data?.address ?? null,
        openingHours: pizzeriaInfo?.data?.openingHours ?? null,
        phoneNumbers: pizzeriaInfo?.data?.phoneNumbers ?? [],
        isOpenNow,
        isLoading,
      }}
    >
      <AuthProvider pizzeriaCode={pizzeriaCode}>
        <CartProvider>
          {children ?? <Outlet />}
        </CartProvider>
      </AuthProvider>
    </PizzeriaContext.Provider>
  );
};

export default PizzeriaProvider;

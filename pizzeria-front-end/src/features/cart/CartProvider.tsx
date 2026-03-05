import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react';
import type { CartItem, CartItemCustomisation } from '../../types/api';
import { usePizzeriaContext } from '../../routes/PizzeriaProvider';

interface CartContextType {
  items: CartItem[];
  itemCount: number;
  subtotal: number;
  addItem: (item: Omit<CartItem, 'id'>) => void;
  updateQuantity: (itemId: string, quantity: number) => void;
  removeItem: (itemId: string) => void;
  clearCart: () => void;
}

const CartContext = createContext<CartContextType | null>(null);

const CART_STORAGE_KEY_PREFIX = 'pizzeria-cart-';

export interface CartProviderProps {
  children: ReactNode;
}

export const CartProvider = ({ children }: CartProviderProps) => {
  const { pizzeriaCode } = usePizzeriaContext();
  const storageKey = `${CART_STORAGE_KEY_PREFIX}${pizzeriaCode}`;
  const [items, setItems] = useState<CartItem[]>(() => {
    try {
      const stored = localStorage.getItem(storageKey);
      return stored ? (JSON.parse(stored) as CartItem[]) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem(storageKey, JSON.stringify(items));
    } catch {
      // Storage might be full or unavailable
    }
  }, [items, storageKey]);

  const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);

  const subtotal = items.reduce((sum, item) => {
    const customisationsTotal = item.customisations.reduce((csum, c) => csum + c.price, 0);
    return sum + (item.basePrice + customisationsTotal) * item.quantity;
  }, 0);

  const addItem = useCallback((newItem: Omit<CartItem, 'id'>) => {
    setItems((prevItems) => {
      // Check if item with same menuItemId, size, and customisations exists
      const existingIndex = prevItems.findIndex(
        (item) =>
          item.menuItemId === newItem.menuItemId &&
          item.size === newItem.size &&
          item.specialInstructions === newItem.specialInstructions &&
          areCustomisationsSame(item.customisations, newItem.customisations)
      );

      if (existingIndex >= 0) {
        // Update quantity of existing item
        const updated = [...prevItems];
        updated[existingIndex] = {
          ...updated[existingIndex],
          quantity: updated[existingIndex].quantity + newItem.quantity,
        };
        return updated;
      }

      // Add new item
      return [...prevItems, { ...newItem, id: crypto.randomUUID() }];
    });
  }, []);

  const updateQuantity = useCallback((itemId: string, quantity: number) => {
    if (quantity <= 0) {
      setItems((prev) => prev.filter((item) => item.id !== itemId));
    } else {
      setItems((prev) =>
        prev.map((item) => (item.id === itemId ? { ...item, quantity } : item))
      );
    }
  }, []);

  const removeItem = useCallback((itemId: string) => {
    setItems((prev) => prev.filter((item) => item.id !== itemId));
  }, []);

  const clearCart = useCallback(() => {
    setItems([]);
  }, []);

  return (
    <CartContext.Provider
      value={{
        items,
        itemCount,
        subtotal,
        addItem,
        updateQuantity,
        removeItem,
        clearCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = (): CartContextType => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};

// Helper to compare customisations
function areCustomisationsSame(
  a: CartItemCustomisation[],
  b: CartItemCustomisation[]
): boolean {
  if (a.length !== b.length) return false;
  const aIds = a.map((c) => c.customisationId).sort();
  const bIds = b.map((c) => c.customisationId).sort();
  return aIds.every((id, i) => id === bIds[i]);
}

export default CartProvider;

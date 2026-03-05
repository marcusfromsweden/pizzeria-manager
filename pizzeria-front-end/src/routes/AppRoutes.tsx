import { Routes, Route, Navigate } from 'react-router-dom';
import { PizzeriaProvider } from './PizzeriaProvider';
import { ProtectedRoute } from './ProtectedRoute';
import { Layout } from '../components/layout/Layout';

// Feature pages
import { HomePage } from '../features/home/HomePage';
import { MenuPage } from '../features/menu/MenuPage';
import { PizzaListPage } from '../features/pizzas/PizzaListPage';
import { PizzaDetailPage } from '../features/pizzas/PizzaDetailPage';
import { LoginPage } from '../features/auth/LoginPage';
import { RegisterPage } from '../features/auth/RegisterPage';
import { VerifyEmailPage } from '../features/auth/VerifyEmailPage';
import { ForgotPasswordPage } from '../features/auth/ForgotPasswordPage';
import { ResetPasswordPage } from '../features/auth/ResetPasswordPage';
import { CartPage } from '../features/cart/CartPage';
import { CheckoutPage } from '../features/checkout/CheckoutPage';
import { OrderHistoryPage } from '../features/orders/OrderHistoryPage';
import { OrderDetailPage } from '../features/orders/OrderDetailPage';
import { ProfilePage } from '../features/profile/ProfilePage';
import { PreferencesPage } from '../features/preferences/PreferencesPage';
import { ScoresPage } from '../features/scores/ScoresPage';
import { FeedbackPage } from '../features/feedback/FeedbackPage';
import { AdminPricesPage } from '../features/admin/AdminPricesPage';
import { AdminFeedbackPage } from '../features/admin/AdminFeedbackPage';
import { NotFoundPage } from '../features/error/NotFoundPage';

export const AppRoutes = () => {
  return (
    <Routes>
      {/* Redirect root to a default pizzeria (for development) */}
      <Route path="/" element={<Navigate to="/ramonamalmo" replace />} />

      {/* All routes nested under pizzeriaCode */}
      <Route path="/:pizzeriaCode" element={<PizzeriaProvider />}>
        <Route element={<Layout />}>
          {/* Public routes */}
          <Route index element={<HomePage />} />
          <Route path="menu" element={<MenuPage />} />
          <Route path="pizzas" element={<PizzaListPage />} />
          <Route path="pizzas/:pizzaId" element={<PizzaDetailPage />} />
          <Route path="cart" element={<CartPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="verify-email" element={<VerifyEmailPage />} />
          <Route path="forgot-password" element={<ForgotPasswordPage />} />
          <Route path="reset-password" element={<ResetPasswordPage />} />

          {/* Protected routes */}
          <Route element={<ProtectedRoute />}>
            <Route path="checkout" element={<CheckoutPage />} />
            <Route path="orders" element={<OrderHistoryPage />} />
            <Route path="orders/:orderId" element={<OrderDetailPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="preferences" element={<PreferencesPage />} />
            <Route path="scores" element={<ScoresPage />} />
            <Route path="feedback" element={<FeedbackPage />} />
            <Route path="admin/prices" element={<AdminPricesPage />} />
            <Route path="admin/feedback" element={<AdminFeedbackPage />} />
          </Route>

          {/* 404 within pizzeria context */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>

      {/* Global 404 */}
      <Route path="/not-found" element={<NotFoundPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};

export default AppRoutes;

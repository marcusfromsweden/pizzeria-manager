import { useState, useRef, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../hooks/useAuth';
import { useUnreadFeedbackCount } from '../../hooks/useUnreadFeedbackCount';
import { usePizzeriaContext } from '../../routes/PizzeriaProvider';
import { useCart } from '../../features/cart/CartProvider';
import { useConsoleCaptureSettings } from '../../hooks/useConsoleCapture';
import type { ConsoleLevel } from '../../hooks/useConsoleCapture';
import { Avatar } from '../ui/Avatar';
import { Badge } from '../ui/Badge';
import { Container } from './Container';

const CONSOLE_LEVEL_ITEMS: { level: ConsoleLevel; dotColor: string }[] = [
  { level: 'error', dotColor: 'bg-red-500' },
  { level: 'warn', dotColor: 'bg-yellow-500' },
  { level: 'info', dotColor: 'bg-blue-500' },
  { level: 'debug', dotColor: 'bg-slate-500' },
];

// Available languages with native names
const AVAILABLE_LANGUAGES = [
  { code: 'en', name: 'English' },
  { code: 'sv', name: 'Svenska' },
] as const;

export const Header = () => {
  const { t, i18n } = useTranslation('common');
  const { isAuthenticated, logout, profile } = useAuth();
  const queryClient = useQueryClient();
  const { data: unreadData } = useUnreadFeedbackCount();
  const { pizzeriaCode, pizzeriaName, isOpenNow, phoneNumbers } =
    usePizzeriaContext();
  const { itemCount } = useCart();
  const location = useLocation();
  const { levelSettings, setLevelEnabled, openPanel, hasLogs, errorCount, warnCount } =
    useConsoleCaptureSettings();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isLanguageDropdownOpen, setIsLanguageDropdownOpen] = useState(false);
  const [isAdminDropdownOpen, setIsAdminDropdownOpen] = useState(false);
  const [isConsoleDropdownOpen, setIsConsoleDropdownOpen] = useState(false);
  const languageDropdownRef = useRef<HTMLDivElement>(null);
  const adminDropdownRef = useRef<HTMLDivElement>(null);
  const consoleDropdownRef = useRef<HTMLDivElement>(null);
  const primaryPhone = phoneNumbers[0];
  const isAdmin = profile?.pizzeriaAdmin === pizzeriaCode;
  const unreadCount = unreadData?.unreadCount ?? 0;

  // Refetch unread count when navigating between pages
  useEffect(() => {
    if (isAuthenticated) {
      void queryClient.invalidateQueries({ queryKey: ['unread-feedback-count'] });
    }
  }, [location.pathname, isAuthenticated, queryClient]);

  // Close dropdowns when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        languageDropdownRef.current &&
        !languageDropdownRef.current.contains(event.target as Node)
      ) {
        setIsLanguageDropdownOpen(false);
      }
      if (
        adminDropdownRef.current &&
        !adminDropdownRef.current.contains(event.target as Node)
      ) {
        setIsAdminDropdownOpen(false);
      }
      if (
        consoleDropdownRef.current &&
        !consoleDropdownRef.current.contains(event.target as Node)
      ) {
        setIsConsoleDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const changeLanguage = (langCode: string) => {
    void i18n.changeLanguage(langCode);
    setIsLanguageDropdownOpen(false);
  };

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // Error handled by AuthProvider
    }
  };

  const isActive = (path: string) => {
    return location.pathname === `/${pizzeriaCode}${path}`;
  };

  const navLinkClass = (path: string) =>
    `px-3 py-2 text-sm font-medium transition-colors border-b-2 ${
      isActive(path)
        ? 'border-primary-600 text-slate-900'
        : 'border-transparent text-slate-600 hover:text-slate-900 hover:border-slate-300'
    }`;

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200 bg-white">
      <Container>
        <div className="flex h-16 items-center justify-between">
          {/* Logo and Status */}
          <div className="flex items-center space-x-3">
            <Link
              to={`/${pizzeriaCode}`}
              className="flex items-center space-x-2"
            >
              <span className="text-2xl">🍕</span>
              <span className="text-xl font-bold text-slate-900">
                {pizzeriaName ?? pizzeriaCode}
              </span>
            </Link>
            <Badge variant={isOpenNow ? 'success' : 'danger'} size="sm">
              {isOpenNow ? t('status.open') : t('status.closed')}
            </Badge>
            {primaryPhone && (
              <a
                href={`tel:${primaryPhone.number}`}
                className="hidden text-sm text-slate-600 hover:text-slate-900 lg:block"
              >
                {primaryPhone.number}
              </a>
            )}
          </div>

          {/* Desktop Navigation */}
          <nav className="hidden items-center space-x-1 md:flex">
            <Link to={`/${pizzeriaCode}/menu`} className={navLinkClass('/menu')}>
              {t('nav.menu')}
            </Link>
            <Link
              to={`/${pizzeriaCode}/pizzas`}
              className={navLinkClass('/pizzas')}
            >
              {t('nav.pizzas')}
            </Link>
            {isAuthenticated && (
              <>
                {!isAdmin && (
                  <>
                    <Link
                      to={`/${pizzeriaCode}/orders`}
                      className={navLinkClass('/orders')}
                    >
                      {t('nav.orders')}
                    </Link>
                    <Link
                      to={`/${pizzeriaCode}/scores`}
                      className={navLinkClass('/scores')}
                    >
                      {t('nav.scores')}
                    </Link>
                    <Link
                      to={`/${pizzeriaCode}/feedback`}
                      className={`${navLinkClass('/feedback')} relative flex items-center`}
                    >
                      {t('nav.feedback')}
                      {unreadCount > 0 && (
                        <span className="ml-1.5 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-red-500 px-1.5 text-xs font-medium text-white">
                          {unreadCount > 9 ? '9+' : unreadCount}
                        </span>
                      )}
                    </Link>
                    <Link
                      to={`/${pizzeriaCode}/preferences`}
                      className={navLinkClass('/preferences')}
                    >
                      {t('nav.preferences')}
                    </Link>
                  </>
                )}
                {isAdmin && (
                  <div className="relative" ref={adminDropdownRef}>
                    <button
                      onClick={() => setIsAdminDropdownOpen(!isAdminDropdownOpen)}
                      className={`flex items-center space-x-1 px-3 py-2 text-sm font-medium transition-colors border-b-2 ${
                        location.pathname.includes('/admin/')
                          ? 'border-primary-600 text-slate-900'
                          : 'border-transparent text-slate-600 hover:text-slate-900 hover:border-slate-300'
                      }`}
                    >
                      <span>{t('nav.admin', 'Admin')}</span>
                      <svg
                        className={`h-4 w-4 transition-transform ${isAdminDropdownOpen ? 'rotate-180' : ''}`}
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>
                    {isAdminDropdownOpen && (
                      <div className="absolute left-0 mt-1 w-48 rounded-md border border-slate-200 bg-white py-1 shadow-lg">
                        <Link
                          to={`/${pizzeriaCode}/admin/prices`}
                          onClick={() => setIsAdminDropdownOpen(false)}
                          className={`block px-3 py-2 text-sm hover:bg-slate-100 ${
                            isActive('/admin/prices')
                              ? 'bg-slate-100 text-slate-900 font-medium'
                              : 'text-slate-700'
                          }`}
                        >
                          {t('nav.adminPrices', 'Price Management')}
                        </Link>
                        <Link
                          to={`/${pizzeriaCode}/admin/feedback`}
                          onClick={() => setIsAdminDropdownOpen(false)}
                          className={`block px-3 py-2 text-sm hover:bg-slate-100 ${
                            isActive('/admin/feedback')
                              ? 'bg-slate-100 text-slate-900 font-medium'
                              : 'text-slate-700'
                          }`}
                        >
                          {t('nav.adminFeedback', 'Customer Feedback')}
                        </Link>
                      </div>
                    )}
                  </div>
                )}
              </>
            )}
          </nav>

          {/* Right side actions */}
          <div className="hidden items-center space-x-3 md:flex">
            {/* Cart icon */}
            <Link
              to={`/${pizzeriaCode}/cart`}
              className="relative rounded-md p-2 text-slate-600 hover:bg-slate-100 hover:text-slate-900"
            >
              <svg
                className="h-6 w-6"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2}
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
                />
              </svg>
              {itemCount > 0 && (
                <span className="absolute -right-1 -top-1 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-primary-600 px-1.5 text-xs font-medium text-white">
                  {itemCount > 9 ? '9+' : itemCount}
                </span>
              )}
            </Link>

            {/* Console capture settings dropdown */}
            <div className="relative" ref={consoleDropdownRef}>
              <button
                onClick={() => setIsConsoleDropdownOpen(!isConsoleDropdownOpen)}
                className="relative rounded-md p-2 text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                aria-label={t('console.settings')}
              >
                <svg
                  className="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2}
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
                  />
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                  />
                </svg>
                {(errorCount > 0 || warnCount > 0) && (
                  <span className="absolute -right-1 -top-1 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-medium text-white">
                    {errorCount + warnCount > 9 ? '9+' : errorCount + warnCount}
                  </span>
                )}
              </button>
              {isConsoleDropdownOpen && (
                <div className="absolute right-0 mt-1 w-56 rounded-md border border-slate-200 bg-white py-2 shadow-lg">
                  <div className="mb-1 px-3 text-xs font-semibold text-slate-500">
                    {t('console.captureSettings')}
                  </div>
                  {CONSOLE_LEVEL_ITEMS.map(({ level, dotColor }) => (
                    <label
                      key={level}
                      className="flex cursor-pointer items-center justify-between px-3 py-1.5 text-sm hover:bg-slate-100"
                    >
                      <span className="flex items-center space-x-2">
                        <span className={`inline-block h-2.5 w-2.5 rounded-full ${dotColor}`} />
                        <span className="text-slate-700">{t(`console.level.${level}`)}</span>
                      </span>
                      <input
                        type="checkbox"
                        checked={levelSettings[level]}
                        onChange={(e) => setLevelEnabled(level, e.target.checked)}
                        className="h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
                      />
                    </label>
                  ))}
                  {hasLogs && (
                    <button
                      onClick={() => {
                        openPanel();
                        setIsConsoleDropdownOpen(false);
                      }}
                      className="mt-1 w-full border-t border-slate-200 px-3 pt-2 text-left text-xs font-medium text-primary-600 hover:text-primary-700"
                    >
                      {t('console.viewLogs')}
                    </button>
                  )}
                </div>
              )}
            </div>

            {/* Language selector dropdown */}
            <div className="relative" ref={languageDropdownRef}>
              <button
                onClick={() => setIsLanguageDropdownOpen(!isLanguageDropdownOpen)}
                className="flex items-center space-x-1 rounded-md px-2 py-1 text-sm text-slate-600 hover:bg-slate-100"
              >
                <span>🌐</span>
                <span>{t('language.label')}</span>
                <svg
                  className={`h-4 w-4 transition-transform ${isLanguageDropdownOpen ? 'rotate-180' : ''}`}
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              {isLanguageDropdownOpen && (
                <div className="absolute right-0 mt-1 w-40 rounded-md border border-slate-200 bg-white py-1 shadow-lg">
                  {AVAILABLE_LANGUAGES.map((lang) => (
                    <button
                      key={lang.code}
                      onClick={() => changeLanguage(lang.code)}
                      className={`flex w-full items-center justify-between px-3 py-2 text-sm hover:bg-slate-100 ${
                        i18n.language === lang.code
                          ? 'bg-slate-100 text-slate-900 font-medium'
                          : 'text-slate-700'
                      }`}
                    >
                      <span>{lang.name}</span>
                      <span className="text-slate-400">({lang.code.toUpperCase()})</span>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {isAuthenticated ? (
              <div className="flex items-center space-x-3">
                <Link
                  to={`/${pizzeriaCode}/profile`}
                  className={`${navLinkClass('/profile')} flex items-center space-x-2`}
                >
                  <Avatar
                    src={profile?.profilePhotoBase64}
                    size="sm"
                    alt={profile?.name ?? t('nav.profile')}
                  />
                  <span className="hidden lg:inline">{profile?.name ?? t('nav.profile')}</span>
                </Link>
                <button
                  onClick={() => void handleLogout()}
                  className="px-3 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"
                >
                  {t('nav.signOut')}
                </button>
              </div>
            ) : (
              <div className="flex items-center space-x-3">
                <Link
                  to={`/${pizzeriaCode}/register`}
                  className="px-3 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"
                >
                  {t('nav.register')}
                </Link>
                <Link
                  to={`/${pizzeriaCode}/login`}
                  className="px-3 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"
                >
                  {t('nav.signIn')}
                </Link>
              </div>
            )}
          </div>

          {/* Mobile menu button */}
          <button
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="rounded-md p-2 text-slate-600 hover:bg-slate-100 md:hidden"
          >
            <span className="sr-only">Open menu</span>
            {isMobileMenuOpen ? (
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            )}
          </button>
        </div>

        {/* Mobile Navigation */}
        {isMobileMenuOpen && (
          <div className="border-t border-slate-200 py-3 md:hidden">
            <nav className="flex flex-col space-y-1">
              <Link
                to={`/${pizzeriaCode}/cart`}
                className={`${navLinkClass('/cart')} flex items-center justify-between`}
                onClick={() => setIsMobileMenuOpen(false)}
              >
                <span className="flex items-center space-x-2">
                  <svg
                    className="h-5 w-5"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={2}
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
                    />
                  </svg>
                  <span>{t('nav.cart')}</span>
                </span>
                {itemCount > 0 && (
                  <span className="flex h-5 min-w-[20px] items-center justify-center rounded-full bg-primary-600 px-1.5 text-xs font-medium text-white">
                    {itemCount > 9 ? '9+' : itemCount}
                  </span>
                )}
              </Link>
              <Link
                to={`/${pizzeriaCode}/menu`}
                className={navLinkClass('/menu')}
                onClick={() => setIsMobileMenuOpen(false)}
              >
                {t('nav.menu')}
              </Link>
              <Link
                to={`/${pizzeriaCode}/pizzas`}
                className={navLinkClass('/pizzas')}
                onClick={() => setIsMobileMenuOpen(false)}
              >
                {t('nav.pizzas')}
              </Link>
              {isAuthenticated && (
                <>
                  {!isAdmin && (
                    <>
                      <Link
                        to={`/${pizzeriaCode}/orders`}
                        className={navLinkClass('/orders')}
                        onClick={() => setIsMobileMenuOpen(false)}
                      >
                        {t('nav.orders')}
                      </Link>
                      <Link
                        to={`/${pizzeriaCode}/scores`}
                        className={navLinkClass('/scores')}
                        onClick={() => setIsMobileMenuOpen(false)}
                      >
                        {t('nav.scores')}
                      </Link>
                      <Link
                        to={`/${pizzeriaCode}/feedback`}
                        className={`${navLinkClass('/feedback')} flex items-center justify-between`}
                        onClick={() => setIsMobileMenuOpen(false)}
                      >
                        <span>{t('nav.feedback')}</span>
                        {unreadCount > 0 && (
                          <span className="flex h-5 min-w-[20px] items-center justify-center rounded-full bg-red-500 px-1.5 text-xs font-medium text-white">
                            {unreadCount > 9 ? '9+' : unreadCount}
                          </span>
                        )}
                      </Link>
                      <Link
                        to={`/${pizzeriaCode}/preferences`}
                        className={navLinkClass('/preferences')}
                        onClick={() => setIsMobileMenuOpen(false)}
                      >
                        {t('nav.preferences')}
                      </Link>
                      <Link
                        to={`/${pizzeriaCode}/profile`}
                        className={`${navLinkClass('/profile')} flex items-center space-x-2`}
                        onClick={() => setIsMobileMenuOpen(false)}
                      >
                        <Avatar
                          src={profile?.profilePhotoBase64}
                          size="sm"
                          alt={profile?.name ?? t('nav.profile')}
                        />
                        <span>{t('nav.profile')}</span>
                      </Link>
                    </>
                  )}
                  {isAdmin && (
                    <>
                      <div className="border-t border-slate-200 pt-2 mt-2">
                        <div className="mb-1 text-xs font-medium text-slate-500 px-3">
                          {t('nav.admin', 'Admin')}
                        </div>
                        <Link
                          to={`/${pizzeriaCode}/admin/prices`}
                          className={navLinkClass('/admin/prices')}
                          onClick={() => setIsMobileMenuOpen(false)}
                        >
                          {t('nav.adminPrices', 'Price Management')}
                        </Link>
                        <Link
                          to={`/${pizzeriaCode}/admin/feedback`}
                          className={navLinkClass('/admin/feedback')}
                          onClick={() => setIsMobileMenuOpen(false)}
                        >
                          {t('nav.adminFeedback', 'Customer Feedback')}
                        </Link>
                      </div>
                    </>
                  )}
                </>
              )}
              <div className="border-t border-slate-200 pt-3 mt-2">
                <div className="mb-2 text-xs font-medium text-slate-500 px-3">
                  🌐 {t('language.label')}
                </div>
                <div className="flex flex-col space-y-1">
                  {AVAILABLE_LANGUAGES.map((lang) => (
                    <button
                      key={lang.code}
                      onClick={() => {
                        changeLanguage(lang.code);
                        setIsMobileMenuOpen(false);
                      }}
                      className={`flex items-center justify-between px-3 py-2 text-sm rounded-md ${
                        i18n.language === lang.code
                          ? 'bg-slate-100 text-slate-900 font-medium'
                          : 'text-slate-600 hover:bg-slate-100'
                      }`}
                    >
                      <span>{lang.name}</span>
                      <span className="text-slate-400">({lang.code.toUpperCase()})</span>
                    </button>
                  ))}
                </div>
              </div>
              <div className="border-t border-slate-200 pt-3 mt-2">
                <div className="mb-2 text-xs font-medium text-slate-500 px-3">
                  {t('console.captureSettings')}
                </div>
                <div className="flex flex-col space-y-1">
                  {CONSOLE_LEVEL_ITEMS.map(({ level, dotColor }) => (
                    <label
                      key={level}
                      className="flex cursor-pointer items-center justify-between px-3 py-2 text-sm"
                    >
                      <span className="flex items-center space-x-2">
                        <span className={`inline-block h-2.5 w-2.5 rounded-full ${dotColor}`} />
                        <span className="text-slate-700">{t(`console.level.${level}`)}</span>
                      </span>
                      <input
                        type="checkbox"
                        checked={levelSettings[level]}
                        onChange={(e) => setLevelEnabled(level, e.target.checked)}
                        className="h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
                      />
                    </label>
                  ))}
                  {hasLogs && (
                    <button
                      onClick={() => {
                        openPanel();
                        setIsMobileMenuOpen(false);
                      }}
                      className="px-3 py-1 text-left text-xs font-medium text-primary-600 hover:text-primary-700"
                    >
                      {t('console.viewLogs')}
                    </button>
                  )}
                </div>
              </div>
              <div className="flex items-center justify-end border-t border-slate-200 pt-3 mt-2 space-x-3">
                {isAuthenticated ? (
                  <button
                    onClick={() => void handleLogout()}
                    className="px-3 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"
                  >
                    {t('nav.signOut')}
                  </button>
                ) : (
                  <>
                    <Link
                      to={`/${pizzeriaCode}/register`}
                      className="px-3 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"
                      onClick={() => setIsMobileMenuOpen(false)}
                    >
                      {t('nav.register')}
                    </Link>
                    <Link
                      to={`/${pizzeriaCode}/login`}
                      className="px-3 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"
                      onClick={() => setIsMobileMenuOpen(false)}
                    >
                      {t('nav.signIn')}
                    </Link>
                  </>
                )}
              </div>
            </nav>
          </div>
        )}
      </Container>
    </header>
  );
};

export default Header;

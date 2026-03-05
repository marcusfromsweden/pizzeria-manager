import { Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Header } from './Header';
import { Container } from './Container';
import { OpeningHoursDisplay } from '../ui/OpeningHoursDisplay';
import { usePizzeriaContext } from '../../routes/PizzeriaProvider';

export const Layout = () => {
  const { t } = useTranslation('common');
  const { pizzeriaName, address, openingHours, phoneNumbers, timezone } =
    usePizzeriaContext();

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex-1 py-8">
        <Container>
          <Outlet />
        </Container>
      </main>
      <footer className="border-t border-slate-200 bg-white py-8">
        <Container>
          <div className="grid gap-8 md:grid-cols-3">
            {/* Brand and Address */}
            <div>
              <h3 className="text-lg font-semibold text-slate-900">
                {pizzeriaName}
              </h3>
              {address && (address.street || address.city) && (
                <address className="mt-2 text-sm not-italic text-slate-600">
                  {address.street && <div>{address.street}</div>}
                  {(address.postalCode || address.city) && (
                    <div>
                      {address.postalCode} {address.city}
                    </div>
                  )}
                </address>
              )}
              <p className="mt-2 text-sm text-slate-500">
                &copy; {new Date().getFullYear()} {t('footer.rights')}
              </p>
            </div>

            {/* Opening Hours */}
            {openingHours && timezone && (
              <div>
                <h4 className="font-medium text-slate-900">
                  {t('footer.openingHours')}
                </h4>
                <OpeningHoursDisplay hours={openingHours} timezone={timezone} />
              </div>
            )}

            {/* Contact */}
            {phoneNumbers.length > 0 && (
              <div>
                <h4 className="font-medium text-slate-900">
                  {t('footer.contact')}
                </h4>
                <ul className="mt-2 space-y-1">
                  {phoneNumbers.map((phone, i) => (
                    <li key={i} className="text-sm text-slate-600">
                      <span className="font-medium">{t(phone.label)}:</span>{' '}
                      <a
                        href={`tel:${phone.number}`}
                        className="hover:text-slate-900"
                      >
                        {phone.number}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </Container>
      </footer>
    </div>
  );
};

export default Layout;

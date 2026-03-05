import { useTranslation } from 'react-i18next';

type SpinnerSize = 'sm' | 'md' | 'lg';

interface SpinnerProps {
  size?: SpinnerSize;
  className?: string;
}

const sizeStyles: Record<SpinnerSize, string> = {
  sm: 'h-4 w-4 border-2',
  md: 'h-8 w-8 border-4',
  lg: 'h-12 w-12 border-4',
};

export const Spinner = ({ size = 'md', className = '' }: SpinnerProps) => {
  const { t } = useTranslation('common');

  return (
    <div
      className={`
        animate-spin rounded-full border-primary-600 border-t-transparent
        ${sizeStyles[size]}
        ${className}
      `}
      role="status"
      aria-label={t('status.loading')}
    >
      <span className="sr-only">{t('status.loading')}</span>
    </div>
  );
};

export default Spinner;

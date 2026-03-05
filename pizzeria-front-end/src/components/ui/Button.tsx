import { forwardRef, type ButtonHTMLAttributes } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
}

const variantStyles: Record<ButtonVariant, string> = {
  primary:
    'bg-primary-600 text-white hover:bg-primary-700 focus:ring-primary-500 border-t-primary-400 border-l-primary-400 border-b-primary-800 border-r-primary-800 border-4 shadow-lg active:shadow-inner active:border-t-primary-800 active:border-l-primary-800 active:border-b-primary-400 active:border-r-primary-400',
  secondary:
    'bg-slate-200 text-slate-900 hover:bg-slate-300 focus:ring-slate-500 border-t-white border-l-white border-b-slate-500 border-r-slate-500 border-4 shadow-lg active:shadow-inner active:border-t-slate-500 active:border-l-slate-500 active:border-b-white active:border-r-white',
  danger:
    'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500 border-t-red-300 border-l-red-300 border-b-red-900 border-r-red-900 border-4 shadow-lg active:shadow-inner active:border-t-red-900 active:border-l-red-900 active:border-b-red-300 active:border-r-red-300',
  ghost: 'bg-transparent text-slate-700 hover:bg-slate-100 focus:ring-slate-500',
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-4 py-2 text-sm',
  lg: 'px-3 py-1.5 text-base',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = 'primary',
      size = 'md',
      isLoading = false,
      disabled,
      className = '',
      children,
      ...props
    },
    ref
  ) => {
    return (
      <button
        ref={ref}
        disabled={disabled || isLoading}
        className={`
          inline-flex items-center justify-center rounded-md font-medium
          transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2
          disabled:pointer-events-none disabled:opacity-50
          ${variantStyles[variant]}
          ${sizeStyles[size]}
          ${className}
        `}
        {...props}
      >
        {isLoading && (
          <svg
            className="-ml-1 mr-2 h-4 w-4 animate-spin"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        {children}
      </button>
    );
  }
);

Button.displayName = 'Button';

export default Button;

import type { FC } from 'react';

type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

interface AvatarProps {
  src?: string | null;
  alt?: string;
  size?: AvatarSize;
  className?: string;
}

const sizeStyles: Record<AvatarSize, string> = {
  xs: 'h-6 w-6',
  sm: 'h-8 w-8',
  md: 'h-10 w-10',
  lg: 'h-12 w-12',
  xl: 'h-16 w-16',
};

// Generic silhouette SVG for fallback
const DefaultAvatar: FC<{ className?: string }> = ({ className }) => (
  <svg
    className={className}
    viewBox="0 0 24 24"
    fill="currentColor"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
  </svg>
);

export const Avatar: FC<AvatarProps> = ({
  src,
  alt = 'User avatar',
  size = 'md',
  className = '',
}) => {
  const baseClasses = `rounded-full overflow-hidden flex-shrink-0 ${sizeStyles[size]} ${className}`;

  if (src) {
    return (
      <img
        src={src}
        alt={alt}
        className={`${baseClasses} object-cover`}
      />
    );
  }

  return (
    <div className={`${baseClasses} bg-slate-200 flex items-center justify-center`}>
      <DefaultAvatar className="h-3/4 w-3/4 text-slate-400" />
    </div>
  );
};

export default Avatar;

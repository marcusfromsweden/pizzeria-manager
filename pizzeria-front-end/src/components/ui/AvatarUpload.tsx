import { useRef, useState, type ChangeEvent, type FC } from 'react';
import { useTranslation } from 'react-i18next';
import { Avatar } from './Avatar';
import { Button } from './Button';
import { Alert } from './Alert';
import { AvatarCropModal } from './AvatarCropModal';

interface AvatarUploadProps {
  currentPhoto: string | null;
  onPhotoChange: (base64: string | null) => void;
  isLoading?: boolean;
}

const MAX_FILE_SIZE = 500 * 1024; // 500KB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

export const AvatarUpload: FC<AvatarUploadProps> = ({
  currentPhoto,
  onPhotoChange,
  isLoading = false,
}) => {
  const { t } = useTranslation('auth');
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [cropModalOpen, setCropModalOpen] = useState(false);
  const [originalImage, setOriginalImage] = useState<string | null>(null);

  const handleFileSelect = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setError(null);

    // Validate file type
    if (!ALLOWED_TYPES.includes(file.type)) {
      setError(t('profile.avatar.invalidType'));
      return;
    }

    // Validate file size (allow larger files since we'll crop)
    if (file.size > MAX_FILE_SIZE * 4) {
      setError(t('profile.avatar.fileTooLarge'));
      return;
    }

    // Read file and open crop modal
    const reader = new FileReader();
    reader.onload = (event) => {
      const base64 = event.target?.result as string;
      setOriginalImage(base64);
      setCropModalOpen(true);
    };
    reader.onerror = () => {
      setError(t('profile.avatar.uploadError'));
    };
    reader.readAsDataURL(file);

    // Reset file input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleCropConfirm = (croppedBase64: string) => {
    // Check final size
    if (croppedBase64.length > MAX_FILE_SIZE * 1.4) {
      setError(t('profile.avatar.fileTooLarge'));
      setCropModalOpen(false);
      setOriginalImage(null);
      return;
    }

    onPhotoChange(croppedBase64);
    setCropModalOpen(false);
    setOriginalImage(null);
  };

  const handleCropCancel = () => {
    setCropModalOpen(false);
    setOriginalImage(null);
  };

  const handleRemove = () => {
    onPhotoChange(null);
  };

  return (
    <div className="flex flex-col items-center space-y-4">
      <Avatar src={currentPhoto} size="xl" alt={t('profile.avatar.alt')} />

      {error && (
        <Alert variant="error" className="w-full" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <div className="flex space-x-2">
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          onChange={handleFileSelect}
          className="hidden"
          disabled={isLoading}
        />
        <Button
          type="button"
          variant="secondary"
          size="sm"
          onClick={() => fileInputRef.current?.click()}
          disabled={isLoading}
        >
          {currentPhoto ? t('profile.avatar.change') : t('profile.avatar.upload')}
        </Button>
        {currentPhoto && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={handleRemove}
            disabled={isLoading}
          >
            {t('profile.avatar.remove')}
          </Button>
        )}
      </div>

      <p className="text-xs text-slate-500">{t('profile.avatar.hint')}</p>

      {originalImage && (
        <AvatarCropModal
          imageSrc={originalImage}
          onConfirm={handleCropConfirm}
          onCancel={handleCropCancel}
          isOpen={cropModalOpen}
        />
      )}
    </div>
  );
};

export default AvatarUpload;

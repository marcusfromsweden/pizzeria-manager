import { useState, useCallback, type FC } from 'react';
import Cropper from 'react-easy-crop';
import type { Area, Point } from 'react-easy-crop';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { getCroppedImg } from './avatarCropUtils';

interface AvatarCropModalProps {
  imageSrc: string;
  onConfirm: (croppedBase64: string) => void;
  onCancel: () => void;
  isOpen: boolean;
}

export const AvatarCropModal: FC<AvatarCropModalProps> = ({
  imageSrc,
  onConfirm,
  onCancel,
  isOpen,
}) => {
  const { t } = useTranslation('auth');
  const [crop, setCrop] = useState<Point>({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState<Area | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const onCropComplete = useCallback((_croppedArea: Area, croppedAreaPixels: Area) => {
    setCroppedAreaPixels(croppedAreaPixels);
  }, []);

  const handleConfirm = async () => {
    if (!croppedAreaPixels) return;

    setIsProcessing(true);
    try {
      const croppedImage = await getCroppedImg(imageSrc, croppedAreaPixels);
      onConfirm(croppedImage);
    } catch (error) {
      console.error('Failed to crop image:', error);
    } finally {
      setIsProcessing(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onCancel}
        aria-hidden="true"
      />

      {/* Modal */}
      <div className="relative z-10 w-full max-w-md rounded-lg bg-white p-4 shadow-xl">
        <h2 className="mb-2 text-lg font-semibold text-slate-900">
          {t('profile.avatar.crop.title', 'Adjust photo')}
        </h2>
        <p className="mb-4 text-sm text-slate-500">
          {t('profile.avatar.crop.instructions', 'Drag to reposition, scroll to zoom')}
        </p>

        {/* Cropper container */}
        <div className="relative h-64 w-full overflow-hidden rounded-lg bg-slate-100">
          <Cropper
            image={imageSrc}
            crop={crop}
            zoom={zoom}
            aspect={1}
            cropShape="round"
            showGrid={false}
            onCropChange={setCrop}
            onCropComplete={onCropComplete}
            onZoomChange={setZoom}
          />
        </div>

        {/* Zoom slider */}
        <div className="mt-4">
          <label className="mb-1 block text-sm font-medium text-slate-700">
            {t('profile.avatar.crop.zoom', 'Zoom')}
          </label>
          <input
            type="range"
            min={1}
            max={3}
            step={0.1}
            value={zoom}
            onChange={(e) => setZoom(Number(e.target.value))}
            className="w-full accent-primary-600"
          />
        </div>

        {/* Buttons */}
        <div className="mt-4 flex justify-end space-x-2">
          <Button
            variant="secondary"
            onClick={onCancel}
            disabled={isProcessing}
          >
            {t('profile.avatar.crop.cancel', 'Cancel')}
          </Button>
          <Button
            variant="primary"
            onClick={() => void handleConfirm()}
            isLoading={isProcessing}
            disabled={isProcessing}
          >
            {t('profile.avatar.crop.confirm', 'Apply')}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default AvatarCropModal;

import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../hooks/useAuth';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { updateProfile } from '../../api/auth';
import { getApiErrorMessage } from '../../api/client';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Alert } from '../../components/ui/Alert';
import { Badge } from '../../components/ui/Badge';
import { AvatarUpload } from '../../components/ui/AvatarUpload';

export const ProfilePage = () => {
  const { t } = useTranslation('auth');
  const { t: tCommon } = useTranslation('common');
  const { profile, refreshProfile, deleteAccount } = useAuth();
  const pizzeriaCode = usePizzeriaCode();
  const navigate = useNavigate();

  const [name, setName] = useState(profile?.name ?? '');
  const [phone, setPhone] = useState('');
  const [pendingPhoto, setPendingPhoto] = useState<string | null | undefined>(
    undefined
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Use pending photo if set, otherwise use profile photo
  const currentPhoto =
    pendingPhoto !== undefined ? pendingPhoto : profile?.profilePhotoBase64 ?? null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    setIsSubmitting(true);

    try {
      // Build update payload
      const updatePayload: { name: string; phone?: string; profilePhotoBase64?: string } = {
        name,
        phone: phone || undefined,
      };

      // Include photo change if user modified it
      if (pendingPhoto !== undefined) {
        // Empty string removes the photo, non-empty string sets new photo
        updatePayload.profilePhotoBase64 = pendingPhoto ?? '';
      }

      await updateProfile(updatePayload);
      await refreshProfile();
      setPendingPhoto(undefined); // Reset pending state after successful save
      setSuccess(t('profile.updateSuccess'));
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await deleteAccount();
      navigate(`/${pizzeriaCode}/login`, { replace: true });
    } catch (err) {
      setError(getApiErrorMessage(err));
      setIsDeleting(false);
    }
  };

  if (!profile) {
    return null;
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">
        {t('profile.title')}
      </h1>

      {/* Profile Info */}
      <Card padding="lg">
        {error && (
          <Alert variant="error" className="mb-4" onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert
            variant="success"
            className="mb-4"
            onClose={() => setSuccess(null)}
          >
            {success}
          </Alert>
        )}

        <form onSubmit={(e) => void handleSubmit(e)} className="space-y-6">
          {/* Avatar Upload Section */}
          <div className="border-b border-slate-200 pb-6">
            <AvatarUpload
              currentPhoto={currentPhoto}
              onPhotoChange={setPendingPhoto}
              isLoading={isSubmitting}
            />
          </div>

          <Input
            label={t('profile.name')}
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            maxLength={100}
          />

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">
              {t('profile.email')}
            </label>
            <div className="flex items-center gap-2">
              <span className="text-slate-900">{profile.email}</span>
              <Badge
                variant={profile.emailVerified ? 'success' : 'warning'}
                size="sm"
              >
                {profile.emailVerified
                  ? t('profile.emailVerified')
                  : t('profile.emailNotVerified')}
              </Badge>
            </div>
          </div>

          <Input
            label={t('profile.phone')}
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder={t('profile.phonePlaceholder')}
            maxLength={30}
          />

          <div className="text-sm text-slate-500">
            <p>
              {t('profile.memberSince')}:{' '}
              {new Date(profile.createdAt).toLocaleDateString()}
            </p>
            <p>
              {t('profile.lastUpdated')}:{' '}
              {new Date(profile.updatedAt).toLocaleDateString()}
            </p>
          </div>

          <Button type="submit" isLoading={isSubmitting} disabled={isSubmitting}>
            {isSubmitting ? tCommon('status.saving') : tCommon('actions.save')}
          </Button>
        </form>
      </Card>

      {/* Delete Account */}
      <Card padding="lg">
        <h2 className="text-lg font-semibold text-red-600">
          {t('profile.deleteAccount.title')}
        </h2>
        <p className="mt-2 text-sm text-slate-600">
          {t('profile.deleteAccount.warning')}
        </p>

        {!showDeleteConfirm ? (
          <Button
            variant="danger"
            className="mt-4"
            onClick={() => setShowDeleteConfirm(true)}
          >
            {t('profile.deleteAccount.button')}
          </Button>
        ) : (
          <div className="mt-4 space-y-3">
            <Alert variant="warning">
              {t('profile.deleteAccount.confirm')}
            </Alert>
            <div className="flex gap-3">
              <Button
                variant="danger"
                onClick={() => void handleDelete()}
                isLoading={isDeleting}
                disabled={isDeleting}
              >
                {t('profile.deleteAccount.button')}
              </Button>
              <Button
                variant="secondary"
                onClick={() => setShowDeleteConfirm(false)}
                disabled={isDeleting}
              >
                {tCommon('actions.cancel')}
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
};

export default ProfilePage;

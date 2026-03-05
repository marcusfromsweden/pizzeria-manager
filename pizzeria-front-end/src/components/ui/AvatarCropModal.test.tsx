import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { I18nextProvider } from 'react-i18next';
import i18n from '../../i18n/config';
import { AvatarCropModal } from './AvatarCropModal';

// Mock react-easy-crop
vi.mock('react-easy-crop', () => ({
  default: ({ onCropComplete }: { onCropComplete: (area: unknown, pixels: unknown) => void }) => {
    // Simulate crop complete on mount
    setTimeout(() => {
      onCropComplete(
        { x: 0, y: 0, width: 100, height: 100 },
        { x: 0, y: 0, width: 200, height: 200 }
      );
    }, 0);
    return <div data-testid="cropper">Cropper Mock</div>;
  },
}));

// Mock the crop utility
vi.mock('./avatarCropUtils', () => ({
  getCroppedImg: vi.fn().mockResolvedValue('data:image/jpeg;base64,cropped'),
}));

const renderModal = (props: Partial<Parameters<typeof AvatarCropModal>[0]> = {}) => {
  const defaultProps = {
    imageSrc: 'data:image/jpeg;base64,test',
    onConfirm: vi.fn(),
    onCancel: vi.fn(),
    isOpen: true,
    ...props,
  };

  return {
    ...render(
      <I18nextProvider i18n={i18n}>
        <AvatarCropModal {...defaultProps} />
      </I18nextProvider>
    ),
    props: defaultProps,
  };
};

describe('AvatarCropModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('when closed', () => {
    it('should not render anything when isOpen is false', () => {
      renderModal({ isOpen: false });

      expect(screen.queryByText(/adjust photo/i)).not.toBeInTheDocument();
      expect(screen.queryByTestId('cropper')).not.toBeInTheDocument();
    });
  });

  describe('when open', () => {
    it('should render the modal with title and instructions', () => {
      renderModal();

      expect(screen.getByText(/adjust photo/i)).toBeInTheDocument();
      expect(screen.getByText(/drag to reposition/i)).toBeInTheDocument();
    });

    it('should render the cropper component', () => {
      renderModal();

      expect(screen.getByTestId('cropper')).toBeInTheDocument();
    });

    it('should render zoom slider', () => {
      renderModal();

      const slider = screen.getByRole('slider');
      expect(slider).toBeInTheDocument();
      expect(slider).toHaveAttribute('min', '1');
      expect(slider).toHaveAttribute('max', '3');
    });

    it('should render confirm and cancel buttons', () => {
      renderModal();

      expect(screen.getByRole('button', { name: /apply/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
    });
  });

  describe('interactions', () => {
    it('should call onCancel when cancel button is clicked', () => {
      const { props } = renderModal();

      fireEvent.click(screen.getByRole('button', { name: /cancel/i }));

      expect(props.onCancel).toHaveBeenCalledTimes(1);
    });

    it('should call onCancel when backdrop is clicked', () => {
      const { props } = renderModal();

      // Click the backdrop (the first div with bg-black/50)
      const backdrop = document.querySelector('.bg-black\\/50');
      if (backdrop) {
        fireEvent.click(backdrop);
      }

      expect(props.onCancel).toHaveBeenCalledTimes(1);
    });

    it('should update zoom when slider is changed', () => {
      renderModal();

      const slider = screen.getByRole('slider');
      fireEvent.change(slider, { target: { value: '2' } });

      expect(slider).toHaveValue('2');
    });
  });
});

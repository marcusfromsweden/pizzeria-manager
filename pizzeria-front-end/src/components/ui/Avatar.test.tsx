import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Avatar } from './Avatar';

describe('Avatar', () => {
  describe('with image source', () => {
    it('should render an image when src is provided', () => {
      render(<Avatar src="data:image/jpeg;base64,abc123" alt="Test user" />);

      const img = screen.getByRole('img', { name: 'Test user' });
      expect(img).toBeInTheDocument();
      expect(img).toHaveAttribute('src', 'data:image/jpeg;base64,abc123');
    });

    it('should apply object-cover class for proper image scaling', () => {
      render(<Avatar src="data:image/jpeg;base64,abc123" />);

      const img = screen.getByRole('img');
      expect(img).toHaveClass('object-cover');
    });
  });

  describe('without image source', () => {
    it('should render fallback silhouette when src is null', () => {
      render(<Avatar src={null} />);

      // Should not render an img element
      expect(screen.queryByRole('img')).not.toBeInTheDocument();

      // Should render the fallback SVG container
      const container = document.querySelector('.bg-slate-200');
      expect(container).toBeInTheDocument();
    });

    it('should render fallback silhouette when src is undefined', () => {
      render(<Avatar />);

      expect(screen.queryByRole('img')).not.toBeInTheDocument();

      const container = document.querySelector('.bg-slate-200');
      expect(container).toBeInTheDocument();
    });
  });

  describe('size variants', () => {
    it('should apply xs size classes', () => {
      render(<Avatar src="test.jpg" size="xs" />);

      const img = screen.getByRole('img');
      expect(img).toHaveClass('h-6', 'w-6');
    });

    it('should apply sm size classes', () => {
      render(<Avatar src="test.jpg" size="sm" />);

      const img = screen.getByRole('img');
      expect(img).toHaveClass('h-8', 'w-8');
    });

    it('should apply md size classes by default', () => {
      render(<Avatar src="test.jpg" />);

      const img = screen.getByRole('img');
      expect(img).toHaveClass('h-10', 'w-10');
    });

    it('should apply lg size classes', () => {
      render(<Avatar src="test.jpg" size="lg" />);

      const img = screen.getByRole('img');
      expect(img).toHaveClass('h-12', 'w-12');
    });

    it('should apply xl size classes', () => {
      render(<Avatar src="test.jpg" size="xl" />);

      const img = screen.getByRole('img');
      expect(img).toHaveClass('h-16', 'w-16');
    });
  });

  describe('styling', () => {
    it('should always be circular', () => {
      render(<Avatar src="test.jpg" />);

      const img = screen.getByRole('img');
      expect(img).toHaveClass('rounded-full');
    });

    it('should apply custom className', () => {
      render(<Avatar src="test.jpg" className="custom-class" />);

      const img = screen.getByRole('img');
      expect(img).toHaveClass('custom-class');
    });

    it('should use default alt text when not provided', () => {
      render(<Avatar src="test.jpg" />);

      const img = screen.getByRole('img', { name: 'User avatar' });
      expect(img).toBeInTheDocument();
    });
  });
});

import { describe, it, expect } from 'vitest';
import { renderWithProviders, screen, testI18n } from './test-utils';

describe('Test Setup', () => {
  it('should render with all providers', () => {
    renderWithProviders(<div data-testid="test-element">Test Content</div>);

    expect(screen.getByTestId('test-element')).toBeInTheDocument();
    expect(screen.getByText('Test Content')).toBeInTheDocument();
  });

  it('should have i18n configured', () => {
    expect(testI18n.language).toBe('en');
    expect(testI18n.t('nav.home', { ns: 'common' })).toBe('Home');
  });

  it('should have localStorage mocked', () => {
    localStorage.setItem('test-key', 'test-value');
    expect(localStorage.getItem('test-key')).toBe('test-value');

    localStorage.removeItem('test-key');
    expect(localStorage.getItem('test-key')).toBeNull();
  });
});

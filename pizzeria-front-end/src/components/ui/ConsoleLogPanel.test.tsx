import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '../../tests/test-utils';
import { ConsoleLogPanel } from './ConsoleLogPanel';
import type { ConsoleLine } from '../../hooks/useConsoleCapture';

const makeLine = (
  type: ConsoleLine['type'],
  message: string,
  timestamp = '12:34:56.789'
): ConsoleLine => ({
  type,
  message,
  timestamp,
  args: [message],
});

const defaultProps = {
  open: true,
  logs: [] as ConsoleLine[],
  currentBatchLogs: [] as ConsoleLine[],
  onClose: vi.fn(),
  onClear: vi.fn(),
};

describe('ConsoleLogPanel', () => {
  describe('Rendering', () => {
    it('should not render when open is false', () => {
      renderWithProviders(<ConsoleLogPanel {...defaultProps} open={false} />);

      expect(screen.queryByRole('log')).not.toBeInTheDocument();
    });

    it('should render when open is true', () => {
      renderWithProviders(<ConsoleLogPanel {...defaultProps} />);

      expect(screen.getByRole('log')).toBeInTheDocument();
    });

    it('should show empty state when no logs', () => {
      renderWithProviders(<ConsoleLogPanel {...defaultProps} />);

      expect(screen.getByText('No console logs yet')).toBeInTheDocument();
    });

    it('should render log entries', () => {
      const logs = [makeLine('error', 'Something broke'), makeLine('warn', 'Watch out')];

      renderWithProviders(
        <ConsoleLogPanel {...defaultProps} logs={logs} currentBatchLogs={logs} />
      );

      expect(screen.getByText('Something broke')).toBeInTheDocument();
      expect(screen.getByText('Watch out')).toBeInTheDocument();
    });

    it('should display log count in title', () => {
      const logs = [makeLine('error', 'err1'), makeLine('warn', 'wrn1')];

      renderWithProviders(
        <ConsoleLogPanel {...defaultProps} logs={logs} currentBatchLogs={logs} />
      );

      expect(screen.getByText('(2)')).toBeInTheDocument();
    });
  });

  describe('Color-coding', () => {
    it('should apply red border for error entries', () => {
      const logs = [makeLine('error', 'err msg')];
      renderWithProviders(
        <ConsoleLogPanel {...defaultProps} logs={logs} currentBatchLogs={logs} />
      );

      const entry = screen.getByText('err msg').closest('div');
      expect(entry?.className).toContain('border-l-red-500');
    });

    it('should apply yellow border for warn entries', () => {
      const logs = [makeLine('warn', 'warn msg')];
      renderWithProviders(
        <ConsoleLogPanel {...defaultProps} logs={logs} currentBatchLogs={logs} />
      );

      const entry = screen.getByText('warn msg').closest('div');
      expect(entry?.className).toContain('border-l-yellow-500');
    });

    it('should apply blue border for info entries', () => {
      const logs = [makeLine('info', 'info msg')];
      renderWithProviders(
        <ConsoleLogPanel {...defaultProps} logs={logs} currentBatchLogs={logs} />
      );

      const entry = screen.getByText('info msg').closest('div');
      expect(entry?.className).toContain('border-l-blue-500');
    });

    it('should apply slate border for debug entries', () => {
      const logs = [makeLine('debug', 'debug msg')];
      renderWithProviders(
        <ConsoleLogPanel {...defaultProps} logs={logs} currentBatchLogs={logs} />
      );

      const entry = screen.getByText('debug msg').closest('div');
      expect(entry?.className).toContain('border-l-slate-500');
    });

    it('should apply full opacity for current batch logs', () => {
      const logs = [makeLine('error', 'current batch msg')];
      renderWithProviders(
        <ConsoleLogPanel {...defaultProps} logs={logs} currentBatchLogs={logs} />
      );

      const entry = screen.getByText('current batch msg').closest('div');
      expect(entry?.className).toContain('opacity-100');
    });

    it('should apply reduced opacity for historical logs', () => {
      const logs = [makeLine('error', 'old msg')];
      renderWithProviders(
        <ConsoleLogPanel {...defaultProps} logs={logs} currentBatchLogs={[]} />
      );

      const entry = screen.getByText('old msg').closest('div');
      expect(entry?.className).toContain('opacity-60');
    });
  });

  describe('Actions', () => {
    it('should call onClose when close button is clicked', () => {
      const onClose = vi.fn();
      renderWithProviders(<ConsoleLogPanel {...defaultProps} onClose={onClose} />);

      const closeBtn = screen.getByLabelText('Close');
      closeBtn.click();

      expect(onClose).toHaveBeenCalledOnce();
    });

    it('should call onClear when Clear All button is clicked', () => {
      const onClear = vi.fn();
      renderWithProviders(<ConsoleLogPanel {...defaultProps} onClear={onClear} />);

      screen.getByText('Clear All').click();

      expect(onClear).toHaveBeenCalledOnce();
    });

    it('should write to clipboard when Copy Latest is clicked', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, { clipboard: { writeText } });

      const logs = [makeLine('error', 'test msg', '10:00:00.123')];
      renderWithProviders(
        <ConsoleLogPanel {...defaultProps} logs={logs} currentBatchLogs={logs} />
      );

      screen.getByText('Copy Latest').click();

      await vi.waitFor(() => {
        expect(writeText).toHaveBeenCalledWith(
          'Here is the content of the web client Console log:\n\n[10:00:00.123] ERROR: test msg'
        );
      });
    });

    it('should disable copy button when there are no logs', () => {
      renderWithProviders(<ConsoleLogPanel {...defaultProps} />);

      const copyBtn = screen.getByText('Copy Latest').closest('button');
      expect(copyBtn).toBeDisabled();
    });
  });

  describe('Accessibility', () => {
    it('should have role="log" on the container', () => {
      renderWithProviders(<ConsoleLogPanel {...defaultProps} />);

      expect(screen.getByRole('log')).toBeInTheDocument();
    });

    it('should have aria-label on close button', () => {
      renderWithProviders(<ConsoleLogPanel {...defaultProps} />);

      expect(screen.getByLabelText('Close')).toBeInTheDocument();
    });
  });
});

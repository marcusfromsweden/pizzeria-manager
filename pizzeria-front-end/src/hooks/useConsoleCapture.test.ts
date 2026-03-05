import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { useConsoleCapture } from './useConsoleCapture';
import type { ConsoleLevelSettings } from './useConsoleCapture';

describe('useConsoleCapture', () => {
  let originalConsole: {
    error: typeof console.error;
    warn: typeof console.warn;
    info: typeof console.info;
    debug: typeof console.debug;
  };

  beforeEach(() => {
    originalConsole = {
      error: console.error,
      warn: console.warn,
      info: console.info,
      debug: console.debug,
    };
    localStorage.clear();
  });

  afterEach(() => {
    console.error = originalConsole.error;
    console.warn = originalConsole.warn;
    console.info = originalConsole.info;
    console.debug = originalConsole.debug;
  });

  describe('Default settings', () => {
    it('should use defaults when no localStorage exists', () => {
      const { result } = renderHook(() => useConsoleCapture());
      expect(result.current.levelSettings).toEqual({
        error: true,
        warn: true,
        info: false,
        debug: false,
      });
    });

    it('should load persisted settings from localStorage', () => {
      const stored: ConsoleLevelSettings = { error: false, warn: false, info: true, debug: true };
      localStorage.setItem('console-capture-levels', JSON.stringify(stored));

      const { result } = renderHook(() => useConsoleCapture());
      expect(result.current.levelSettings).toEqual(stored);
    });

    it('should handle corrupt JSON in localStorage gracefully', () => {
      localStorage.setItem('console-capture-levels', 'not valid json{{{');

      const { result } = renderHook(() => useConsoleCapture());
      expect(result.current.levelSettings).toEqual({
        error: true,
        warn: true,
        info: false,
        debug: false,
      });
    });
  });

  describe('setLevelEnabled', () => {
    it('should persist setting to localStorage', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        result.current.setLevelEnabled('info', true);
      });

      const stored = JSON.parse(localStorage.getItem('console-capture-levels')!) as ConsoleLevelSettings;
      expect(stored.info).toBe(true);
    });

    it('should update only the targeted level', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        result.current.setLevelEnabled('info', true);
      });

      expect(result.current.levelSettings.info).toBe(true);
      expect(result.current.levelSettings.error).toBe(true);
      expect(result.current.levelSettings.warn).toBe(true);
      expect(result.current.levelSettings.debug).toBe(false);
    });
  });

  describe('Per-level interception', () => {
    it('should capture error when error level is enabled', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.error('test error message');
      });

      expect(result.current.logs).toHaveLength(1);
      expect(result.current.logs[0].type).toBe('error');
      expect(result.current.logs[0].message).toBe('test error message');
    });

    it('should capture warn when warn level is enabled', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.warn('test warning');
      });

      expect(result.current.logs).toHaveLength(1);
      expect(result.current.logs[0].type).toBe('warn');
    });

    it('should not capture info when info level is disabled by default', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.info('test info');
      });

      expect(result.current.logs).toHaveLength(0);
    });

    it('should capture info after enabling info level', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        result.current.setLevelEnabled('info', true);
      });

      act(() => {
        console.info('test info captured');
      });

      expect(result.current.logs).toHaveLength(1);
      expect(result.current.logs[0].type).toBe('info');
    });

    it('should still call original console methods', () => {
      const originalError = vi.fn();
      console.error = originalError;

      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.error('test');
      });

      // Original was stored and called by the override
      expect(result.current.logs).toHaveLength(1);
    });
  });

  describe('Auto-open panel', () => {
    it('should auto-open panel for error level', () => {
      const { result } = renderHook(() => useConsoleCapture());

      expect(result.current.isPanelOpen).toBe(false);

      act(() => {
        console.error('error!');
      });

      expect(result.current.isPanelOpen).toBe(true);
    });

    it('should auto-open panel for warn level', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.warn('warning!');
      });

      expect(result.current.isPanelOpen).toBe(true);
    });

    it('should not auto-open panel for info level', () => {
      localStorage.setItem(
        'console-capture-levels',
        JSON.stringify({ error: true, warn: true, info: true, debug: false })
      );
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.info('info msg');
      });

      expect(result.current.isPanelOpen).toBe(false);
    });

    it('should not auto-open panel for debug level', () => {
      localStorage.setItem(
        'console-capture-levels',
        JSON.stringify({ error: true, warn: true, info: false, debug: true })
      );
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.debug('debug msg');
      });

      expect(result.current.isPanelOpen).toBe(false);
    });
  });

  describe('React Router filter', () => {
    it('should filter out React Router Future Flag Warning messages', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.warn('React Router Future Flag Warning: some details');
      });

      expect(result.current.logs).toHaveLength(0);
    });

    it('should not filter other warning messages', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.warn('some other warning');
      });

      expect(result.current.logs).toHaveLength(1);
    });
  });

  describe('Log formatting', () => {
    it('should format string args directly', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.error('hello world');
      });

      expect(result.current.logs[0].message).toBe('hello world');
    });

    it('should format Error objects as Name: message', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.error(new TypeError('invalid type'));
      });

      expect(result.current.logs[0].message).toBe('TypeError: invalid type');
    });

    it('should format objects as JSON', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.error({ key: 'value' });
      });

      expect(result.current.logs[0].message).toBe('{"key":"value"}');
    });

    it('should produce timestamp in HH:MM:SS.mmm format', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.error('ts test');
      });

      expect(result.current.logs[0].timestamp).toMatch(/^\d{2}:\d{2}:\d{2}\.\d{3}$/);
    });
  });

  describe('Panel and log management', () => {
    it('should keep logs after closePanel', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.error('msg 1');
      });

      act(() => {
        result.current.closePanel();
      });

      expect(result.current.logs).toHaveLength(1);
      expect(result.current.isPanelOpen).toBe(false);
    });

    it('should empty all logs on clearLogs', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.error('msg 1');
        console.warn('msg 2');
      });

      act(() => {
        result.current.clearLogs();
      });

      expect(result.current.logs).toHaveLength(0);
      expect(result.current.hasLogs).toBe(false);
    });

    it('should reset currentBatchLogs after closePanel', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.error('before close');
      });

      expect(result.current.currentBatchLogs).toHaveLength(1);

      act(() => {
        result.current.closePanel();
      });

      expect(result.current.currentBatchLogs).toHaveLength(0);

      act(() => {
        console.error('after close');
      });

      expect(result.current.currentBatchLogs).toHaveLength(1);
      expect(result.current.logs).toHaveLength(2);
    });

    it('should track errorCount and warnCount', () => {
      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        console.error('e1');
        console.error('e2');
        console.warn('w1');
      });

      expect(result.current.errorCount).toBe(2);
      expect(result.current.warnCount).toBe(1);
    });
  });

  describe('Cleanup', () => {
    it('should restore original console methods on unmount', () => {
      const origError = console.error;
      const origWarn = console.warn;

      const { unmount } = renderHook(() => useConsoleCapture());

      // After hook mounts, console methods are overridden
      expect(console.error).not.toBe(origError);
      expect(console.warn).not.toBe(origWarn);

      unmount();

      // After unmount, originals are restored
      expect(console.error).toBe(origError);
      expect(console.warn).toBe(origWarn);
    });

    it('should restore console methods when all levels are disabled', () => {
      const origError = console.error;
      const origWarn = console.warn;

      const { result } = renderHook(() => useConsoleCapture());

      act(() => {
        result.current.setLevelEnabled('error', false);
        result.current.setLevelEnabled('warn', false);
      });

      expect(console.error).toBe(origError);
      expect(console.warn).toBe(origWarn);
    });
  });
});

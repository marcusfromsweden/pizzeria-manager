import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';

// --- Types ---

export type ConsoleLevel = 'error' | 'warn' | 'info' | 'debug';

export interface ConsoleLine {
  type: ConsoleLevel;
  message: string;
  timestamp: string;
  args: unknown[];
}

export type ConsoleLevelSettings = Record<ConsoleLevel, boolean>;

export interface ConsoleCaptureContextValue {
  levelSettings: ConsoleLevelSettings;
  setLevelEnabled: (level: ConsoleLevel, enabled: boolean) => void;
  openPanel: () => void;
  hasLogs: boolean;
  errorCount: number;
  warnCount: number;
}

// --- Constants ---

const STORAGE_KEY = 'console-capture-levels';
const MAX_LOGS = 500;
const CONSOLE_LEVELS: ConsoleLevel[] = ['error', 'warn', 'info', 'debug'];
const AUTO_OPEN_LEVELS: ConsoleLevel[] = ['error', 'warn'];

const DEFAULT_SETTINGS: ConsoleLevelSettings = {
  error: true,
  warn: true,
  info: false,
  debug: false,
};

// --- Context ---

export const ConsoleCaptureContext = createContext<ConsoleCaptureContextValue | null>(null);

export const useConsoleCaptureSettings = (): ConsoleCaptureContextValue => {
  const context = useContext(ConsoleCaptureContext);
  if (!context) {
    throw new Error('useConsoleCaptureSettings must be used within a ConsoleCaptureContext.Provider');
  }
  return context;
};

// --- Helpers ---

function formatArg(arg: unknown): string {
  if (typeof arg === 'string') return arg;
  if (arg instanceof Error) return `${arg.name}: ${arg.message}`;
  try {
    return JSON.stringify(arg);
  } catch {
    return String(arg);
  }
}

function formatArgs(args: unknown[]): string {
  return args.map(formatArg).join(' ');
}

function formatTimestamp(date: Date): string {
  const h = String(date.getHours()).padStart(2, '0');
  const m = String(date.getMinutes()).padStart(2, '0');
  const s = String(date.getSeconds()).padStart(2, '0');
  const ms = String(date.getMilliseconds()).padStart(3, '0');
  return `${h}:${m}:${s}.${ms}`;
}

function loadSettings(): ConsoleLevelSettings {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored) as Record<string, unknown>;
      const result = { ...DEFAULT_SETTINGS };
      for (const level of CONSOLE_LEVELS) {
        if (typeof parsed[level] === 'boolean') {
          result[level] = parsed[level];
        }
      }
      return result;
    }
  } catch {
    // Corrupt JSON — fall through to defaults
  }
  return { ...DEFAULT_SETTINGS };
}

function saveSettings(settings: ConsoleLevelSettings): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
}

function shouldFilter(args: unknown[]): boolean {
  return args.some(
    (arg) => typeof arg === 'string' && arg.includes('React Router Future Flag Warning')
  );
}

// --- Hook ---

export interface UseConsoleCaptureReturn {
  levelSettings: ConsoleLevelSettings;
  setLevelEnabled: (level: ConsoleLevel, enabled: boolean) => void;
  logs: ConsoleLine[];
  currentBatchLogs: ConsoleLine[];
  isPanelOpen: boolean;
  openPanel: () => void;
  closePanel: () => void;
  clearLogs: () => void;
  hasLogs: boolean;
  errorCount: number;
  warnCount: number;
}

export function useConsoleCapture(): UseConsoleCaptureReturn {
  const [levelSettings, setLevelSettings] = useState<ConsoleLevelSettings>(loadSettings);
  const [logs, setLogs] = useState<ConsoleLine[]>([]);
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [batchStartIndex, setBatchStartIndex] = useState(0);

  // Store original console methods
  const originalsRef = useRef<Partial<Record<ConsoleLevel, (...args: unknown[]) => void>>>({});

  const openPanel = useCallback(() => setIsPanelOpen(true), []);

  const closePanel = useCallback(() => {
    setIsPanelOpen(false);
    // Next logs start a fresh batch
    setLogs((prev) => {
      setBatchStartIndex(prev.length);
      return prev;
    });
  }, []);

  const clearLogs = useCallback(() => {
    setLogs([]);
    setBatchStartIndex(0);
  }, []);

  const setLevelEnabled = useCallback((level: ConsoleLevel, enabled: boolean) => {
    setLevelSettings((prev) => {
      const next = { ...prev, [level]: enabled };
      saveSettings(next);
      return next;
    });
  }, []);

  // Install/remove console overrides based on levelSettings
  useEffect(() => {
    const installedLevels: ConsoleLevel[] = [];

    for (const level of CONSOLE_LEVELS) {
      if (levelSettings[level]) {
        // Save original if not already saved
        if (!originalsRef.current[level]) {
          originalsRef.current[level] = console[level] as (...args: unknown[]) => void;
        }
        const original = originalsRef.current[level];

        console[level] = (...args: unknown[]) => {
          // Always call original first
          original.apply(console, args);

          // Filter unwanted messages
          if (shouldFilter(args)) return;

          const line: ConsoleLine = {
            type: level,
            message: formatArgs(args),
            timestamp: formatTimestamp(new Date()),
            args,
          };

          setLogs((prev) => {
            const next = [...prev, line];
            return next.length > MAX_LOGS ? next.slice(next.length - MAX_LOGS) : next;
          });

          // Auto-open panel for error/warn
          if (AUTO_OPEN_LEVELS.includes(level)) {
            setIsPanelOpen(true);
          }
        };

        installedLevels.push(level);
      } else {
        // Restore original if we had overridden it
        if (originalsRef.current[level]) {
          console[level] = originalsRef.current[level] as typeof console.error;
          delete originalsRef.current[level];
        }
      }
    }

    const currentOriginals = originalsRef.current;
    return () => {
      // Cleanup: restore all overridden methods
      for (const level of installedLevels) {
        if (currentOriginals[level]) {
          console[level] = currentOriginals[level] as typeof console.error;
          delete currentOriginals[level];
        }
      }
    };
  }, [levelSettings]);

  const currentBatchLogs = logs.slice(batchStartIndex);
  const errorCount = logs.filter((l) => l.type === 'error').length;
  const warnCount = logs.filter((l) => l.type === 'warn').length;

  return {
    levelSettings,
    setLevelEnabled,
    logs,
    currentBatchLogs,
    isPanelOpen,
    openPanel,
    closePanel,
    clearLogs,
    hasLogs: logs.length > 0,
    errorCount,
    warnCount,
  };
}

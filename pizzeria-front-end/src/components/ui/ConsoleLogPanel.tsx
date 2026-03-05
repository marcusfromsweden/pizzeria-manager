import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { ConsoleLine } from '../../hooks/useConsoleCapture';
import { Button } from './Button';

const LEVEL_STYLES: Record<string, { border: string; badge: string; badgeText: string }> = {
  error: {
    border: 'border-l-red-500',
    badge: 'bg-red-100 text-red-800',
    badgeText: 'ERR',
  },
  warn: {
    border: 'border-l-yellow-500',
    badge: 'bg-yellow-100 text-yellow-800',
    badgeText: 'WRN',
  },
  info: {
    border: 'border-l-blue-500',
    badge: 'bg-blue-100 text-blue-800',
    badgeText: 'INF',
  },
  debug: {
    border: 'border-l-slate-500',
    badge: 'bg-slate-100 text-slate-800',
    badgeText: 'DBG',
  },
};

interface ConsoleLogPanelProps {
  open: boolean;
  logs: ConsoleLine[];
  currentBatchLogs: ConsoleLine[];
  onClose: () => void;
  onClear: () => void;
}

export const ConsoleLogPanel = ({
  open,
  logs,
  currentBatchLogs,
  onClose,
  onClear,
}: ConsoleLogPanelProps) => {
  const { t } = useTranslation('common');
  const [copied, setCopied] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);
  const copiedTimerRef = useRef<ReturnType<typeof setTimeout>>();

  // Auto-scroll to bottom when new logs arrive
  useEffect(() => {
    if (scrollRef.current && open) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [logs.length, open]);

  // Clean up timer on unmount
  useEffect(() => {
    return () => {
      if (copiedTimerRef.current) clearTimeout(copiedTimerRef.current);
    };
  }, []);

  const handleCopy = async () => {
    if (logs.length === 0) return;

    const text =
      'Here is the content of the web client Console log:\n\n' +
      logs.map((l) => `[${l.timestamp}] ${l.type.toUpperCase()}: ${l.message}`).join('\n');

    await navigator.clipboard.writeText(text);
    setCopied(true);
    if (copiedTimerRef.current) clearTimeout(copiedTimerRef.current);
    copiedTimerRef.current = setTimeout(() => setCopied(false), 2000);
  };

  if (!open) return null;

  const batchStartIndex = logs.length - currentBatchLogs.length;

  return (
    <div
      className="fixed bottom-0 inset-x-0 z-50 flex max-h-[50vh] flex-col border-t border-slate-300 bg-white shadow-lg"
      role="log"
    >
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-4 py-2">
        <h3 className="text-sm font-semibold text-slate-700">
          {t('console.title')}
          {logs.length > 0 && (
            <span className="ml-2 text-xs font-normal text-slate-500">({logs.length})</span>
          )}
        </h3>
        <div className="flex items-center space-x-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => void handleCopy()}
            disabled={logs.length === 0}
          >
            {copied ? t('console.copied') : t('console.copyLatest')}
          </Button>
          <Button variant="ghost" size="sm" onClick={onClear}>
            {t('console.clearAll')}
          </Button>
          <button
            onClick={onClose}
            className="rounded p-1 text-slate-500 hover:bg-slate-200 hover:text-slate-700"
            aria-label={t('actions.close')}
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
      </div>

      {/* Log entries */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto p-2">
        {logs.length === 0 ? (
          <p className="py-8 text-center text-sm text-slate-400">{t('console.noLogs')}</p>
        ) : (
          <div className="space-y-0.5">
            {logs.map((line, index) => {
              const style = LEVEL_STYLES[line.type] ?? LEVEL_STYLES.debug;
              const isCurrentBatch = index >= batchStartIndex;

              return (
                <div
                  key={index}
                  className={`flex items-start border-l-4 px-2 py-1 font-mono text-xs ${style.border} ${
                    isCurrentBatch ? 'opacity-100' : 'opacity-60'
                  }`}
                >
                  <span className="mr-2 whitespace-nowrap text-slate-400">{line.timestamp}</span>
                  <span
                    className={`mr-2 inline-flex items-center rounded px-1 py-0.5 text-[10px] font-semibold ${style.badge}`}
                  >
                    {style.badgeText}
                  </span>
                  <span className="min-w-0 break-all text-slate-700">{line.message}</span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default ConsoleLogPanel;

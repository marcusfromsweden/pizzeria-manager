import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppRoutes } from './routes/AppRoutes';
import { useConsoleCapture, ConsoleCaptureContext } from './hooks/useConsoleCapture';
import { ConsoleLogPanel } from './components/ui/ConsoleLogPanel';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: 1,
    },
  },
});

function App() {
  const consoleCapture = useConsoleCapture();

  return (
    <QueryClientProvider client={queryClient}>
      <ConsoleCaptureContext.Provider
        value={{
          levelSettings: consoleCapture.levelSettings,
          setLevelEnabled: consoleCapture.setLevelEnabled,
          openPanel: consoleCapture.openPanel,
          hasLogs: consoleCapture.hasLogs,
          errorCount: consoleCapture.errorCount,
          warnCount: consoleCapture.warnCount,
        }}
      >
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
        <ConsoleLogPanel
          open={consoleCapture.isPanelOpen}
          logs={consoleCapture.logs}
          currentBatchLogs={consoleCapture.currentBatchLogs}
          onClose={consoleCapture.closePanel}
          onClear={consoleCapture.clearLogs}
        />
      </ConsoleCaptureContext.Provider>
    </QueryClientProvider>
  );
}

export default App;

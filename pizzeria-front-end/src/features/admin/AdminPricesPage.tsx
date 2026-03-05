import { useState, useRef, type ChangeEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { exportPrices, importPrices } from '../../api/admin';
import { getApiErrorMessage } from '../../api/client';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { usePizzeriaCode } from '../../hooks/usePizzeriaCode';
import { useAuth } from '../../hooks/useAuth';
import { useTranslateKey } from '../../hooks/useTranslateKey';
import type { PriceImportResponse, PriceChangeRow } from '../../types/api';

export const AdminPricesPage = () => {
  const { t } = useTranslation('common');
  const { translateKey } = useTranslateKey();
  const pizzeriaCode = usePizzeriaCode();
  const { profile } = useAuth();

  const [error, setError] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [dryRun, setDryRun] = useState(true);
  const [importResult, setImportResult] = useState<PriceImportResponse | null>(
    null
  );
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isAdmin = profile?.pizzeriaAdmin === pizzeriaCode;

  const exportMutation = useMutation({
    mutationFn: () => exportPrices(pizzeriaCode),
    onSuccess: (response) => {
      // Create download link for CSV
      const blob = new Blob([response.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `prices-${pizzeriaCode}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      setError(null);
    },
    onError: (err) => {
      setError(getApiErrorMessage(err));
    },
  });

  const importMutation = useMutation({
    mutationFn: () => {
      if (!selectedFile) throw new Error('No file selected');
      return importPrices(pizzeriaCode, selectedFile, dryRun);
    },
    onSuccess: (response) => {
      setImportResult(response.data);
      setError(null);
      if (!response.data.dryRun) {
        // Clear file after successful real import
        setSelectedFile(null);
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
      }
    },
    onError: (err) => {
      setError(getApiErrorMessage(err));
      setImportResult(null);
    },
  });

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    setSelectedFile(file);
    setImportResult(null);
  };

  const handleImport = () => {
    importMutation.mutate();
  };

  const getStatusBadge = (status: PriceChangeRow['status']) => {
    switch (status) {
      case 'UPDATED':
        return (
          <span className="rounded-full bg-green-100 px-2 py-1 text-xs font-medium text-green-800">
            {t('admin.prices.statusUpdated', 'Updated')}
          </span>
        );
      case 'NO_CHANGE':
        return (
          <span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600">
            {t('admin.prices.statusNoChange', 'No change')}
          </span>
        );
      case 'NOT_FOUND':
        return (
          <span className="rounded-full bg-red-100 px-2 py-1 text-xs font-medium text-red-800">
            {t('admin.prices.statusNotFound', 'Not found')}
          </span>
        );
    }
  };

  if (!isAdmin) {
    return (
      <div className="mx-auto max-w-2xl">
        <Alert variant="error">
          {t('admin.noAccess', 'You do not have admin access to this pizzeria.')}
        </Alert>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Card padding="lg">
        <h1 className="text-2xl font-bold text-slate-900">
          {t('admin.prices.title', 'Price Management')}
        </h1>
        <p className="mt-2 text-slate-600">
          {t(
            'admin.prices.subtitle',
            'Export and import menu item and customization prices.'
          )}
        </p>
      </Card>

      {error && (
        <Alert variant="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Export Section */}
      <Card padding="lg">
        <h2 className="text-lg font-semibold text-slate-900">
          {t('admin.prices.export', 'Export Prices')}
        </h2>
        <p className="mt-1 text-sm text-slate-600">
          {t(
            'admin.prices.exportDescription',
            'Download a CSV file containing all current prices for menu items and customizations.'
          )}
        </p>
        <div className="mt-4">
          <Button
            onClick={() => exportMutation.mutate()}
            disabled={exportMutation.isPending}
            isLoading={exportMutation.isPending}
          >
            {exportMutation.isPending
              ? t('admin.prices.exporting', 'Exporting...')
              : t('admin.prices.downloadCsv', 'Download CSV')}
          </Button>
        </div>
      </Card>

      {/* Import Section */}
      <Card padding="lg">
        <h2 className="text-lg font-semibold text-slate-900">
          {t('admin.prices.import', 'Import Prices')}
        </h2>
        <p className="mt-1 text-sm text-slate-600">
          {t(
            'admin.prices.importDescription',
            'Upload a CSV file to update prices. Use the dry-run option to preview changes before applying.'
          )}
        </p>

        <div className="mt-4 space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">
              {t('admin.prices.selectFile', 'Select CSV File')}
            </label>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv"
              onChange={handleFileChange}
              className="block w-full text-sm text-slate-500 file:mr-4 file:rounded-md file:border-0 file:bg-primary-50 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary-700 hover:file:bg-primary-100"
            />
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="dryRun"
              checked={dryRun}
              onChange={(e) => setDryRun(e.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
            />
            <label htmlFor="dryRun" className="text-sm text-slate-700">
              {t(
                'admin.prices.dryRun',
                'Dry run (preview changes without applying)'
              )}
            </label>
          </div>

          <Button
            onClick={handleImport}
            disabled={!selectedFile || importMutation.isPending}
            isLoading={importMutation.isPending}
            variant={dryRun ? 'secondary' : 'primary'}
          >
            {importMutation.isPending
              ? t('admin.prices.importing', 'Processing...')
              : dryRun
                ? t('admin.prices.previewChanges', 'Preview Changes')
                : t('admin.prices.applyChanges', 'Apply Changes')}
          </Button>
        </div>
      </Card>

      {/* Import Results */}
      {importResult && (
        <Card padding="lg">
          <h2 className="text-lg font-semibold text-slate-900">
            {importResult.dryRun
              ? t('admin.prices.previewResults', 'Preview Results')
              : t('admin.prices.importResults', 'Import Results')}
          </h2>

          {importResult.dryRun && (
            <Alert variant="info" className="mt-2">
              {t(
                'admin.prices.dryRunNote',
                'This is a preview. No changes have been applied yet.'
              )}
            </Alert>
          )}

          <div className="mt-4 grid grid-cols-4 gap-4 text-center">
            <div className="rounded-lg bg-slate-50 p-3">
              <div className="text-2xl font-bold text-slate-900">
                {importResult.totalProcessed}
              </div>
              <div className="text-xs text-slate-600">
                {t('admin.prices.total', 'Total')}
              </div>
            </div>
            <div className="rounded-lg bg-green-50 p-3">
              <div className="text-2xl font-bold text-green-700">
                {importResult.updated}
              </div>
              <div className="text-xs text-green-600">
                {t('admin.prices.updated', 'Updated')}
              </div>
            </div>
            <div className="rounded-lg bg-slate-50 p-3">
              <div className="text-2xl font-bold text-slate-700">
                {importResult.unchanged}
              </div>
              <div className="text-xs text-slate-600">
                {t('admin.prices.unchanged', 'Unchanged')}
              </div>
            </div>
            <div className="rounded-lg bg-red-50 p-3">
              <div className="text-2xl font-bold text-red-700">
                {importResult.errors}
              </div>
              <div className="text-xs text-red-600">
                {t('admin.prices.errors', 'Errors')}
              </div>
            </div>
          </div>

          {importResult.changes.length > 0 && (
            <div className="mt-4 overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-3 py-2 text-left text-xs font-medium uppercase text-slate-500">
                      {t('admin.prices.type', 'Type')}
                    </th>
                    <th className="px-3 py-2 text-left text-xs font-medium uppercase text-slate-500">
                      {t('admin.prices.name', 'Name')}
                    </th>
                    <th className="px-3 py-2 text-right text-xs font-medium uppercase text-slate-500">
                      {t('admin.prices.regularPrice', 'Regular')}
                    </th>
                    <th className="px-3 py-2 text-right text-xs font-medium uppercase text-slate-500">
                      {t('admin.prices.familyPrice', 'Family')}
                    </th>
                    <th className="px-3 py-2 text-center text-xs font-medium uppercase text-slate-500">
                      {t('admin.prices.status', 'Status')}
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 bg-white">
                  {importResult.changes.map((change) => (
                    <tr key={change.id}>
                      <td className="whitespace-nowrap px-3 py-2 text-sm text-slate-600">
                        {change.type === 'MENU_ITEM'
                          ? t('admin.prices.typeMenuItem', 'Menu item')
                          : t('admin.prices.typeExtra', 'Extra')}
                      </td>
                      <td className="px-3 py-2 text-sm text-slate-900">
                        {translateKey(change.nameKey)}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-right text-sm">
                        {change.status === 'UPDATED' ? (
                          <>
                            <span className="text-slate-400 line-through">
                              {change.oldPriceRegular}
                            </span>{' '}
                            <span className="font-medium text-green-700">
                              {change.newPriceRegular}
                            </span>
                          </>
                        ) : (
                          <span className="text-slate-600">
                            {change.newPriceRegular}
                          </span>
                        )}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-right text-sm">
                        {change.status === 'UPDATED' ? (
                          <>
                            <span className="text-slate-400 line-through">
                              {change.oldPriceFamily}
                            </span>{' '}
                            <span className="font-medium text-green-700">
                              {change.newPriceFamily}
                            </span>
                          </>
                        ) : (
                          <span className="text-slate-600">
                            {change.newPriceFamily}
                          </span>
                        )}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-center">
                        {getStatusBadge(change.status)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {importResult.dryRun && importResult.updated > 0 && (
            <div className="mt-4">
              <Button
                onClick={() => {
                  setDryRun(false);
                  importMutation.mutate();
                }}
                disabled={importMutation.isPending}
              >
                {t('admin.prices.confirmApply', 'Apply These Changes')}
              </Button>
            </div>
          )}
        </Card>
      )}
    </div>
  );
};

export default AdminPricesPage;

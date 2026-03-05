import { describe, it, expect, beforeEach, vi } from 'vitest';
import axios, { AxiosError, AxiosHeaders } from 'axios';
import api, { setAuthToken, getAuthToken, getApiErrorMessage } from './client';
import type { ProblemDetail } from '../types/api';

describe('API Client', () => {
  beforeEach(() => {
    // Reset token before each test
    setAuthToken(null);
  });

  describe('setAuthToken / getAuthToken', () => {
    it('should store and retrieve auth token', () => {
      expect(getAuthToken()).toBeNull();

      setAuthToken('test-token-123');
      expect(getAuthToken()).toBe('test-token-123');
    });

    it('should clear token when set to null', () => {
      setAuthToken('test-token');
      expect(getAuthToken()).toBe('test-token');

      setAuthToken(null);
      expect(getAuthToken()).toBeNull();
    });
  });

  describe('Request Interceptor', () => {
    it('should add Authorization header when token is set', async () => {
      setAuthToken('my-auth-token');

      // Create a mock adapter to intercept the request
      const mockAdapter = vi.fn().mockResolvedValue({
        data: {},
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {},
      });

      api.defaults.adapter = mockAdapter;

      await api.get('/test');

      expect(mockAdapter).toHaveBeenCalled();
      const requestConfig = mockAdapter.mock.calls[0][0];
      expect(requestConfig.headers.Authorization).toBe('Bearer my-auth-token');
    });

    it('should not add Authorization header when token is not set', async () => {
      setAuthToken(null);

      const mockAdapter = vi.fn().mockResolvedValue({
        data: {},
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {},
      });

      api.defaults.adapter = mockAdapter;

      await api.get('/test');

      expect(mockAdapter).toHaveBeenCalled();
      const requestConfig = mockAdapter.mock.calls[0][0];
      expect(requestConfig.headers.Authorization).toBeUndefined();
    });
  });

  describe('Response Interceptor - 401 Handling', () => {
    it('should clear token and dispatch event on 401 response', async () => {
      setAuthToken('valid-token');
      expect(getAuthToken()).toBe('valid-token');

      const dispatchEventSpy = vi.spyOn(window, 'dispatchEvent');

      const mockAdapter = vi.fn().mockRejectedValue(
        new AxiosError('Unauthorized', '401', undefined, undefined, {
          status: 401,
          statusText: 'Unauthorized',
          data: { detail: 'Token expired' },
          headers: new AxiosHeaders(),
          config: { headers: new AxiosHeaders() },
        })
      );

      api.defaults.adapter = mockAdapter;

      await expect(api.get('/protected')).rejects.toThrow();

      // Token should be cleared
      expect(getAuthToken()).toBeNull();

      // Event should be dispatched
      expect(dispatchEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({ type: 'auth:unauthorized' })
      );

      dispatchEventSpy.mockRestore();
    });

    it('should not clear token on non-401 errors', async () => {
      setAuthToken('valid-token');

      const dispatchEventSpy = vi.spyOn(window, 'dispatchEvent');

      const mockAdapter = vi.fn().mockRejectedValue(
        new AxiosError('Not Found', '404', undefined, undefined, {
          status: 404,
          statusText: 'Not Found',
          data: { detail: 'Resource not found' },
          headers: new AxiosHeaders(),
          config: { headers: new AxiosHeaders() },
        })
      );

      api.defaults.adapter = mockAdapter;

      await expect(api.get('/missing')).rejects.toThrow();

      // Token should NOT be cleared
      expect(getAuthToken()).toBe('valid-token');

      // Event should NOT be dispatched
      expect(dispatchEventSpy).not.toHaveBeenCalled();

      dispatchEventSpy.mockRestore();
    });
  });

  describe('getApiErrorMessage', () => {
    it('should extract detail from ProblemDetail response', () => {
      const error = new AxiosError('Request failed', '400', undefined, undefined, {
        status: 400,
        statusText: 'Bad Request',
        data: {
          type: 'about:blank',
          title: 'Bad Request',
          status: 400,
          detail: 'Email already exists',
          errorCode: 'INVALID_ARGUMENT',
          timestamp: '2024-01-15T12:00:00Z',
        } as ProblemDetail,
        headers: new AxiosHeaders(),
        config: { headers: new AxiosHeaders() },
      });

      expect(getApiErrorMessage(error)).toBe('Email already exists');
    });

    it('should extract message from response data if no detail', () => {
      const error = new AxiosError('Request failed', '400', undefined, undefined, {
        status: 400,
        statusText: 'Bad Request',
        data: {
          message: 'Validation failed',
        },
        headers: new AxiosHeaders(),
        config: { headers: new AxiosHeaders() },
      });

      expect(getApiErrorMessage(error)).toBe('Validation failed');
    });

    it('should fall back to error.message if no response data', () => {
      const error = new AxiosError('Network Error');
      expect(getApiErrorMessage(error)).toBe('Network Error');
    });

    it('should handle standard Error objects', () => {
      const error = new Error('Something went wrong');
      expect(getApiErrorMessage(error)).toBe('Something went wrong');
    });

    it('should return default message for unknown error types', () => {
      expect(getApiErrorMessage('string error')).toBe('An unexpected error occurred');
      expect(getApiErrorMessage(null)).toBe('An unexpected error occurred');
      expect(getApiErrorMessage(undefined)).toBe('An unexpected error occurred');
      expect(getApiErrorMessage(42)).toBe('An unexpected error occurred');
    });
  });

  describe('API Instance Configuration', () => {
    it('should have correct default headers', () => {
      expect(api.defaults.headers['Content-Type']).toBe('application/json');
      expect(api.defaults.headers['Accept']).toBe('application/json');
    });

    it('should have correct base URL', () => {
      // Default is /api/v1 when VITE_API_BASE_URL is not set
      expect(api.defaults.baseURL).toBe('/api/v1');
    });
  });
});

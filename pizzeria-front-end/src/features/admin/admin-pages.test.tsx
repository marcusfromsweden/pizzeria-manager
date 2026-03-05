import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { AdminFeedbackPage } from './AdminFeedbackPage';
import {
  renderWithRoute,
  createAuthenticatedMockAuthContext,
  testI18n,
} from '../../tests/test-utils';
import * as adminApi from '../../api/admin';
import type { FeedbackResponse } from '../../types/api';

// Mock admin API
vi.mock('../../api/admin', () => ({
  fetchAdminFeedback: vi.fn(),
  exportPrices: vi.fn(),
  importPrices: vi.fn(),
}));

// Mock usePizzeriaCode to return our test pizzeria
vi.mock('../../hooks/usePizzeriaCode', () => ({
  usePizzeriaCode: () => 'testpizzeria',
}));

const mockAdminApi = vi.mocked(adminApi);

const mockFeedbackList: FeedbackResponse[] = [
  {
    id: 'feedback-1',
    userId: 'user-a',
    type: 'SERVICE',
    message: 'Great service!',
    rating: 5,
    category: 'Delivery',
    adminReply: null,
    adminRepliedAt: null,
    adminReplyReadAt: null,
    createdAt: '2024-01-15T12:00:00Z',
  },
  {
    id: 'feedback-2',
    userId: 'user-b',
    type: 'SERVICE',
    message: 'Good food',
    rating: 4,
    category: null,
    adminReply: 'Thank you for your feedback!',
    adminRepliedAt: '2024-01-15T14:00:00Z',
    adminReplyReadAt: '2024-01-15T15:00:00Z',
    createdAt: '2024-01-14T10:00:00Z',
  },
];

describe('AdminFeedbackPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should display feedback list when admin is authenticated', async () => {
    mockAdminApi.fetchAdminFeedback.mockResolvedValue({
      data: mockFeedbackList,
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {} as never,
    });

    const authContext = createAuthenticatedMockAuthContext({
      profile: {
        id: 'user-1',
        name: 'Admin User',
        email: 'admin@example.com',
        emailVerified: true,
        preferredDiet: 'NONE',
        preferredIngredientIds: [],
        pizzeriaAdmin: 'testpizzeria',
        profilePhotoBase64: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
      },
    });

    renderWithRoute(<AdminFeedbackPage />, {
      path: '/:pizzeriaCode/admin/feedback',
      route: '/testpizzeria/admin/feedback',
      providerOptions: { authContext, pizzeriaCode: 'testpizzeria' },
    });

    await waitFor(() => {
      expect(screen.getByText('Great service!')).toBeInTheDocument();
    });

    expect(screen.getByText('Good food')).toBeInTheDocument();
    expect(screen.getByText('Delivery')).toBeInTheDocument();
  });

  it('should show access denied message for non-admin users', async () => {
    const authContext = createAuthenticatedMockAuthContext({
      profile: {
        id: 'user-2',
        name: 'Regular User',
        email: 'user@example.com',
        emailVerified: true,
        preferredDiet: 'NONE',
        preferredIngredientIds: [],
        pizzeriaAdmin: null,
        profilePhotoBase64: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
      },
    });

    renderWithRoute(<AdminFeedbackPage />, {
      path: '/:pizzeriaCode/admin/feedback',
      route: '/testpizzeria/admin/feedback',
      providerOptions: { authContext, pizzeriaCode: 'testpizzeria' },
    });

    await waitFor(() => {
      expect(
        screen.getByText(/do not have admin access/i)
      ).toBeInTheDocument();
    });

    expect(mockAdminApi.fetchAdminFeedback).not.toHaveBeenCalled();
  });

  it('should show no feedback message when list is empty', async () => {
    mockAdminApi.fetchAdminFeedback.mockResolvedValue({
      data: [],
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {} as never,
    });

    const authContext = createAuthenticatedMockAuthContext({
      profile: {
        id: 'user-1',
        name: 'Admin User',
        email: 'admin@example.com',
        emailVerified: true,
        preferredDiet: 'NONE',
        preferredIngredientIds: [],
        pizzeriaAdmin: 'testpizzeria',
        profilePhotoBase64: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
      },
    });

    renderWithRoute(<AdminFeedbackPage />, {
      path: '/:pizzeriaCode/admin/feedback',
      route: '/testpizzeria/admin/feedback',
      providerOptions: { authContext, pizzeriaCode: 'testpizzeria' },
    });

    await waitFor(() => {
      expect(screen.getByText(/no customer feedback/i)).toBeInTheDocument();
    });
  });

  it('should display page title', async () => {
    mockAdminApi.fetchAdminFeedback.mockResolvedValue({
      data: [],
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {} as never,
    });

    const authContext = createAuthenticatedMockAuthContext({
      profile: {
        id: 'user-1',
        name: 'Admin User',
        email: 'admin@example.com',
        emailVerified: true,
        preferredDiet: 'NONE',
        preferredIngredientIds: [],
        pizzeriaAdmin: 'testpizzeria',
        profilePhotoBase64: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
      },
    });

    renderWithRoute(<AdminFeedbackPage />, {
      path: '/:pizzeriaCode/admin/feedback',
      route: '/testpizzeria/admin/feedback',
      providerOptions: { authContext, pizzeriaCode: 'testpizzeria' },
    });

    await waitFor(() => {
      expect(screen.getByText(/customer feedback/i)).toBeInTheDocument();
    });
  });

  it('should display rating stars for feedback with rating', async () => {
    mockAdminApi.fetchAdminFeedback.mockResolvedValue({
      data: mockFeedbackList,
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {} as never,
    });

    const authContext = createAuthenticatedMockAuthContext({
      profile: {
        id: 'user-1',
        name: 'Admin User',
        email: 'admin@example.com',
        emailVerified: true,
        preferredDiet: 'NONE',
        preferredIngredientIds: [],
        pizzeriaAdmin: 'testpizzeria',
        profilePhotoBase64: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
      },
    });

    renderWithRoute(<AdminFeedbackPage />, {
      path: '/:pizzeriaCode/admin/feedback',
      route: '/testpizzeria/admin/feedback',
      providerOptions: { authContext, pizzeriaCode: 'testpizzeria' },
    });

    await waitFor(() => {
      expect(screen.getByText('Great service!')).toBeInTheDocument();
    });

    // Check that star characters are rendered (rating display)
    const stars = screen.getAllByText('★');
    expect(stars.length).toBeGreaterThan(0);
  });

  it('should show dash for feedback without category', async () => {
    mockAdminApi.fetchAdminFeedback.mockResolvedValue({
      data: [mockFeedbackList[1]], // Only the one without category
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {} as never,
    });

    const authContext = createAuthenticatedMockAuthContext({
      profile: {
        id: 'user-1',
        name: 'Admin User',
        email: 'admin@example.com',
        emailVerified: true,
        preferredDiet: 'NONE',
        preferredIngredientIds: [],
        pizzeriaAdmin: 'testpizzeria',
        profilePhotoBase64: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
      },
    });

    renderWithRoute(<AdminFeedbackPage />, {
      path: '/:pizzeriaCode/admin/feedback',
      route: '/testpizzeria/admin/feedback',
      providerOptions: { authContext, pizzeriaCode: 'testpizzeria' },
    });

    await waitFor(() => {
      expect(screen.getByText('Good food')).toBeInTheDocument();
    });

    // The category column should show a dash for null category
    const dashes = screen.getAllByText('-');
    expect(dashes.length).toBeGreaterThan(0);
  });
});

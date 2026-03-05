// Enums
export type DietType = 'VEGAN' | 'VEGETARIAN' | 'CARNIVORE' | 'NONE';
export type PizzaType = 'TEMPLATE' | 'CUSTOM';
export type ErrorCode =
  | 'INVALID_ARGUMENT'
  | 'RESOURCE_NOT_FOUND'
  | 'UNAUTHORIZED'
  | 'DOWNSTREAM_ERROR'
  | 'INTERNAL_ERROR';

// Request DTOs
export interface UserRegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface UserLoginRequest {
  email: string;
  password: string;
}

export interface UserVerifyEmailRequest {
  token: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ForgotPasswordResponse {
  resetToken: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface UserProfileUpdateRequest {
  name?: string;
  phone?: string;
  profilePhotoBase64?: string;
}

export interface DietPreferenceUpdateRequest {
  diet: DietType;
}

export interface PreferredIngredientRequest {
  ingredientId: string;
}

export interface PizzaSuitabilityRequest {
  pizzaId: string;
  additionalIngredientIds?: string[] | null;
  removedIngredientIds?: string[] | null;
}

export interface PizzaScoreCreateRequest {
  pizzaId: string;
  pizzaType: PizzaType;
  score: number;
  comment?: string | null;
}

export interface ServiceFeedbackRequest {
  message: string;
  rating?: number | null;
  category?: string | null;
}

// Response DTOs
export interface UserRegisterResponse {
  userId: string;
  emailVerified: boolean;
  verificationToken: string;
}

export interface UserLoginResponse {
  accessToken: string;
}

export interface UserProfileResponse {
  id: string;
  name: string;
  email: string;
  emailVerified: boolean;
  preferredDiet: DietType;
  preferredIngredientIds: string[];
  pizzeriaAdmin: string | null;
  profilePhotoBase64: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DietPreferenceResponse {
  diet: DietType;
}

export interface IngredientIdResponse {
  ingredientId: string;
}

export interface MenuResponse {
  sections: MenuSectionResponse[];
  pizzaCustomisations: PizzaCustomisationResponse[];
}

export interface MenuSectionResponse {
  id: string;
  code: string;
  translationKey: string;
  sortOrder: number;
  items: MenuItemResponse[];
}

export interface MenuItemResponse {
  id: string;
  sectionId: string;
  dishNumber: number;
  nameKey: string;
  descriptionKey: string;
  priceInSek: string;
  familySizePriceInSek: string | null;
  ingredients: MenuIngredientResponse[];
  overallDietaryType: string;
  sortOrder: number;
}

export interface MenuIngredientResponse {
  id: string;
  ingredientKey: string;
  dietaryType: string;
  allergenTags: string[];
  spiceLevel: number;
}

export interface PizzaCustomisationResponse {
  id: string;
  nameKey: string;
  priceInSek: string;
  familySizePriceInSek: string | null;
  sortOrder: number;
}

export interface PizzaSummaryResponse {
  id: string;
  dishNumber: number;
  nameKey: string;
  priceInSek: string;
  familySizePriceInSek: string | null;
  overallDietaryType: string;
  sortOrder: number;
}

export interface PizzaDetailResponse {
  id: string;
  dishNumber: number;
  nameKey: string;
  descriptionKey: string;
  priceInSek: string;
  familySizePriceInSek: string | null;
  ingredients: MenuIngredientResponse[];
  overallDietaryType: string;
  sortOrder: number;
}

export interface PizzaSuitabilityResponse {
  suitable: boolean;
  violations: string[];
  suggestions: string[];
}

export interface PizzaScoreResponse {
  id: string;
  userId: string;
  pizzaId: string;
  pizzaType: PizzaType;
  score: number;
  comment: string | null;
  createdAt: string;
}

export interface FeedbackResponse {
  id: string;
  userId: string;
  type: string;
  message: string;
  rating: number | null;
  category: string | null;
  adminReply: string | null;
  adminRepliedAt: string | null;
  adminReplyReadAt: string | null;
  createdAt: string;
}

export interface UnreadFeedbackCountResponse {
  unreadCount: number;
}

export interface AdminReplyRequest {
  reply: string;
}

// Error Response (RFC 7807)
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string | null;
  errorCode: ErrorCode;
  timestamp: string;
  message?: string;
}

// Pizzeria Info
export interface DayHoursResponse {
  open: string;
  close: string;
}

export interface OpeningHoursResponse {
  monday: DayHoursResponse[];
  tuesday: DayHoursResponse[];
  wednesday: DayHoursResponse[];
  thursday: DayHoursResponse[];
  friday: DayHoursResponse[];
  saturday: DayHoursResponse[];
  sunday: DayHoursResponse[];
}

export interface AddressResponse {
  street: string | null;
  postalCode: string | null;
  city: string | null;
}

export interface PhoneNumberResponse {
  label: string;
  number: string;
}

export interface PizzeriaInfoResponse {
  code: string;
  name: string;
  currency: string;
  timezone: string;
  address: AddressResponse | null;
  openingHours: OpeningHoursResponse | null;
  phoneNumbers: PhoneNumberResponse[];
}

// Admin Price Management
export interface PriceChangeRow {
  type: 'MENU_ITEM' | 'CUSTOMISATION';
  id: string;
  nameKey: string;
  oldPriceRegular: string | null;
  newPriceRegular: string;
  oldPriceFamily: string | null;
  newPriceFamily: string;
  status: 'UPDATED' | 'NO_CHANGE' | 'NOT_FOUND';
}

export interface PriceImportResponse {
  dryRun: boolean;
  totalProcessed: number;
  updated: number;
  unchanged: number;
  errors: number;
  changes: PriceChangeRow[];
}

// Order Enums
export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'PICKED_UP'
  | 'CANCELLED';

export type FulfillmentType = 'PICKUP' | 'DELIVERY';
export type PizzaSize = 'REGULAR' | 'FAMILY';

// Cart Types (local state, not API)
export interface CartItem {
  id: string;
  menuItemId: string;
  menuItemNameKey: string;
  size: PizzaSize;
  quantity: number;
  basePrice: number;
  customisations: CartItemCustomisation[];
  specialInstructions?: string;
}

export interface CartItemCustomisation {
  customisationId: string;
  customisationNameKey: string;
  price: number;
}

// Order Request DTOs
export interface CreateOrderRequest {
  fulfillmentType: FulfillmentType;
  deliveryAddressId?: string;
  deliveryStreet?: string;
  deliveryPostalCode?: string;
  deliveryCity?: string;
  deliveryPhone?: string;
  deliveryInstructions?: string;
  requestedTime?: string;
  customerNotes?: string;
  items: OrderItemRequest[];
}

export interface OrderItemRequest {
  menuItemId: string;
  size: PizzaSize;
  quantity: number;
  customisationIds?: string[];
  specialInstructions?: string;
}

// Order Response DTOs
export interface OrderResponse {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  fulfillmentType: FulfillmentType;
  deliveryStreet: string | null;
  deliveryPostalCode: string | null;
  deliveryCity: string | null;
  deliveryPhone: string | null;
  deliveryInstructions: string | null;
  requestedTime: string | null;
  estimatedReadyTime: string | null;
  subtotal: string;
  deliveryFee: string;
  total: string;
  customerNotes: string | null;
  items: OrderItemResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderItemResponse {
  id: string;
  menuItemId: string;
  menuItemNameKey: string;
  size: PizzaSize;
  quantity: number;
  basePrice: string;
  customisationsPrice: string;
  itemTotal: string;
  specialInstructions: string | null;
  customisations: OrderItemCustomisationResponse[];
}

export interface OrderItemCustomisationResponse {
  id: string;
  customisationId: string;
  customisationNameKey: string;
  price: string;
}

export interface OrderSummaryResponse {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  fulfillmentType: FulfillmentType;
  total: string;
  itemCount: number;
  createdAt: string;
}

// Delivery Address DTOs
export interface SaveDeliveryAddressRequest {
  label?: string;
  street: string;
  postalCode: string;
  city: string;
  phone?: string;
  instructions?: string;
  isDefault: boolean;
}

export interface DeliveryAddressResponse {
  id: string;
  label: string | null;
  street: string;
  postalCode: string;
  city: string;
  phone: string | null;
  instructions: string | null;
  isDefault: boolean;
  createdAt: string;
}

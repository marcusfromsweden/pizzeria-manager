import type { OpeningHoursResponse, DayHoursResponse } from '../types/api';

type DayName =
  | 'sunday'
  | 'monday'
  | 'tuesday'
  | 'wednesday'
  | 'thursday'
  | 'friday'
  | 'saturday';

const DAY_INDEX_TO_NAME: DayName[] = [
  'sunday',
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday',
  'saturday',
];

/**
 * Calculates if the pizzeria is currently open based on opening hours and timezone.
 * Supports multiple time slots per day (e.g., lunch and dinner hours).
 */
export function calculateIsOpen(
  openingHours: OpeningHoursResponse,
  timezone: string
): boolean {
  const now = new Date();

  // Get current time in pizzeria's timezone
  const options: Intl.DateTimeFormatOptions = {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  };

  const timeFormatter = new Intl.DateTimeFormat('en-US', options);
  const timeParts = timeFormatter.formatToParts(now);

  const hourPart = timeParts.find((p) => p.type === 'hour');
  const minutePart = timeParts.find((p) => p.type === 'minute');

  if (!hourPart || !minutePart) return false;

  // Get day of week in timezone
  const dayFormatter = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone,
    weekday: 'long',
  });
  const dayName = dayFormatter.format(now).toLowerCase() as DayName;

  const currentMinutes = parseInt(hourPart.value) * 60 + parseInt(minutePart.value);

  const daySlots = openingHours[dayName];
  if (!daySlots || daySlots.length === 0) return false;

  // Check if current time falls within any of the time slots
  return daySlots.some((slot) => {
    const openMinutes = parseTime(slot.open);
    const closeMinutes = parseTime(slot.close);
    return currentMinutes >= openMinutes && currentMinutes < closeMinutes;
  });
}

/**
 * Gets today's hours in the pizzeria's timezone.
 * Returns an array of time slots for the day.
 */
export function getTodayHours(
  openingHours: OpeningHoursResponse,
  timezone: string
): DayHoursResponse[] {
  const now = new Date();

  const dayFormatter = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone,
    weekday: 'long',
  });
  const dayName = dayFormatter.format(now).toLowerCase() as DayName;

  return openingHours[dayName] || [];
}

/**
 * Gets the current day name in the pizzeria's timezone.
 */
export function getCurrentDayName(timezone: string): DayName {
  const now = new Date();
  const dayFormatter = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone,
    weekday: 'long',
  });
  return dayFormatter.format(now).toLowerCase() as DayName;
}

/**
 * Gets the day name from Date's getDay() index (0 = Sunday).
 */
export function getDayNameFromIndex(index: number): DayName {
  return DAY_INDEX_TO_NAME[index];
}

/**
 * Parses a time string (HH:MM) to minutes since midnight.
 */
function parseTime(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

/**
 * All day names in week order starting from Monday.
 */
export const ORDERED_DAYS: DayName[] = [
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday',
  'saturday',
  'sunday',
];

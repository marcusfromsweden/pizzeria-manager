import { useTranslation } from 'react-i18next';
import type { OpeningHoursResponse, DayHoursResponse } from '../../types/api';
import { ORDERED_DAYS, getCurrentDayName } from '../../utils/openingHours';

interface OpeningHoursDisplayProps {
  hours: OpeningHoursResponse;
  timezone: string;
  compact?: boolean;
}

/**
 * Formats an array of time slots as a display string.
 * E.g., [{open: "11:00", close: "14:00"}, {open: "16:30", close: "21:00"}]
 * becomes "11:00 - 14:00, 16:30 - 21:00"
 */
function formatTimeSlots(slots: DayHoursResponse[]): string {
  if (!slots || slots.length === 0) return '';
  return slots.map((slot) => `${slot.open} - ${slot.close}`).join(', ');
}

export const OpeningHoursDisplay = ({
  hours,
  timezone,
  compact = false,
}: OpeningHoursDisplayProps) => {
  const { t } = useTranslation('common');
  const currentDay = getCurrentDayName(timezone);

  if (compact) {
    const todaySlots = hours[currentDay];
    const hasSlots = todaySlots && todaySlots.length > 0;
    return (
      <span className="text-sm text-slate-600">
        {hasSlots ? formatTimeSlots(todaySlots) : t('status.closed')}
      </span>
    );
  }

  return (
    <ul className="mt-2 space-y-1 text-sm text-slate-600">
      {ORDERED_DAYS.map((day) => {
        const daySlots = hours[day];
        const hasSlots = daySlots && daySlots.length > 0;
        const isToday = day === currentDay;
        return (
          <li
            key={day}
            className={`flex justify-between gap-4 ${isToday ? 'font-medium text-slate-900' : ''}`}
          >
            <span>{t(`days.${day}`)}</span>
            <span className="text-right">
              {hasSlots ? formatTimeSlots(daySlots) : t('status.closed')}
            </span>
          </li>
        );
      })}
    </ul>
  );
};

export default OpeningHoursDisplay;

import type { BadgeVariant } from '../components/ui/Badge';

/**
 * Maps a dietary type to the appropriate Badge variant for consistent styling.
 * - VEGAN: Green (success)
 * - VEGETARIAN: Blue (info)
 * - CARNIVORE: Yellow (warning)
 * - Other: Slate (default)
 */
export const getDietaryBadgeVariant = (dietaryType: string): BadgeVariant => {
  switch (dietaryType.toUpperCase()) {
    case 'VEGAN':
      return 'success';
    case 'VEGETARIAN':
      return 'info';
    case 'CARNIVORE':
      return 'warning';
    default:
      return 'default';
  }
};

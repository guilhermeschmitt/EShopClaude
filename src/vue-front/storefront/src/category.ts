/** Visual identity of a product category: its Material Symbols icon and its colour-block key (see tokens.css). */
export interface CategoryLook {
  icon: string;
  key: 'apparel' | 'bags' | 'drinkware' | 'stationery' | 'other';
}

const LOOKS: Record<string, CategoryLook> = {
  apparel: { icon: 'checkroom', key: 'apparel' },
  bags: { icon: 'backpack', key: 'bags' },
  drinkware: { icon: 'local_cafe', key: 'drinkware' },
  stationery: { icon: 'edit_note', key: 'stationery' },
};

const OTHER: CategoryLook = { icon: 'inventory_2', key: 'other' };

/** `'Apparel'` → its icon and colour key; an unknown category gets the neutral look. */
export function categoryLook(category: string): CategoryLook {
  return LOOKS[category.trim().toLowerCase()] ?? OTHER;
}

// What a product is, read from words in its name. First match wins, so specific words come before general ones.
const PRODUCT_ICONS: [RegExp, string][] = [
  [/t-?shirt|\btee\b/i, 'apparel'],
  [/hoodie|jacket|sweater/i, 'checkroom'],
  [/\bcap\b|\bhat\b/i, 'sports_baseball'],
  [/sock/i, 'footprint'],
  [/beanie/i, 'ac_unit'],
  [/backpack/i, 'backpack'],
  [/tote/i, 'shopping_bag'],
  [/laptop/i, 'laptop_chromebook'],
  [/duffel|weekender|luggage/i, 'luggage'],
  [/sling|wallet/i, 'work'],
  [/gym/i, 'fitness_center'],
  [/bottle/i, 'water_bottle'],
  [/camp/i, 'camping'],
  [/espresso/i, 'coffee'],
  [/travel|tumbler/i, 'coffee_maker'],
  [/mug|cup/i, 'local_cafe'],
  [/notebook|journal/i, 'menu_book'],
  [/planner|calendar/i, 'calendar_month'],
  [/pencil/i, 'draw'],
  [/\bpen\b/i, 'edit'],
  [/sticky/i, 'sticky_note_2'],
  [/sticker/i, 'label'],
];

/** The icon drawn on a product's tile: one for what the product is, else its category's icon. */
export function productIcon(name: string, category: string): string {
  return PRODUCT_ICONS.find(([pattern]) => pattern.test(name))?.[1] ?? categoryLook(category).icon;
}

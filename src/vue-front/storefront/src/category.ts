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

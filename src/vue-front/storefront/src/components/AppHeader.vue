<template>
  <header class="app-header">
    <RouterLink to="/" class="app-header__brand">EShop</RouterLink>
    <nav class="app-header__nav">
      <RouterLink to="/orders">My orders</RouterLink>
      <RouterLink to="/cart" class="app-header__cart" aria-label="Cart">
        <EIcon icon="shopping_cart" />
      </RouterLink>
    </nav>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.5rem;
  border-bottom: 1px solid var(--e-primitive-slate-200, #dee1e7);
}
.app-header__brand {
  font-weight: 700;
  font-size: 1.25rem;
  text-decoration: none;
  color: inherit;
}
.app-header__nav {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}
.app-header__nav a {
  color: inherit;
  text-decoration: none;
}
.app-header__cart {
  display: inline-flex;
}
</style>

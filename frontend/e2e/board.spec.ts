import { expect, test } from '@playwright/test';

test('admin can create, drag, edit and delete a task through the live API', async ({ page }) => {
  const title = `E2E task ${Date.now()}`;
  await page.goto('/');
  await page.getByLabel('Email', { exact: true }).fill('admin@teamflow.local');
  await page.getByLabel('Пароль', { exact: true }).fill('Admin-demo-2026!');
  await page.getByRole('button', { name: 'Войти в Teamflow' }).click();
  await expect(page.getByRole('heading', { name: 'Всё начинается с задачи.' })).toBeVisible();
  await page.getByRole('button', { name: 'Создать задачу', exact: true }).click();
  await page.getByLabel('Название задачи').fill(title);
  await page.getByLabel('Описание', { exact: true }).fill('Created by an end-to-end test');
  await page.getByRole('dialog').getByLabel('Приоритет', { exact: true }).selectOption('HIGH');
  await page.getByRole('dialog').getByRole('button', { name: 'Создать задачу', exact: true }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await page.getByRole('textbox', { name: 'Поиск задач' }).fill(title);
  const card = page.locator('.task-card').filter({ has: page.getByRole('button', { name: title, exact: true }) });
  await expect(card).toHaveCount(1);
  const box = await card.boundingBox();
  const target = await page.locator('#IN_PROGRESS').boundingBox();
  if (!box || !target) throw new Error('Missing drag geometry');
  // Angular CDK starts dragging after a pointer movement threshold.
  await page.mouse.move(box.x + 20, box.y + 20);
  await page.mouse.down();
  await page.mouse.move(box.x + 35, box.y + 25, { steps: 5 });
  await page.mouse.move(target.x + target.width / 2, target.y + 50, { steps: 20 });
  await page.mouse.up();
  await expect(page.locator('#IN_PROGRESS').getByRole('button', { name: title, exact: true })).toBeVisible();
  await page.getByLabel(`Статус: ${title}`, { exact: true }).selectOption('DONE');
  await expect(page.locator('#DONE').getByRole('button', { name: title, exact: true })).toBeVisible();
  await page.getByRole('button', { name: title, exact: true }).click();
  await page.getByLabel('Название задачи').fill(title + ' edited');
  await page.getByRole('button', { name: 'Сохранить изменения' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await page.getByRole('button', { name: 'Обновить доску' }).click();
  await expect(page.getByRole('button', { name: title + ' edited', exact: true })).toBeVisible();
  await page.getByRole('button', { name: `Удалить: ${title} edited`, exact: true }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Удалить задачу', exact: true }).click();
  await expect(page.getByRole('button', { name: title + ' edited', exact: true })).toHaveCount(0);
});

test('member permissions, search and mobile layout work', async ({ page }) => {
  await page.goto('/board');
  await expect(page).toHaveURL(/auth\/login/);
  await page.getByLabel('Email', { exact: true }).fill('alina@teamflow.local');
  await page.getByLabel('Пароль', { exact: true }).fill('User-demo-2026!');
  await page.getByRole('button', { name: 'Войти в Teamflow' }).click();
  await expect(page.locator('.task-card').first()).toBeVisible();
  await expect(page.locator('#IN_PROGRESS .card-actions select').first()).toHaveValue('IN_PROGRESS');
  await expect(page.locator('#DONE .card-actions select').first()).toHaveValue('DONE');
  await expect(page.locator('.delete-button')).toHaveCount(0);
  await page.getByRole('textbox', { name: 'Поиск задач' }).fill('NO_MATCH_123456789');
  await expect(page.locator('.task-card')).toHaveCount(0);
  await page.getByRole('button', { name: 'Сбросить фильтры' }).click();
  await expect(page.locator('.task-card').first()).toBeVisible();
  await page.getByRole('navigation').getByRole('button', { name: /Команда/ }).click();
  await expect(page.getByRole('button', { name: 'Добавить участника' })).toHaveCount(0);
  await page.getByRole('navigation').getByRole('button', { name: /Доска задач/ }).click();
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('button', { name: 'Создать задачу', exact: true })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await expect(page.getByRole('button', { name: 'Выйти из аккаунта' })).toBeVisible();
});

test('login errors stay visible and modal traps focus and closes with Escape', async ({ page }) => {
  await page.goto('/auth/login');
  await page.getByLabel('Email', { exact: true }).fill('admin@teamflow.local');
  await page.getByLabel('Пароль', { exact: true }).fill('wrong-password');
  await page.getByRole('button', { name: 'Войти в Teamflow' }).click();
  await expect(page.getByRole('alert')).toContainText('Неверный email или пароль');
  await page.getByLabel('Пароль', { exact: true }).fill('Admin-demo-2026!');
  await page.getByRole('button', { name: 'Войти в Teamflow' }).click();
  await page.getByRole('button', { name: 'Создать задачу', exact: true }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.keyboard.press('Tab');
  expect(await page.getByRole('dialog').evaluate(dialog => dialog.contains(document.activeElement))).toBe(true);
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).toHaveCount(0);
});

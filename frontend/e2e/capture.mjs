// Optional visual review of the local development application.
// Run from frontend: node e2e/capture.mjs
import { chromium } from '@playwright/test';
import { mkdir } from 'node:fs/promises';
const browser = await chromium.launch({ channel: process.env.PLAYWRIGHT_CHANNEL || undefined });
const page = await browser.newPage({ viewport: { width: 1440, height: 1080 }, deviceScaleFactor: 1 });
const errors = [];
page.on('pageerror', error => errors.push(error.message));
await mkdir('../docs/screenshots', { recursive: true });
await page.goto('http://127.0.0.1:4202');
await page.evaluate(() => document.fonts.ready);
await page.screenshot({ path: '../docs/screenshots/login.png', fullPage: true });
await page.getByLabel('Email', { exact: true }).fill('admin@example.com');
await page.getByLabel('Пароль', { exact: true }).fill('Admin-demo-2026!');
await page.getByRole('button', { name: 'Войти в Teamflow' }).click();
await page.locator('.task-card').first().waitFor();
await page.screenshot({ path: '../docs/screenshots/board.png', fullPage: true });
await page.getByRole('button', { name: 'Создать задачу', exact: true }).click();
await page.getByRole('dialog').waitFor();
await page.screenshot({ path: '../docs/screenshots/task-form.png', fullPage: true });
await page.keyboard.press('Escape');
await page.setViewportSize({ width: 390, height: 844 });
await page.screenshot({ path: '../docs/screenshots/mobile.png', fullPage: true });
console.log(JSON.stringify({ browserErrors: errors }));
await browser.close();

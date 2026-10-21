import { expect, test, type Page } from '@playwright/test';
import { linkFromLatestEmail } from './mailpit';

/**
 * The main journey through the real web, proxy, API, database and email (ADR-022, QA-01):
 * sign up, verify, log in, record a coffee, a recipe and a brew session, stay signed in across a
 * reload, then log out.
 */

const PASSWORD = 'una frase de prueba bastante larga';

/** On a phone the sidebar sits in a drawer behind the menu button (AppShell). */
async function openNavigation(page: Page, isMobile: boolean) {
  if (isMobile) {
    await page.getByRole('button', { name: 'Abrir navegación' }).click();
  }
}

async function navigateTo(page: Page, isMobile: boolean, label: string) {
  await openNavigation(page, isMobile);
  await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: label }).click();
  await expect(page.getByRole('heading', { level: 1, name: label })).toBeVisible();
}

test('sign up, record a brew, and log out', async ({ page, request, baseURL, isMobile }, testInfo) => {
  const email = `e2e-${Date.now()}-${testInfo.project.name}@example.com`;

  await test.step('sign up', async () => {
    await page.goto('/register');
    await page.getByLabel('Correo').fill(email);
    await page.getByLabel('Contraseña', { exact: true }).fill(PASSWORD);
    await page.getByLabel('Confirma la contraseña').fill(PASSWORD);
    await page.getByRole('button', { name: 'Crear cuenta' }).click();
    await expect(page.getByRole('heading', { name: 'Revisa tu correo' })).toBeVisible();
  });

  await test.step('verify the email from the inbox', async () => {
    const link = await linkFromLatestEmail(request, email, `${baseURL}/verify-email?token=`);
    await page.goto(link);
    await expect(page.getByText('Tu correo quedó verificado.')).toBeVisible();
  });

  await test.step('log in', async () => {
    await page.goto('/login');
    await page.getByLabel('Correo').fill(email);
    await page.getByLabel('Contraseña', { exact: true }).fill(PASSWORD);
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await page.waitForURL('**/dashboard');
    // First login asks for language and theme (ThemeOnboardingDialog).
    await page
      .getByRole('dialog', { name: 'Configura BrewDeck' })
      .getByRole('button', { name: 'Continuar' })
      .click();
  });

  await test.step('add a coffee', async () => {
    await navigateTo(page, isMobile, 'Cafés');
    await page.getByRole('button', { name: 'Agregar café' }).click();
    const form = page.getByRole('dialog', { name: 'Agregar café' });
    await form.getByLabel('Nombre').fill('Etiopía Guji');
    await form.getByRole('button', { name: 'Crear' }).click();
    await expect(form).toBeHidden();
    await expect(page.getByRole('link', { name: 'Etiopía Guji' })).toBeVisible();
  });

  await test.step('add a recipe for it', async () => {
    await navigateTo(page, isMobile, 'Recetas');
    await page.getByRole('button', { name: 'Agregar receta' }).click();
    const form = page.getByRole('dialog', { name: 'Agregar receta' });
    await form.getByLabel('Nombre').fill('V60 de la mañana');
    await form.getByRole('combobox', { name: 'Café' }).selectOption({ label: 'Etiopía Guji' });
    await form.getByRole('combobox', { name: 'Método' }).selectOption({ label: 'V60' });
    await form.getByLabel('Gramos de café').fill('15');
    await form.getByLabel('Gramos de agua').fill('250');
    await form.getByRole('button', { name: 'Crear' }).click();
    await expect(form).toBeHidden();
    await expect(page.getByRole('link', { name: 'V60 de la mañana' })).toBeVisible();
    await expect(page.getByText('1:16.7')).toBeVisible();
  });

  await test.step('record a brew session', async () => {
    await navigateTo(page, isMobile, 'Preparaciones');
    await page.getByRole('button', { name: 'Agregar preparación' }).click();
    const form = page.getByRole('dialog', { name: 'Agregar preparación' });
    await form.getByRole('combobox', { name: 'Receta' }).selectOption({ label: 'V60 de la mañana' });
    await form.getByLabel('Calificación').fill('8');
    await form.getByLabel('Sabor en taza').fill('Floral, durazno');
    await form.getByRole('button', { name: 'Crear' }).click();
    await expect(form).toBeHidden();
    await expect(page.getByText('V60 de la mañana').first()).toBeVisible();
  });

  await test.step('stay signed in after a reload', async () => {
    // The access token lives in memory; the refresh cookie through the Next proxy restores it.
    await page.reload();
    await expect(page.getByRole('heading', { level: 1, name: 'Preparaciones' })).toBeVisible();
    await expect(page.getByText('V60 de la mañana').first()).toBeVisible();
  });

  await test.step('log out', async () => {
    await openNavigation(page, isMobile);
    await page.getByRole('button', { name: 'Cerrar sesión' }).click();
    await page.waitForURL('**/login');
    await page.goto('/coffees');
    await page.waitForURL('**/login**');
    await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  });
});

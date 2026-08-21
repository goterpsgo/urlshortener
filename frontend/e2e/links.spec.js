import { expect, test } from '@playwright/test'

function uniqueUsername() {
  return `e2e_${Date.now()}_${Math.floor(Math.random() * 1e6)}`
}

test('register, shorten a URL, then edit it from My Links', async ({ page }) => {
  const username = uniqueUsername()
  const password = 'correcthorsebattery'

  await page.goto('/app/register')
  await page.getByLabel('Username').fill(username)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Register' }).click()

  await page.waitForURL('**/app/')

  await page.getByLabel('URL to shorten').fill('https://example.com/original-path')
  await page.getByRole('button', { name: 'Shorten' }).click()
  await expect(page.getByText('example.com/original-path')).toBeVisible()

  await page.getByRole('link', { name: 'My Links' }).click()
  await page.waitForURL('**/app/links')
  await expect(page.getByText('example.com/original-path')).toBeVisible()

  await page.getByRole('button', { name: 'Edit' }).click()
  await page.locator('input[type="url"]').fill('https://example.com/edited-path')
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByText('example.com/edited-path')).toBeVisible()

  await page.reload()
  await expect(page.getByText('example.com/edited-path')).toBeVisible()
})

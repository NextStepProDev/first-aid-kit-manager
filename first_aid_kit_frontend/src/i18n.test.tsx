import { render, screen } from '@testing-library/react'
import { MemoryRouter, Link } from 'react-router-dom'
import { Trans } from 'react-i18next'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from './i18n'

/**
 * Konfiguracja i18n przy i18next 27 i react-i18next 18: zmieniły się domyślne fallbackLng,
 * returnEmptyString i obsługa kodów języka, a Trans sam reaguje na zmianę języka.
 */
describe('i18n', () => {
  afterEach(async () => {
    await i18n.changeLanguage('pl')
  })

  it('translates both supported languages', async () => {
    await i18n.changeLanguage('pl')
    expect(i18n.t('auth:register.registerButton')).not.toBe('register.registerButton')
    const pl = i18n.t('auth:register.privacyNotice')

    await i18n.changeLanguage('en')
    expect(i18n.t('auth:register.privacyNotice')).not.toBe(pl)
  })

  it('falls back to English for a language without translations', async () => {
    await i18n.changeLanguage('de')
    expect(i18n.t('auth:register.privacyNotice')).toBe(
      'By registering, you accept our <1>Privacy Policy</1>.',
    )
  })

  it('renders the privacy notice link from Trans and follows a language change', async () => {
    await i18n.changeLanguage('pl')
    render(
      <MemoryRouter>
        <Trans
          i18nKey="register.privacyNotice"
          ns="auth"
          components={{ 1: <Link to="/polityka-prywatnosci" /> }}
        />
      </MemoryRouter>,
    )

    const link = screen.getByRole('link', { name: 'Politykę prywatności' })
    expect(link).toHaveAttribute('href', '/polityka-prywatnosci')

    await i18n.changeLanguage('en')
    expect(await screen.findByRole('link', { name: 'Privacy Policy' })).toBeInTheDocument()
  })
})

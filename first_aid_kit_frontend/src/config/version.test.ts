import { describe, it, expect } from 'vitest'
import { APP_VERSION, APP_VERSION_LABEL } from './version'
import versionFile from '../../VERSION?raw'

describe('version config', () => {
  it('exposes a semver-looking app version', () => {
    expect(APP_VERSION).toMatch(/^\d+\.\d+\.\d+$/)
  })

  it('embeds the version in the human-readable label', () => {
    expect(APP_VERSION_LABEL).toContain(APP_VERSION)
  })

  // CI taguje obraz wersją z pliku VERSION, a stopka pokazuje wersję z package.json.
  it('matches the VERSION file the image is tagged with', () => {
    expect(APP_VERSION).toBe(versionFile.trim())
  })
})

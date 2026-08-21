import { useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { getFeaturesForUser, updateFeaturesForUser } from '../api'
import { useAuth } from '../AuthContext'

function overrideToValue(override) {
  if (override === null || override === undefined) return 'default'
  return override ? 'on' : 'off'
}

export default function Toggles() {
  const { logout, features, setFeatureOverride, isAdmin } = useAuth()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  async function handleChange(event) {
    const value = event.target.value
    const h1Green = value === 'default' ? null : value === 'on'

    setSaving(true)
    setError(null)
    try {
      await setFeatureOverride({ h1Green })
    } catch {
      setError('Could not save that toggle')
    } finally {
      setSaving(false)
    }
  }

  const currentValue = overrideToValue(features.h1GreenOverride)

  return (
    <div>
      <header>
        <h1>Feature Toggles</h1>
        <nav>
          <RouterLink to="/">Shorten a URL</RouterLink>
          <RouterLink to="/links">My Links</RouterLink>
        </nav>
        <button type="button" onClick={logout}>
          Log out
        </button>
      </header>

      {error && <p role="alert">{error}</p>}

      <fieldset disabled={saving}>
        <legend>Green heading (h1)</legend>
        <label>
          <input
            type="radio"
            name="h1Green"
            value="default"
            checked={currentValue === 'default'}
            onChange={handleChange}
          />
          Use environment default
        </label>
        <label>
          <input type="radio" name="h1Green" value="on" checked={currentValue === 'on'} onChange={handleChange} />
          On
        </label>
        <label>
          <input type="radio" name="h1Green" value="off" checked={currentValue === 'off'} onChange={handleChange} />
          Off
        </label>
      </fieldset>

      <p>Effective right now: {features.h1Green ? 'green' : 'default color'}</p>

      {isAdmin && <AdminUserToggle />}
    </div>
  )
}

function AdminUserToggle() {
  const [userId, setUserId] = useState('')
  const [target, setTarget] = useState(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  async function handleLookup(event) {
    event.preventDefault()
    setLoading(true)
    setError(null)
    setTarget(null)
    try {
      setTarget(await getFeaturesForUser(userId))
    } catch {
      setError('No user found with that id')
    } finally {
      setLoading(false)
    }
  }

  async function handleChange(event) {
    const value = event.target.value
    const h1Green = value === 'default' ? null : value === 'on'

    setSaving(true)
    setError(null)
    try {
      setTarget(await updateFeaturesForUser(userId, { h1Green }))
    } catch {
      setError('Could not save that toggle for this user')
    } finally {
      setSaving(false)
    }
  }

  const currentValue = target ? overrideToValue(target.h1GreenOverride) : 'default'

  return (
    <section>
      <h2>Admin: set a toggle for another user</h2>

      <form onSubmit={handleLookup}>
        <label>
          User id
          <input
            type="number"
            min="1"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            required
          />
        </label>
        <button type="submit" disabled={loading}>
          Look up
        </button>
      </form>

      {error && <p role="alert">{error}</p>}

      {target && (
        <fieldset disabled={saving}>
          <legend>
            Green heading (h1) for <strong>{target.username}</strong> (id {target.userId})
          </legend>
          <label>
            <input
              type="radio"
              name="adminH1Green"
              value="default"
              checked={currentValue === 'default'}
              onChange={handleChange}
            />
            Use environment default
          </label>
          <label>
            <input
              type="radio"
              name="adminH1Green"
              value="on"
              checked={currentValue === 'on'}
              onChange={handleChange}
            />
            On
          </label>
          <label>
            <input
              type="radio"
              name="adminH1Green"
              value="off"
              checked={currentValue === 'off'}
              onChange={handleChange}
            />
            Off
          </label>
          <p>Effective for this user: {target.h1Green ? 'green' : 'default color'}</p>
        </fieldset>
      )}
    </section>
  )
}

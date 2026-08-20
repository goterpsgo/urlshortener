import { useState } from 'react'
import { createLink } from '../api'
import { useAuth } from '../AuthContext'

export default function Dashboard() {
  const { logout } = useAuth()
  const [originalUrl, setOriginalUrl] = useState('')
  const [links, setLinks] = useState([])
  const [error, setError] = useState(null)

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    try {
      const link = await createLink(originalUrl)
      setLinks((current) => [link, ...current])
      setOriginalUrl('')
    } catch {
      setError('Could not shorten that URL — check it is a valid, complete URL')
    }
  }

  return (
    <div>
      <header>
        <h1>Shorten a URL</h1>
        <button type="button" onClick={logout}>
          Log out
        </button>
      </header>

      <form onSubmit={handleSubmit}>
        {error && <p role="alert">{error}</p>}
        <label>
          URL to shorten
          <input
            type="url"
            value={originalUrl}
            onChange={(e) => setOriginalUrl(e.target.value)}
            placeholder="https://example.com/some/long/path"
            required
          />
        </label>
        <button type="submit">Shorten</button>
      </form>

      {links.length > 0 && (
        <ul>
          {links.map((link) => (
            <li key={link.shortCode}>
              <a href={link.shortUrl}>{link.shortUrl}</a> &rarr; {link.originalUrl}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

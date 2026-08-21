import { useEffect, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { listLinks, updateLink } from '../api'
import { useAuth } from '../AuthContext'

export default function Links() {
  const { logout } = useAuth()
  const [links, setLinks] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(null)
  const [editingId, setEditingId] = useState(null)
  const [editValue, setEditValue] = useState('')
  const [saveError, setSaveError] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let cancelled = false
    listLinks()
      .then((data) => {
        if (!cancelled) setLinks(data)
      })
      .catch(() => {
        if (!cancelled) setLoadError('Could not load your links.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  function startEdit(link) {
    setEditingId(link.id)
    setEditValue(link.originalUrl)
    setSaveError(null)
  }

  function cancelEdit() {
    setEditingId(null)
    setEditValue('')
    setSaveError(null)
  }

  async function saveEdit(id) {
    setSaving(true)
    setSaveError(null)
    try {
      const updated = await updateLink(id, editValue)
      setLinks((current) => current.map((link) => (link.id === id ? updated : link)))
      setEditingId(null)
      setEditValue('')
    } catch {
      setSaveError('Could not save that URL — check it is a valid, complete URL')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <header>
        <h1>My Links</h1>
        <nav>
          <RouterLink to="/">Shorten a URL</RouterLink>
        </nav>
        <button type="button" onClick={logout}>
          Log out
        </button>
      </header>

      {loading && <p>Loading…</p>}
      {loadError && <p role="alert">{loadError}</p>}

      {!loading && !loadError && links.length === 0 && <p>You haven't shortened any URLs yet.</p>}

      {links.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Short URL</th>
              <th>Original URL</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {links.map((link) => (
              <tr key={link.id}>
                <td>
                  <a href={link.shortUrl}>{link.shortUrl}</a>
                </td>
                <td>
                  {editingId === link.id ? (
                    <>
                      <input
                        type="url"
                        value={editValue}
                        onChange={(e) => setEditValue(e.target.value)}
                        required
                      />
                      {saveError && <p role="alert">{saveError}</p>}
                    </>
                  ) : (
                    link.originalUrl
                  )}
                </td>
                <td>
                  {editingId === link.id ? (
                    <>
                      <button type="button" onClick={() => saveEdit(link.id)} disabled={saving}>
                        Save
                      </button>
                      <button type="button" onClick={cancelEdit} disabled={saving}>
                        Cancel
                      </button>
                    </>
                  ) : (
                    <button type="button" onClick={() => startEdit(link)}>
                      Edit
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

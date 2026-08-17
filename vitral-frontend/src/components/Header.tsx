import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './Header.css'

export function Header() {
  const navigate = useNavigate()
  const [search, setSearch] = useState('')

  return (
    <header className="header">
      <div className="header-inner container">
        <form
          className="header-search"
          onSubmit={(e) => {
            e.preventDefault()
            const term = search.trim()
            if (term) navigate(`/busca?tab=produtos&q=${encodeURIComponent(term)}`)
          }}
        >
          <input
            type="search"
            aria-label="Pesquisar produtos"
            placeholder="O que você está procurando?"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <button type="submit" aria-label="Buscar">🔍</button>
        </form>
      </div>
    </header>
  )
}

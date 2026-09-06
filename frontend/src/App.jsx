import './App.css'

function App() {
  return (
    <main className="app">
      <section className="welcome-card" aria-labelledby="welcome-title">
        <div className="brand-mark" aria-hidden="true">
          B
        </div>
        <p className="eyebrow">Appointment management, simplified</p>
        <h1 id="welcome-title">BookFlow</h1>
        <p className="status">
          Frontend foundation is ready. Authentication and workspace setup come
          next.
        </p>
        <div className="stack" aria-label="Frontend technology">
          React 18 · JavaScript · Vite
        </div>
      </section>
    </main>
  )
}

export default App

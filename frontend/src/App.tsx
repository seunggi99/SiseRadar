import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './lib/auth';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { WatchlistPage } from './pages/WatchlistPage';

function RequireAuth({ children }: { children: React.ReactElement }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<DashboardPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/watchlist"
        element={
          <RequireAuth>
            <WatchlistPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;

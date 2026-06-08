import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';

// Layout
import Navbar from './components/common/Navbar';
import Footer from './components/common/Footer';
import ProtectedRoute from './components/common/ProtectedRoute';

// Pages
import Home            from './pages/Home';
import Login           from './pages/Login';
import Register        from './pages/Register';
import SearchResults   from './pages/SearchResults';
import PropertyDetails from './pages/PropertyDetails';
import Wishlist        from './pages/Wishlist';
import UserDashboard   from './pages/dashboard/UserDashboard';
import HostDashboard   from './pages/dashboard/HostDashboard';
import AdminDashboard  from './pages/dashboard/AdminDashboard';

const App = () => {
  return (
    <AuthProvider>
      <BrowserRouter>

        {/* Toast notifications — shows success/error messages */}
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 3000,
            style: {
              background: '#363636',
              color: '#fff',
            },
            success: {
              style: { background: '#059669' },
            },
            error: {
              style: { background: '#DC2626' },
            },
          }}
        />

        <Navbar />

        <main style={{ minHeight: '80vh' }}>
          <Routes>

            {/* Public routes */}
            <Route path="/"        element={<Home />} />
            <Route path="/login"   element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/search"  element={<SearchResults />} />
            <Route path="/properties/:id" element={<PropertyDetails />} />

            {/* Protected — any logged in user */}
            <Route path="/wishlist" element={
              <ProtectedRoute>
                <Wishlist />
              </ProtectedRoute>
            }/>

            <Route path="/dashboard/user" element={
              <ProtectedRoute>
                <UserDashboard />
              </ProtectedRoute>
            }/>

            {/* Protected — host only */}
            <Route path="/dashboard/host" element={
              <ProtectedRoute role="ROLE_HOST">
                <HostDashboard />
              </ProtectedRoute>
            }/>

            {/* Protected — admin only */}
            <Route path="/dashboard/admin" element={
              <ProtectedRoute role="ROLE_ADMIN">
                <AdminDashboard />
              </ProtectedRoute>
            }/>

          </Routes>
        </main>

        <Footer />

      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
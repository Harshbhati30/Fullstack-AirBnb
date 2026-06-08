import React from 'react';
import { Navigate } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import Loader from './Loader';

const ProtectedRoute = ({ children, role }) => {
  const { user, loading } = useAuth();

  if (loading) return <Loader />;

  if (!user) return <Navigate to="/login" replace />;

  if (role) {
    const hasRole = role === 'ROLE_HOST'
      ? user.roles?.includes('ROLE_HOST') ||
        user.roles?.includes('ROLE_ADMIN')
      : user.roles?.includes(role);

    if (!hasRole) return <Navigate to="/" replace />;
  }

  return children;
};

export default ProtectedRoute;
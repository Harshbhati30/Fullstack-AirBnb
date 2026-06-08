import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiHeart, FiUser, FiLogOut,
         FiHome, FiSearch } from 'react-icons/fi';
import useAuth from '../../hooks/useAuth';

const Navbar = () => {
  const { user, logout, isAdmin, isHost } = useAuth();
  const navigate = useNavigate();
  const [dropdownOpen, setDropdownOpen] = useState(false);

  return (
    <nav className="navbar navbar-expand-lg bg-white border-bottom sticky-top shadow-sm">
      <div className="container">

        {/* Logo */}
        <Link className="navbar-brand d-flex align-items-center gap-2
                         fw-bold fs-4 text-danger" to="/">
          <FiHome />
          StayNest
        </Link>

        {/* Toggler for mobile */}
        <button className="navbar-toggler" type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarContent">
          <span className="navbar-toggler-icon"></span>
        </button>

        <div className="collapse navbar-collapse" id="navbarContent">

          {/* Center links */}
          <ul className="navbar-nav me-auto mb-2 mb-lg-0 ms-4">
            <li className="nav-item">
              <Link className="nav-link nav-link-custom" to="/search">
                <FiSearch className="me-1" />
                Explore
              </Link>
            </li>

            {isHost && (
              <li className="nav-item">
                <Link className="nav-link nav-link-custom"
                      to="/dashboard/host">
                  Host Dashboard
                </Link>
              </li>
            )}

            {isAdmin && (
              <li className="nav-item">
                <Link className="nav-link nav-link-custom"
                      to="/dashboard/admin">
                  Admin
                </Link>
              </li>
            )}
          </ul>

          {/* Right side */}
          <div className="d-flex align-items-center gap-3">
            {user ? (
              <>
                <Link to="/wishlist"
                  className="btn btn-light btn-sm d-flex
                             align-items-center gap-1">
                  <FiHeart className="text-danger" />
                  Wishlist
                </Link>

                {/* Dropdown */}
                <div className="dropdown">
                  <button
                    className="btn btn-outline-secondary btn-sm
                               d-flex align-items-center gap-2"
                    onClick={() => setDropdownOpen(!dropdownOpen)}>
                    <FiUser />
                    {user.firstName}
                  </button>

                  {dropdownOpen && (
                    <ul className="dropdown-menu dropdown-menu-end show">
                      <li>
                        <Link className="dropdown-item"
                              to="/dashboard/user"
                              onClick={() => setDropdownOpen(false)}>
                          My Bookings
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item"
                              to="/profile"
                              onClick={() => setDropdownOpen(false)}>
                          Profile
                        </Link>
                      </li>
                      <li><hr className="dropdown-divider" /></li>
                      <li>
                        <button className="dropdown-item text-danger
                                           d-flex align-items-center gap-2"
                          onClick={logout}>
                          <FiLogOut />
                          Logout
                        </button>
                      </li>
                    </ul>
                  )}
                </div>
              </>
            ) : (
              <div className="d-flex gap-2">
                <Link to="/login"
                  className="btn btn-outline-danger btn-sm">
                  Login
                </Link>
                <Link to="/register"
                  className="btn btn-danger btn-sm">
                  Sign Up
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
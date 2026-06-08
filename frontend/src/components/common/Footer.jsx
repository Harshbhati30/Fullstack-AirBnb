import React from 'react';
import { Link } from 'react-router-dom';
import { FiHome } from 'react-icons/fi';

const Footer = () => {
  return (
    <footer className="bg-dark text-white mt-5 py-5">
      <div className="container">
        <div className="row g-4">

          <div className="col-md-4">
            <div className="d-flex align-items-center gap-2 mb-3">
              <FiHome className="text-danger fs-4" />
              <span className="fw-bold fs-5">StayNest</span>
            </div>
            <p className="text-muted small">
              Find your perfect stay anywhere in India.
              Book unique homes and experiences.
            </p>
          </div>

          <div className="col-md-2">
            <h6 className="fw-semibold mb-3">Explore</h6>
            <ul className="list-unstyled">
              <li className="mb-2">
                <Link to="/search" className="text-muted
                      text-decoration-none small">
                  Search Homes
                </Link>
              </li>
              <li className="mb-2">
                <Link to="/search?propertyType=VILLA"
                  className="text-muted text-decoration-none small">
                  Villas
                </Link>
              </li>
              <li>
                <Link to="/search?propertyType=APARTMENT"
                  className="text-muted text-decoration-none small">
                  Apartments
                </Link>
              </li>
            </ul>
          </div>

          <div className="col-md-2">
            <h6 className="fw-semibold mb-3">Host</h6>
            <ul className="list-unstyled">
              <li className="mb-2">
                <Link to="/dashboard/host"
                  className="text-muted text-decoration-none small">
                  Host Dashboard
                </Link>
              </li>
              <li>
                <Link to="/dashboard/host"
                  className="text-muted text-decoration-none small">
                  List Your Property
                </Link>
              </li>
            </ul>
          </div>

          <div className="col-md-2">
            <h6 className="fw-semibold mb-3">Account</h6>
            <ul className="list-unstyled">
              <li className="mb-2">
                <Link to="/login"
                  className="text-muted text-decoration-none small">
                  Login
                </Link>
              </li>
              <li>
                <Link to="/register"
                  className="text-muted text-decoration-none small">
                  Sign Up
                </Link>
              </li>
            </ul>
          </div>

        </div>

        <hr className="border-secondary mt-4" />
        <div className="d-flex justify-content-between
                        align-items-center flex-wrap gap-2">
          <p className="text-muted small mb-0">
            © 2024 StayNest. All rights reserved.
          </p>
          <p className="text-muted small mb-0">
            Built with Java Spring Boot + React JS
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
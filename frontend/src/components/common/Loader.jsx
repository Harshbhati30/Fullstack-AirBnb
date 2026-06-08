import React from 'react';

const Loader = ({ fullScreen = true }) => {
  if (fullScreen) {
    return (
      <div className="loader-overlay">
        <div className="text-center">
          <div className="spinner-border text-danger mb-3"
               style={{ width: '3rem', height: '3rem' }}
               role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="text-muted">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="d-flex justify-content-center py-5">
      <div className="spinner-border text-danger" role="status">
        <span className="visually-hidden">Loading...</span>
      </div>
    </div>
  );
};

export default Loader;
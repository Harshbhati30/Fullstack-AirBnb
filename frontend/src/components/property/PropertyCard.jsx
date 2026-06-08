import React from 'react';
import { useNavigate } from 'react-router-dom';
import { FiStar, FiMapPin, FiUsers } from 'react-icons/fi';
import { formatPrice, getImageUrl, truncateText } from '../../utils/helpers';

const PropertyCard = ({ property }) => {
  const navigate = useNavigate();

  const primaryImage = property.images?.find(img => img.isPrimary)
    || property.images?.[0];

  return (
    <div
      className="card property-card h-100 rounded-4 overflow-hidden"
      onClick={() => navigate(`/properties/${property.id}`)}>


      <div style={{ height: '200px', overflow: 'hidden' }}>
        <img
          src={getImageUrl(primaryImage?.imagePath)}
          alt={property.title}
          className="w-100 h-100"
          style={{ objectFit: 'cover', transition: 'transform 0.3s' }}
          onMouseEnter={e =>
            e.target.style.transform = 'scale(1.05)'}
          onMouseLeave={e =>
            e.target.style.transform = 'scale(1)'}
        />
      </div>

      <div className="card-body p-3">

        <div className="d-flex align-items-center gap-1
                        text-muted small mb-1">
          <FiMapPin size={12} />
          <span>{property.city}</span>
        </div>

  
        <h6 className="card-title fw-semibold mb-1">
          {truncateText(property.title, 50)}
        </h6>


        <p className="text-muted small mb-2">
          {property.propertyType?.replace('_', ' ')}
        </p>


        <div className="d-flex align-items-center gap-1
                        text-muted small mb-2">
          <FiUsers size={12} />
          <span>Up to {property.maxGuests} guests</span>
        </div>


        <div className="d-flex justify-content-between
                        align-items-center mt-auto">
          <div>
            <span className="fw-bold text-dark">
              {formatPrice(property.pricePerNight)}
            </span>
            <span className="text-muted small"> / night</span>
          </div>

          {property.totalReviews > 0 && (
            <div className="d-flex align-items-center gap-1">
              <FiStar className="star-filled" size={13} />
              <span className="small fw-semibold">
                {parseFloat(property.averageRating).toFixed(1)}
              </span>
              <span className="text-muted small">
                ({property.totalReviews})
              </span>
            </div>
          )}
        </div>

      </div>
    </div>
  );
};

export default PropertyCard;
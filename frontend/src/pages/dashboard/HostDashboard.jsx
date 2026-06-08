import React, { useState, useEffect } from 'react';
import { FiPlus, FiEdit, FiTrash2,
         FiHome, FiDollarSign } from 'react-icons/fi';
import toast from 'react-hot-toast';
import axiosInstance from '../../api/axios';
import useAuth from '../../hooks/useAuth';
import Loader from '../../components/common/Loader';
import { formatPrice, getImageUrl } from '../../utils/helpers';

const HostDashboard = () => {
  const { user } = useAuth();

  const [properties, setProperties]     = useState([]);
  const [bookings, setBookings]         = useState([]);
  const [loading, setLoading]           = useState(true);
  const [activeTab, setActiveTab]       = useState('listings');
  const [showAddModal, setShowAddModal] = useState(false);


  const [newProperty, setNewProperty] = useState({
    title: '', description: '', pricePerNight: '',
    maxGuests: '', bedrooms: '', bathrooms: '',
    propertyType: 'APARTMENT',
    street: '', city: '', state: '',
    country: 'India', zipCode: '',
  });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchProperties();
    fetchBookings();
  }, []);

  const fetchProperties = async () => {
    try {
      const res = await axiosInstance.get(
        '/properties/host/my-listings?page=0&size=20');
      setProperties(res.data.data.content || []);
    } catch {
      toast.error('Failed to load listings');
    } finally {
      setLoading(false);
    }
  };

  const fetchBookings = async () => {
    try {
      const res = await axiosInstance.get(
        '/bookings/host/bookings?page=0&size=20');
      setBookings(res.data.data.content || []);
    } catch {}
  };

  const handleAddProperty = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await axiosInstance.post('/properties', {
        ...newProperty,
        pricePerNight: parseFloat(newProperty.pricePerNight),
        maxGuests: parseInt(newProperty.maxGuests),
        bedrooms: parseInt(newProperty.bedrooms),
        bathrooms: parseInt(newProperty.bathrooms),
      });
      toast.success('Property listed successfully!');
      setShowAddModal(false);
      fetchProperties();
      setNewProperty({
        title: '', description: '', pricePerNight: '',
        maxGuests: '', bedrooms: '', bathrooms: '',
        propertyType: 'APARTMENT',
        street: '', city: '', state: '',
        country: 'India', zipCode: '',
      });
    } catch (error) {
      toast.error(error.response?.data?.message ||
                  'Failed to add property');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (propertyId) => {
    if (!window.confirm('Delete this property?')) return;
    try {
      await axiosInstance.delete(`/properties/${propertyId}`);
      toast.success('Property removed');
      fetchProperties();
    } catch {
      toast.error('Failed to delete');
    }
  };

  if (loading) return <Loader />;

  const totalRevenue = bookings
    .filter(b => b.status === 'CONFIRMED')
    .reduce((sum, b) => sum + (b.totalAmount || 0), 0);

  return (
    <div className="container py-5">

    
      <div className="d-flex justify-content-between
                      align-items-center mb-4">
        <div>
          <h4 className="fw-bold mb-1">Host Dashboard</h4>
          <p className="text-muted mb-0">
            Welcome, {user?.firstName}
          </p>
        </div>
        <button
          className="btn btn-danger d-flex
                     align-items-center gap-2"
          onClick={() => setShowAddModal(true)}>
          <FiPlus />
          Add Property
        </button>
      </div>

    
      <div className="row g-3 mb-4">
        {[
          { label: 'Total Listings',
            value: properties.length,
            icon: <FiHome />, color: 'text-primary' },
          { label: 'Total Bookings',
            value: bookings.length,
            icon: <FiHome />, color: 'text-warning' },
          { label: 'Total Revenue',
            value: formatPrice(totalRevenue),
            icon: <FiDollarSign />, color: 'text-success' },
        ].map((stat, i) => (
          <div key={i} className="col-md-4">
            <div className="card border-0 shadow-sm rounded-4
                            p-4 text-center">
              <div className={`fs-3 mb-2 ${stat.color}`}>
                {stat.icon}
              </div>
              <h4 className="fw-bold mb-0">{stat.value}</h4>
              <p className="text-muted small mb-0">{stat.label}</p>
            </div>
          </div>
        ))}
      </div>


      <ul className="nav nav-tabs mb-4">
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'listings'
              ? 'active text-danger' : 'text-muted'}`}
            onClick={() => setActiveTab('listings')}>
            My Listings
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'bookings'
              ? 'active text-danger' : 'text-muted'}`}
            onClick={() => setActiveTab('bookings')}>
            Bookings
          </button>
        </li>
      </ul>


      {activeTab === 'listings' && (
        <div className="row g-4">
          {properties.length === 0 ? (
            <div className="col-12 text-center py-5">
              <FiHome size={48} className="text-muted mb-3" />
              <h5 className="text-muted">No listings yet</h5>
              <button
                className="btn btn-danger mt-2"
                onClick={() => setShowAddModal(true)}>
                Add your first property
              </button>
            </div>
          ) : (
            properties.map(property => (
              <div key={property.id} className="col-md-6 col-lg-4">
                <div className="card border-0 shadow-sm rounded-4
                                overflow-hidden h-100">
                  <img
                    src={getImageUrl(
                      property.images?.[0]?.imagePath)}
                    alt={property.title}
                    style={{ height: '160px', objectFit: 'cover' }}
                    className="w-100"
                  />
                  <div className="card-body p-3">
                    <h6 className="fw-bold mb-1">{property.title}</h6>
                    <p className="text-muted small mb-2">
                      {property.city}
                    </p>
                    <p className="text-danger fw-semibold mb-3">
                      {formatPrice(property.pricePerNight)}/night
                    </p>
                    <div className="d-flex gap-2">
                      <button
                        className="btn btn-outline-danger btn-sm"
                        onClick={() => handleDelete(property.id)}>
                        <FiTrash2 size={14} />
                      </button>
                      <span className={`badge ${property.isActive
                        ? 'bg-success' : 'bg-secondary'}`}>
                        {property.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}


      {activeTab === 'bookings' && (
        <div>
          {bookings.length === 0 ? (
            <div className="text-center py-5">
              <p className="text-muted">No bookings yet</p>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover">
                <thead className="table-light">
                  <tr>
                    <th>Property</th>
                    <th>Guest</th>
                    <th>Check In</th>
                    <th>Check Out</th>
                    <th>Amount</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {bookings.map(booking => (
                    <tr key={booking.id}>
                      <td className="small fw-semibold">
                        {booking.property?.title}
                      </td>
                      <td className="small">
                        {booking.user?.firstName}{' '}
                        {booking.user?.lastName}
                      </td>
                      <td className="small">
                        {booking.checkInDate}
                      </td>
                      <td className="small">
                        {booking.checkOutDate}
                      </td>
                      <td className="small text-danger fw-semibold">
                        {formatPrice(booking.totalAmount)}
                      </td>
                      <td>
                        <span className="badge bg-secondary">
                          {booking.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

     
      {showAddModal && (
        <div className="modal show d-block"
             style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-lg modal-dialog-scrollable">
            <div className="modal-content rounded-4 border-0">
              <div className="modal-header border-0 pb-0">
                <h5 className="modal-title fw-bold">
                  Add New Property
                </h5>
                <button
                  className="btn-close"
                  onClick={() => setShowAddModal(false)} />
              </div>
              <div className="modal-body">
                <form onSubmit={handleAddProperty}>
                  <div className="row g-3">

                    <div className="col-12">
                      <label className="form-label small fw-semibold">
                        Title
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Beautiful beachside villa..."
                        value={newProperty.title}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, title: e.target.value }))}
                        required
                      />
                    </div>

                    <div className="col-12">
                      <label className="form-label small fw-semibold">
                        Description
                      </label>
                      <textarea
                        className="form-control"
                        rows={3}
                        placeholder="Describe your property..."
                        value={newProperty.description}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, description: e.target.value }))}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label small fw-semibold">
                        Price per night (₹)
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        placeholder="2500"
                        value={newProperty.pricePerNight}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, pricePerNight: e.target.value }))}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label small fw-semibold">
                        Max Guests
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        placeholder="4"
                        min={1}
                        value={newProperty.maxGuests}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, maxGuests: e.target.value }))}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label small fw-semibold">
                        Property Type
                      </label>
                      <select
                        className="form-select"
                        value={newProperty.propertyType}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, propertyType: e.target.value }))}>
                        {['APARTMENT','HOUSE','VILLA','STUDIO',
                          'COTTAGE','FARMHOUSE'].map(t => (
                          <option key={t} value={t}>{t}</option>
                        ))}
                      </select>
                    </div>

                    <div className="col-6">
                      <label className="form-label small fw-semibold">
                        Bedrooms
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        min={0}
                        value={newProperty.bedrooms}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, bedrooms: e.target.value }))}
                      />
                    </div>

                    <div className="col-6">
                      <label className="form-label small fw-semibold">
                        Bathrooms
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        min={0}
                        value={newProperty.bathrooms}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, bathrooms: e.target.value }))}
                      />
                    </div>

                    <div className="col-12">
                      <hr />
                      <h6 className="fw-semibold">Address</h6>
                    </div>

                    <div className="col-12">
                      <label className="form-label small fw-semibold">
                        Street
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="123 Beach Road"
                        value={newProperty.street}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, street: e.target.value }))}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label small fw-semibold">
                        City
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Mumbai"
                        value={newProperty.city}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, city: e.target.value }))}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label small fw-semibold">
                        State
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Maharashtra"
                        value={newProperty.state}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, state: e.target.value }))}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label small fw-semibold">
                        ZIP Code
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="400001"
                        value={newProperty.zipCode}
                        onChange={e => setNewProperty(p =>
                          ({ ...p, zipCode: e.target.value }))}
                      />
                    </div>

                    <div className="col-12 mt-3">
                      <button
                        type="submit"
                        className="btn btn-danger w-100 py-2 fw-semibold"
                        disabled={submitting}>
                        {submitting
                          ? <><span className="spinner-border
                                     spinner-border-sm me-2" />
                              Adding property...</>
                          : 'Add Property'}
                      </button>
                    </div>

                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default HostDashboard;
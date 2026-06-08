export const formatPrice = (amount) => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(amount);
};

export const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

export const calculateNights = (checkIn, checkOut) => {
  const start = new Date(checkIn);
  const end   = new Date(checkOut);
  return Math.ceil((end - start) / (1000 * 60 * 60 * 24));
};

export const getImageUrl = (imagePath) => {
  if (!imagePath) return 'https://via.placeholder.com/400x300?text=No+Image';
  if (imagePath.startsWith('http')) return imagePath;
  return `http://localhost:8080/api/images/${imagePath}`;
};

export const getStatusBadge = (status) => {
  const badges = {
    PENDING:   'badge-pending',
    CONFIRMED: 'badge-confirmed',
    CANCELLED: 'badge-cancelled',
    COMPLETED: 'badge-completed',
  };
  return badges[status] || 'bg-secondary';
};

export const truncateText = (text, maxLength = 100) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};

export const getTomorrow = () => {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return tomorrow.toISOString().split('T')[0];
};

export const getDatePlusDays = (days) => {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().split('T')[0];
};
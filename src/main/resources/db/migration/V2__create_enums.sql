CREATE TYPE client_type AS ENUM (
    'WEB_BROWSER',
    'MOBILE_APP'
    );

CREATE TYPE cuisine_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
    );

CREATE TYPE day_of_week AS ENUM (
    'SUNDAY',
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY'
    );

CREATE TYPE delivery_agent_document_type AS ENUM (
    'AADHAR',
    'DRIVING_LICENSE',
    'OTHER'
    );

CREATE TYPE delivery_agent_verification_status as ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
    );

CREATE TYPE document_verification_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
    );

CREATE TYPE order_notification_type AS ENUM (
    'ACCEPTED',
    'CANCELLED',
    'DECLINED',
    'DELIVERED',
    'OUT_FOR_DELIVERY',
    'PLACED',
    'PREPARING',
    'READY_FOR_PICKUP'
    );

CREATE TYPE order_status AS ENUM (
    'ACCEPTED',
    'CANCELLED',
    'DECLINED',
    'DELIVERED',
    'OUT_FOR_DELIVERY',
    'PLACED',
    'PREPARING',
    'READY_FOR_PICKUP'
    );

CREATE TYPE ownership_status AS ENUM (
    'PENDING_VERIFICATION',
    'ACTIVE',
    'REJECTED',
    'TRANSFERRED',
    'EXPIRED'
    );

CREATE TYPE payment_method AS ENUM (
    'UPI',
    'CARD',
    'NET_BANKING',
    'COD',
    'WALLET'
    );

CREATE TYPE payment_notification_type AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED',
    'CANCELLED',
    'REFUNDED'
    );

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED',
    'CANCELLED',
    'REFUNDED'
    );

CREATE TYPE restaurant_document_type AS ENUM (
    'FSSAI_LICENSE',
    'GST',
    'PAN',
    'BANK_PROOF',
    'BUSINESS_LICENSE',
    'OTHER'
    );

CREATE TYPE restaurant_verification_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
    );

CREATE TYPE user_role AS ENUM (
    'ADMIN',
    'CUSTOMER',
    'DELIVERY_AGENT',
    'RESTAURANT_OWNER'
    );

CREATE TYPE vehicle_ownership_document_type AS ENUM (
    'RC',
    'INSURANCE',
    'OTHER'
    );

CREATE TYPE vehicle_type AS ENUM (
    'BIKE',
    'BICYCLE',
    'CAR',
    'SCOOTER'
    );
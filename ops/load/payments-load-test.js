import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 10 },
    { duration: '3m', target: 50 },
    { duration: '2m', target: 0 }
  ]
};

const BASE_URL = __ENV.BASE_URL || 'https://REPLACE_ME_HOST';

export default function () {
  const payload = JSON.stringify({
    orderId: `ORD-${Math.random().toString(36).substring(2, 8)}`,
    userId: `USER-${Math.random().toString(36).substring(2, 8)}`,
    amount: 10.5,
    currency: 'USD',
    paymentMethod: 'CARD',
    cardLast4: '4242'
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': Math.random().toString(36).substring(2, 15)
    }
  };

  const res = http.post(`${BASE_URL}/api/payments`, payload, params);

  check(res, {
    'status is 201': (r) => r.status === 201
  });

  sleep(1);
}


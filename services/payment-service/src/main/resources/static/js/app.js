// API base URLs - same origin when deployed behind ingress, different ports for local dev
const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
const API = {
  payments: isLocal ? 'http://localhost:8080' : '',
  orders: isLocal ? 'http://localhost:8081' : 'https://order-service-5k7f.onrender.com',
  fraud: isLocal ? 'http://localhost:8082' : ''
};

// Fresh idempotency key per page load so repeat "Create payment" tests insert new rows (same key = same payment only).
document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('payment-form');
  if (!form) return;
  const ik = form.querySelector('input[name=idempotencyKey]');
  if (ik && !ik.value.trim()) {
    ik.value = `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  }
});

function showToast(message, type = 'success') {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = `toast show ${type}`;
  setTimeout(() => toast.classList.remove('show'), 3000);
}

function showResult(el, data, isError = false) {
  el.textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
  el.className = 'result-box ' + (isError ? 'error' : 'success');
}

// Tabs
document.querySelectorAll('.tab').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    tab.classList.add('active');
    document.getElementById(tab.dataset.tab).classList.add('active');
  });
});

// Create Payment
document.getElementById('payment-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target;
  const btn = form.querySelector('button[type="submit"]');
  btn.disabled = true;

  const payload = {
    orderId: form.orderId.value,
    userId: form.userId.value,
    amount: parseFloat(form.amount.value),
    currency: form.currency.value || 'USD',
    paymentMethod: form.paymentMethod.value || 'CARD',
    cardLast4: form.cardLast4.value || undefined
  };

  try {
    const res = await fetch(`${API.payments}/api/payments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': form.idempotencyKey.value
      },
      body: JSON.stringify(payload)
    });
    const data = await res.json();

    if (res.ok) {
      showResult(document.getElementById('payment-result'), data);
      showToast(`Payment ${data.paymentId} created`);
    } else {
      showResult(document.getElementById('payment-result'), data, true);
      showToast(data.error || 'Failed to create payment', 'error');
    }
  } catch (err) {
    showResult(document.getElementById('payment-result'), `Error: ${err.message}`, true);
    showToast('Network error - is the payment service running?', 'error');
  }
  btn.disabled = false;
});

// Lookup Payment
document.getElementById('payment-lookup-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = e.target.paymentId.value.trim();
  const resultEl = document.getElementById('payment-result');

  try {
    const res = await fetch(`${API.payments}/api/payments/${encodeURIComponent(id)}`);
    const data = await res.json();

    if (res.ok) {
      showResult(resultEl, data);
    } else {
      showResult(resultEl, res.status === 404 ? 'Payment not found' : data, true);
    }
  } catch (err) {
    showResult(resultEl, `Error: ${err.message}`, true);
    showToast('Network error', 'error');
  }
});

// Create Order
document.getElementById('order-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target;
  const btn = form.querySelector('button[type="submit"]');

  const payload = {
    userId: form.userId.value,
    amount: parseFloat(form.amount.value),
    currency: form.currency.value || 'USD'
  };

  btn.disabled = true;
  try {
    const res = await fetch(`${API.orders}/api/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();

    if (res.ok) {
      showResult(document.getElementById('order-result'), data);
      showToast(`Order ${data.orderId} created`);
    } else {
      showResult(document.getElementById('order-result'), data, true);
      showToast('Failed to create order', 'error');
    }
  } catch (err) {
    showResult(document.getElementById('order-result'), `Error: ${err.message}`, true);
    showToast('Network error - is the order service running?', 'error');
  }
  btn.disabled = false;
});

// Lookup Order
document.getElementById('order-lookup-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = e.target.orderId.value.trim();
  const resultEl = document.getElementById('order-result');

  try {
    const res = await fetch(`${API.orders}/api/orders/${encodeURIComponent(id)}`);
    const data = await res.json();

    if (res.ok) {
      showResult(resultEl, data);
    } else {
      showResult(resultEl, res.status === 404 ? 'Order not found' : data, true);
    }
  } catch (err) {
    showResult(resultEl, `Error: ${err.message}`, true);
    showToast('Network error', 'error');
  }
});

// Fraud Rules
document.getElementById('load-fraud-rules').addEventListener('click', async () => {
  const resultEl = document.getElementById('fraud-result');

  try {
    const res = await fetch(`${API.fraud}/api/fraud/rules`);
    const data = await res.json();

    if (res.ok) {
      showResult(resultEl, data);
      showToast('Rules loaded');
    } else {
      showResult(resultEl, data, true);
    }
  } catch (err) {
    const fallback = {
      strategy: 'simple-threshold',
      maxAmountWithoutReview: 1000,
      version: 'v1',
      note: 'Fraud service not reachable - showing default rules. Run fraud-service on port 8082 for live data.'
    };
    showResult(resultEl, fallback);
    showToast('Fraud service not running - showing default rules', 'error');
  }
});

-- Run in Supabase → SQL Editor if inserts from the Java apps do not appear.

-- 1) Confirm tables exist
SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename IN ('payments', 'orders');

-- 2) Row Level Security: if ON and no policy allows INSERT, apps can fail (depends on role).
SELECT schemaname, tablename, rowsecurity AS rls_enabled
FROM pg_tables
WHERE schemaname = 'public' AND tablename IN ('payments', 'orders');

-- 3) If RLS is on and you want the backend (postgres connection string) to work without policies, turn RLS off:
-- ALTER TABLE public.payments DISABLE ROW LEVEL SECURITY;
-- ALTER TABLE public.orders DISABLE ROW LEVEL SECURITY;

-- 4) Manual insert test (should return 1 row in Table Editor afterward)
-- INSERT INTO public.payments (payment_id, order_id, user_id, amount, currency, status, idempotency_key)
-- VALUES ('TEST-PAY-1', 'ORD-TEST', 'USER-1', 1.00, 'USD', 'PENDING', 'idem-test-1');

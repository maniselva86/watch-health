create table readings (
  id bigint generated always as identity primary key,
  metric text not null check (metric in ('heart_rate','steps')),
  value double precision not null,
  ts timestamptz not null default now()
);
create index on readings (ts);
alter table readings enable row level security;
-- watch (public key) can only add rows
create policy "watch insert" on readings for insert to anon with check (true);
-- only a signed-in user (you) can read
create policy "owner read" on readings for select to authenticated using (true);

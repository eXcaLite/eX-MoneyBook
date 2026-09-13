-- eX Money Book Sync - Supabase setup V2
-- Safe to run again.

create table if not exists public.money_transactions (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  txn_date date not null,
  txn_type text not null,
  category text not null default '',
  amount numeric(18,2) not null check (amount >= 0),
  note text not null default '',
  updated_at timestamptz not null default now(),
  deleted boolean not null default false
);

create index if not exists money_transactions_user_updated_idx
  on public.money_transactions(user_id, updated_at);

alter table public.money_transactions enable row level security;

grant usage on schema public to authenticated;
grant select, insert, update, delete
on table public.money_transactions
to authenticated;

drop policy if exists "money_select_own" on public.money_transactions;
create policy "money_select_own"
on public.money_transactions for select
using (auth.uid() = user_id);

drop policy if exists "money_insert_own" on public.money_transactions;
create policy "money_insert_own"
on public.money_transactions for insert
with check (auth.uid() = user_id);

drop policy if exists "money_update_own" on public.money_transactions;
create policy "money_update_own"
on public.money_transactions for update
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

drop policy if exists "money_delete_own" on public.money_transactions;
create policy "money_delete_own"
on public.money_transactions for delete
using (auth.uid() = user_id);

create or replace function public.set_money_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_money_updated_at on public.money_transactions;
create trigger trg_money_updated_at
before insert or update on public.money_transactions
for each row execute function public.set_money_updated_at();

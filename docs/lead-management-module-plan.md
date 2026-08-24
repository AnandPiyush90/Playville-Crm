# Lead Management and Paid-Ads Attribution Plan

This document is the product and technical specification for a PlayVille **lead
management** module: capture interest from Instagram, Facebook (Meta), and Google
ads, keep first-touch attribution, match those people when they visit or buy, and
give Admin a monthly dashboard that is good enough to decide **where to spend more**.

It extends (does not replace) today’s enquiry and customer `lead_source` fields.
Related: [Reporting Module Plan](reporting-module-plan.md) CRM reports, [Trial
Conversion Plan](trial-conversion-implementation-plan.md).

Implementation is deferred. This file is the design contract.

---

## 1. Goal

At month end, Admin should answer:

- How many **leads** did each platform and campaign produce?
- How many of those **visited** (trial or paid check-in)?
- How many **bought** a package / paid invoice (true conversion)?
- What **revenue** did first purchases (and optionally 30-day LTV) bring?
- What was **cost per lead, cost per purchase, and ROAS** (needs ad spend)?

That is not the same as “how many customers ticked Instagram at the desk.” Desk
staff routinely mark `Walk_in` even when the parent came from an ad.

---

## 2. Verdict on the matching idea

**Matching a stored lead to a customer by normalised phone, then email, when
onboarding or first purchase happens is the right conversion rule** for ads that
already captured contact details (Meta Lead Ads, Google Lead Form Extensions,
website enquiry form, WhatsApp with a phone).

Do **not** try to pull “everyone who showed interest” (video views, likes,
comments, profile visits). Those events have no reliable phone/email, are noisy
for spend decisions, and are a privacy problem.

Better model:

1. **Ingest only identifiable leads** (name + phone and/or email + campaign ids).
2. **Lock first paid touch** on that lead. Never overwrite it when staff later
   select Walk-in.
3. **Progress the same lead** through visit and purchase; do not create a second
   “Walk_in customer” with a blank campaign.
4. **Import campaign spend** (CSV is enough at first). Conversion *rate* without
   spend cannot tell you where to burn more money; **CPA and ROAS** can.
5. Optionally **upload offline purchases** back to Meta/Google so the ad platforms
   themselves optimise (Conversions API / Enhanced Conversions). That is more
   valuable than a CRM-only dashboard.

---

## 3. What the CRM does today (and why it is not enough)

| Today | Limitation for ads |
|---|---|
| `LeadSource` enum: `Walk_in`, `Google`, `Instagram`, `Facebook`, `Friend_Referral`, `School`, `Other` | Channel only. No campaign, ad set, ad, or paid vs organic. |
| `pv_enquiries.lead_source` | Staff-entered; no UTM, no Meta/Google lead id. |
| `pv_customers.lead_source` | Copied at create; can be wrong Walk-in. |
| Enquiry convert matches **exact phone** and copies source onto a **new** customer | Good seed. Does not run when family is onboarded without converting the enquiry. Does not match email. Does not attach campaign. |
| `pv_customer_entitlements.campaign_code` | Trial promo code (`FIRST_VISIT`), not an ads campaign. |
| Trial reports filter by customer `lead_source` | Same staff-attribution problem. |

There is **no** ads webhook, **no** spend table, **no** click ids (`gclid` /
`fbclid`), and **no** first-touch lock.

---

## 4. Lead types you actually run

Treat these as different **capture methods**. All become one `Lead` row.

| Method | What Meta/Google give you | How it enters PlayVille |
|---|---|---|
| **A. Lead form ads** (Instagram/Facebook Lead Ads, Google lead forms) | Name, phone, email, campaign/ad ids, platform lead id | Webhook into CRM (best automatic path) |
| **B. Click to WhatsApp / call** | Often click id only; phone appears when they message or you call | WhatsApp Business / staff paste; store campaign from the ad’s unique number or pre-filled message |
| **C. Landing page / website form** | UTMs + form PII | Public enquiry form posts into CRM with UTM query params |
| **D. Walk-in / call-in after seeing an ad** | Nothing unless you ask | Staff **must** pick platform + campaign from a list, not free text. Matching later still works if they already submitted a lead form. |
| **E. Organic Instagram / Google Maps** | No ad id | Separate from paid: `organic` medium so paid budget is not credited |

For **A and C**, automatic ingest + phone match is excellent.  
For **B and D**, ingest still helps, but staff process and unique campaign tracking
links matter as much as matching.

---

## 5. Attribution rules (lock these before build)

1. **Identity:** one lead per normalised phone per branch (email is secondary match
   if phone missing or conflicts).
2. **Phone normalisation:** digits only, last 10 digits for Indian mobiles, store
   E.164 (`91xxxxxxxxxx`) as canonical. Same helper as `EnquiryService.normalizePhone`,
   extended.
3. **First-touch wins** for paid attribution: if a Meta lead exists, a later walk-in
   onboarding must **link** that customer to the lead and **keep** `Facebook` /
   campaign ids.
4. **Attribution window:** first purchase within **90 days** of lead created counts
   as a paid conversion (configurable; 30 days is too short for a kids’ play centre).
   Visit/trial can count inside the same window.
5. **Conversion events (count each once per lead):**
   - `LEAD_CAPTURED`
   - `CONTACTED` (optional, staff)
   - `VISIT` first check-in (trial or paid)
   - `PURCHASE` first paid membership/invoice (`grand_total` > 0, not voided)
   - `REVENUE` amount of that first purchase; optional `LTV_30D` later
6. **Do not count** recharge of an existing member as a *new* ad conversion.
   Attribute only **new families** (no prior paid purchase).
7. **Duplicates:** same Meta `leadgen_id` / Google resource name ingested twice is
   idempotent. Same phone from two campaigns: keep first paid touch; record later
   touches as `touch` history, not a second conversion.
8. **Staff cannot change** `attribution_channel` / campaign ids after capture except
   Admin correction with audit.

---

## 6. Recommended data model

Keep `pv_enquiries` as the **staff work queue** (call, schedule visit). Introduce a
richer lead record that enquiries, webhooks, and onboarding all attach to.

### 6.1 `pv_leads`

| Column | Purpose |
|---|---|
| `id` | PK |
| `branch_id` | Centre the ad or form targeted |
| `enquiry_id` | Optional link to existing enquiry |
| `customer_id` | Set when matched |
| `parent_name`, `phone_e164`, `phone_digits`, `email` | Identity |
| `channel` | `GOOGLE`, `META_FACEBOOK`, `META_INSTAGRAM`, `WALK_IN`, `REFERRAL`, `SCHOOL`, `OTHER` |
| `medium` | `PAID_LEAD_FORM`, `PAID_CLICK`, `ORGANIC`, `WALK_IN`, `REFERRAL`, `MANUAL` |
| `campaign_id`, `campaign_name` | Ads campaign |
| `adset_id`, `adset_name`, `ad_id`, `ad_name` | Optional granularity |
| `utm_source`, `utm_medium`, `utm_campaign`, `utm_content`, `utm_term` | Web forms |
| `gclid`, `fbclid` | Click ids when present |
| `external_source` | `META`, `GOOGLE`, `WEB`, `STAFF` |
| `external_lead_id` | Platform lead id (unique with source) |
| `status` | `NEW`, `CONTACTED`, `VISIT_SCHEDULED`, `VISITED`, `PURCHASED`, `LOST`, `DUPLICATE` |
| `lost_reason` | Optional |
| `first_touch_at` | Immutable |
| `matched_at`, `visited_at`, `purchased_at` | Funnel clocks |
| `first_purchase_amount`, `first_purchase_id` / `first_invoice_id` | Conversion value |
| `match_method` | `PHONE`, `EMAIL`, `MANUAL` |
| `raw_payload_json` | Webhook snapshot (no secrets) |
| `idempotency_key` | Staff/manual create |

Indexes: `(branch_id, phone_digits)`, unique `(external_source, external_lead_id)`,
`(customer_id)`, `(first_touch_at)`.

### 6.2 `pv_lead_touches`

Append-only: every ingest, staff contact, visit, purchase. Supports later
last-touch analysis without changing first-touch reports.

### 6.3 `pv_ad_campaigns` (catalogue)

Known campaigns for staff dropdowns and spend join: platform, external campaign id,
name, branch (or network-wide), active flag.

### 6.4 `pv_ad_spend`

| Column | Purpose |
|---|---|
| `period_month` | `2026-08-01` |
| `branch_id` nullable | Null = org-level spend |
| `channel` | Google / Meta Instagram / Meta Facebook |
| `campaign_id` | Join to leads |
| `spend_amount`, `currency` | INR |
| `impressions`, `clicks`, `platform_leads` | Optional, from ads UI export |
| `source` | `CSV`, `API` |
| `imported_by_staff_id` | Audit |

Without this table, the dashboard can show **conversion rates** only, not
**whether a campaign is worth more budget**.

### 6.5 Existing tables (additive)

- Enquiry: `lead_id`, campaign/UTM columns (or enquiry *is* a view on lead).
- Customer: `first_lead_id`; **do not overwrite** `lead_source` once set from a lead.
- Onboarding: if phone matches an open lead, auto-fill source and attach.

---

## 7. Ingestion

### 7.1 Meta Lead Ads (Instagram + Facebook)

1. Create a Meta App; subscribe Page to `leadgen`.
2. Public `POST /public/webhooks/meta/leads` (signature verification, challenge
   handshake). Authenticated staff APIs stay private.
3. On event: fetch lead from Graph API (or use filled fields if sent), map form
   fields to name/phone/email, store campaign/ad ids, create or merge `pv_leads`.
4. Create or update `pv_enquiries` in `NEW` so the centre can call them.
5. Idempotency on `leadgen_id`.

Instagram vs Facebook is the ad’s `publisher_platform` / placement, not a guess
from staff.

### 7.2 Google Ads lead forms

Google Lead Form Extensions / Local Services can POST to a webhook. Same merge
rules; `external_source = GOOGLE`, store `gclid` when present.

Search/Performance Max **clicks** without a form do not create a lead until the
parent submits a form, WhatsApps, or walks in with UTMs.

### 7.3 Website / landing page

Public `POST /public/enquiries` (rate-limited, honeypot, optional recaptcha):

```json
{
  "branchCode": "PV-HSR",
  "parentName": "...",
  "phoneNumber": "...",
  "email": "...",
  "utmSource": "instagram",
  "utmMedium": "paid",
  "utmCampaign": "aug-trial-hsr",
  "fbclid": "...",
  "gclid": "..."
}
```

Map UTM source to channel; never default to Walk-in.

### 7.4 Staff desk

Keep current enquiry create, but require `channel` and, for paid channels, a
**campaign picker** from `pv_ad_campaigns`. Optional “matched existing lead”
banner when phone already exists.

### 7.5 What not to ingest automatically

- Page likes, reel views, comments, shares.
- Full Messenger/Instagram DM transcripts until a phone is known.
- Custom Audiences / pixel events without PII (use those inside Meta, not CRM).

---

## 8. Match when they become a customer or pay

Run a single `LeadMatchingService` from:

- Enquiry convert (already phone-based)
- Customer onboarding complete
- Customer create by phone
- First paid purchase / invoice finalisation

Algorithm:

1. Normalise inbound phone and email.
2. Find unmatched (or same-customer) leads in attribution window, same branch
   first, then other branches (Admin policy: prefer home branch).
3. Precedence: exact `phone_digits` → email (case-insensitive) → none.
4. If match: set `customer_id`, `matched_at`, `match_method`; copy locked
   `lead_source` onto customer **only if customer source is null or Walk_in**.
5. On first qualifying purchase: set `PURCHASED`, amount, invoice/purchase id.
6. Ambiguous matches (two different phones, same email): do not auto-merge;
   queue `NEEDS_REVIEW`.

This is exactly the idea you described, with first-touch lock and a time window
so a 2024 Instagram lead is not credited for a 2026 walk-in.

---

## 9. Monthly ads dashboard (the decision screen)

API sketch (admin/manager, same `/reports` auth):

`GET /reports/leads/attribution?from=2026-08-01&to=2026-08-31&branchId=&channel=&groupBy=channel|campaign`

### 9.1 Rows

Group by **month + channel**, with drill-down to **campaign**.

| Metric | Definition |
|---|---|
| Spend | Sum `pv_ad_spend` for that month/channel/campaign |
| Leads | Leads with `first_touch_at` in month (or leads *created* in month — pick one and keep it; recommend **lead created in month**) |
| CPL | Spend / leads |
| Visited | Distinct leads with `visited_at` in window (or visit within 90d of lead — **report both** as “same month” vs “cohort”) |
| Purchased | Distinct leads with first purchase in window / cohort |
| Visit rate | Visited / leads |
| Purchase rate | Purchased / leads |
| Revenue | Sum first_purchase_amount for those conversions |
| CPA | Spend / purchased |
| ROAS | Revenue / spend |

**Cohort view (recommended for budget):** August campaigns → how many of *those*
leads visited/bought in the next 90 days, even if purchase is in September.

**Calendar view:** purchases in August, attributed to original campaign (can mix
months). Show both; label them.

### 9.2 Charts

- Conversion rate by channel (Instagram paid / Facebook paid / Google / organic / walk-in / referral)
- CPA by campaign (hide campaigns with < N leads to avoid noise)
- Funnel: leads → contacted → visited → purchased
- Month-over-month sparkline

### 9.3 Guardrails so you do not over-invest

- Do not raise budget on a campaign with **< 30 leads** or **< 5 purchases**;
  rates are noise.
- Separate **Instagram organic** from **Instagram paid**.
- Show **new-family purchases only**.
- If spend is missing, show conversion rates and a banner: “CPA/ROAS unavailable”.

---

## 10. Staff lead inbox (operations, not ads)

Extend the enquiry UI rather than a second CRM:

- List leads/enquiries: New, today, overdue follow-up
- Click-to-call / WhatsApp deep link
- Status + lost reason
- “This phone already has a Meta lead from campaign X” warning
- Convert / onboard without asking source again

Lost reasons should be a small enum (price, distance, age, no show, duplicate),
not only free text.

---

## 11. Privacy and compliance

- Store webhook payloads without access tokens.
- Marketing consent on the form; do not WhatsApp blast unmatched lead lists.
- Meta/Google lead data is for fulfilment; follow each platform’s terms.
- Public webhooks: verify signatures, reject unsigned bodies.
- PII in reports: aggregates on the dashboard; detail lists paged and role-gated.

---

## 12. Delivery phases

### Phase L1 — Attribution inside CRM (no ads API yet)

Highest ROI. Stops Walk-in from wiping ads.

- Add campaign + UTM + medium on enquiry (and customer first-touch lock).
- Staff campaign dropdown (seed campaigns manually).
- Match enquiry/onboarding/purchase by phone (and email).
- Cohort report: leads / visits / purchases / revenue by channel and campaign
  **without spend** (conversion quality).

Exit: month-end you can see “Instagram campaign X produced 80 leads, 20 visits,
8 packages” even if spend is still in Ads Manager.

### Phase L2 — Automatic lead forms

- Meta Lead Ads webhook + Google lead form webhook.
- Idempotent `external_lead_id`.
- Auto-create enquiry for the target branch.

Exit: form fills appear in the inbox without typing.

### Phase L3 — Spend and money dashboard

- CSV import of monthly spend by campaign (date, channel, campaign id, INR).
- Optional later: Meta Insights + Google Ads API pull.
- Dashboard: CPL, CPA, ROAS.

Exit: Admin can justify shifting budget.

### Phase L4 — Close the loop with the ad platforms

- Meta Conversions API / Google Enhanced Conversions: send `Purchase` with hashed
  phone/email when first package is paid.
- Platforms then optimise for buyers, not just form fills.

### Phase L5 — Optional

- WhatsApp click-to-lead uniqueness
- Pixel + server events for website
- Multi-touch reports
- Branch-level campaign geo

---

## 13. Suggested API surface (when built)

```text
POST /public/webhooks/meta/leads
POST /public/webhooks/google/leads
POST /public/enquiries

GET  /leads
GET  /leads/{id}
POST /leads                      (staff)
PATCH /leads/{id}/status

POST /admin/ad-campaigns
POST /admin/ad-spend/import

GET /reports/leads/attribution
GET /reports/leads/funnel
GET /reports/leads/conversions     (paged, like trial-conversions)
```

Keep `/enquiries` working; create enquiry from lead or add `lead_id` on enquiry
so the inbox stays one list.

---

## 14. Mapping to reporting catalogue

| Reporting ID | After this module |
|---|---|
| CRM-01–03 | Driven by `pv_leads` funnel, not only enquiry status |
| CRM-02 | Split paid vs organic; add campaign dimension |
| New ADS-01 | Monthly/cohort attribution dashboard (§9) |
| New ADS-02 | Spend vs CPA/ROAS |
| New ADS-03 | Ambiguous match review queue |

---

## 15. Out of scope for v1

- Scraping Instagram comments or “everyone who viewed a reel”.
- Building a full marketing automation suite (drip campaigns, journeys).
- Real-time bidding or budget changes inside CRM.
- Counting membership recharges as ad conversions.

---

## 16. Recommended start

Do **not** start with Meta API. Start with **Phase L1**: campaign on the enquiry,
first-touch lock, phone match at onboarding and first purchase, then the monthly
cohort table. That already beats Ads Manager form-fill counts because it uses
**PlayVille money** (invoice/purchase) as the conversion.

Add webhooks (L2) once the form field mapping and branch targeting are clear.
Add spend import (L3) before any “increase Instagram 20%” decision.

# BinItRight Tier-1 + Tier-2 Scan Service Engineering Specification

Version: v0.1 (Contract Baseline)
Audience: Android Client, ML (On-device + Server), Backend/Platform, QA/DevOps

## 1. Scope and goals

This document specifies the end-to-end engineering contract and runtime behavior for the “Scan Item” pipeline with a two-tier prediction strategy:

* **Tier-1 (on-device):** fast, low-cost, runs on Android via ONNX Runtime. Outputs one of the fixed Tier-1 labels plus confidence signals (top3, escalate).
* **Tier-2 (server expert):** slower, higher cost, invoked only when Tier-1 is uncertain or escalated. Tier-2 may return refined categories beyond Tier-1 labels, and must return disposal instructions that are actionable.

**Primary outcomes required by the mobile UI:**
a) User-facing category name (short)
b) Recyclable boolean
c) Confidence score (0–1) to support “scan again” prompts
d) A prominent single instruction plus optional step-by-step instructions
e) Optional disposal method / bin type for clearer UX

**Non-goals (v0.1):**
a) Location-based disposal point lookup
b) Personalization (user profiles, habits)
c) Multi-item detection (more than one object per image)
d) Long-running chat dialogue (Tier-2 is “expert decision”, not full chatbot)

## 2. System overview and architecture

### 2.1 External view (stable for Android)

Android sends one request to a single endpoint:

`POST /api/v1/scan` (multipart/form-data)

Server returns a single JSON response that always contains final UI-ready fields, regardless of whether Tier-2 was invoked.

### 2.2 Internal logical modules (implementation may vary)

A reference decomposition that allows incremental evolution:

1. **Scan API (Gateway)**
2. **Tier-1 Resolver**
    * If Android provides Tier-1 results: use them
    * Else: run Tier-1 on server for parity/QA/fallback
3. **Decision Engine**
    * Applies escalation rules (confidence, margin, predicted other_uncertain, etc.)
4. **Tier-2 Expert Adapter (optional path)**
    * Calls LLM or expert model endpoint
    * Enforces strict JSON schema and post-validation
5. **Instruction Composer**
    * Normalizes outputs to the final contract
6. **Response Assembler**
    * Returns tier1/decision/final/followup/meta

### 2.3 Deployment topology options

* **Option A (fastest, minimal components):** Android → Python FastAPI (Tier-1 optional fallback + Tier-2 adapter)
* **Option B (more “enterprise”):** Android → Spring Boot (auth/limits/logging) → Python service (Tier-1/Tier-2)
* **Option C (hybrid):** Spring Boot calls Tier-2 only; Tier-1 remains strictly on-device

The external contract in this document is identical for A/B/C.

## 3. Tier-1 model contract (source-of-truth alignment)

### 3.1 Fixed Tier-1 labels

Tier-1 labels are fixed and must match the canonical label map used for ONNX inference. Current list (as per ML documentation):

* paper
* plastic
* metal
* glass
* e-waste
* textile
* other_uncertain

**Important:** the exact spelling and ordering are the source of truth in `label_map.json` shipped with the model. No server-side “correction” is assumed.

### 3.2 Tier-1 output semantics

Tier-1 produces:

* `category`: top-1 label from the fixed set
* `confidence`: top-1 probability (0–1)
* `top3`: ordered list of top-3 labels with probabilities
* `escalate`: boolean indicating “uncertain/complex, needs Tier-2”

Tier-1 may also produce debug signals (margin, entropy, multicrop status). These are valuable for QA and threshold tuning but are not required by the mobile UI.

## 4. Public API specification (Android-facing)

### 4.1 Endpoint

`POST /api/v1/scan`

* **Content-Type:** `multipart/form-data`
* **Protocol:** HTTP/1.1 or HTTP/2

### 4.2 Request fields

**image (required)**

* Type: binary file
* Formats: JPEG or PNG (JPEG recommended)
* Size limits (recommended): max 8 MB; server returns 413 on violation

**timestamp (optional)**

* Type: Long
* Unit: Unix milliseconds recommended (documented; server should accept missing)

**tier1 (optional)**

* Type: string (JSON)
* Purpose: pass on-device Tier-1 results to the server to skip duplicate inference and help Tier-2 reasoning

**Recommended tier1 JSON schema:**

```json
{
  "category": "plastic",
  "confidence": 0.91,
  "top3": [
    {"label":"plastic","p":0.91},
    {"label":"glass","p":0.05},
    {"label":"other_uncertain","p":0.02}
  ],
  "escalate": false
}
```

**Notes on compatibility:**

* This schema is compatible with the ML team’s Tier-1 server response fields and the Android Tier-1 runtime representation.
* If `tier1` is not provided, the server may run Tier-1 (optional fallback) and still populate `tier1` in the response.

### 4.3 Success response (HTTP 200)

**Top-level envelope:**

```json
{
  "status": "success",
  "request_id": "uuid-string",
  "data": { ... }
}
```

**data fields:**

* `tier1` (optional, object or null): echoes Tier-1 results (from Android or server fallback)
* `decision` (required): indicates whether Tier-2 was used and why
* `final` (required): UI-ready final decision and instructions
* `followup` (optional but recommended): question set when still uncertain
* `meta` (optional): schema/model versions, latency

#### 4.3.1 data.final (UI-critical contract)

```json
{
  "category": "PET Plastic Bottle",
  "category_id": "plastic.pet_bottle",
  "recyclable": true,
  "confidence": 0.93,
  "instruction": "Rinse and empty containers.",
  "instructions": [
    "Empty all contents.",
    "Rinse to remove food residue.",
    "If heavily contaminated, dispose as general waste."
  ],
  "disposal_method": "Blue Recycling Bin",
  "bin_type": "blue",
  "rationale_tags": ["looks_like_bottle"]
}
```

**Field semantics:**

* `category` (Critical): short, user-facing item name (Tier-2 may refine beyond Tier-1 labels)
* `category_id` (Recommended): machine-readable taxonomy id; supports Tier-2 expansion without breaking UI
* `recyclable` (Critical): boolean used by the app’s primary success/fail display logic
* `confidence` (Critical): 0–1 “system confidence” (may be post-processed; not necessarily Tier-1 or LLM raw value)
* `instruction` (Critical): one-line primary instruction shown on the result card
* `instructions` (Recommended): 2–6 short imperative steps; warnings/conditions should be embedded here (no separate warnings field in v0.1)
* `disposal_method` (Optional): user-facing disposal channel text (e.g., “Blue Recycling Bin”, “General Waste”, “E-waste Collection Point”)
* `bin_type` (Optional): normalized channel enum for UI/analytics (blue|general|ewaste|textile|special|unknown)
* `rationale_tags` (Optional): <=3 tags for debugging/explainability (non-sensitive, no long text)

#### 4.3.2 data.decision (Tier-2 usage transparency)

```json
{
  "used_tier2": false,
  "reason_codes": [],
  "thresholds": {
    "conf_threshold": 0.70,
    "margin_threshold": 0.12
  }
}
```

* `used_tier2` (required): true if Tier-2 expert was called and its output affected final
* `reason_codes` (optional): e.g., LOW_CONFIDENCE, LOW_MARGIN, PRED_OTHER_UNCERTAIN
* `thresholds` (optional): echoed tuning values for observability and experiment tracking

#### 4.3.3 data.followup (uncertainty handling as UX)

When the system cannot confidently decide, it should provide follow-up questions instead of returning an unusable answer.

```json
{
  "needs_confirmation": true,
  "questions": [
    {
      "id": "q1",
      "type": "single_choice",
      "question": "Is there food residue?",
      "options": ["Yes","No","Not sure"]
    }
  ]
}
```

#### 4.3.4 data.tier1 (echoed for parity/debug)

```json
{
  "category": "other_uncertain",
  "confidence": 0.41,
  "top3": [
    {"label":"other_uncertain","p":0.41},
    {"label":"plastic","p":0.33},
    {"label":"paper","p":0.12}
  ],
  "escalate": true
}
```

#### 4.3.5 data.meta (optional)

```json
{
  "schema_version": "0.1",
  "model_versions": {"tier1":"onnx_v1.0.0","tier2":"llm_v0.1.0"},
  "latency_ms": {"total": 2650, "tier2": 1900}
}
```

## 5. Tier-2 expert behavior (LLM / expert model)

### 5.1 When Tier-2 is invoked

Tier-2 is invoked when Tier-1 indicates uncertainty. Typical triggers:

* `tier1.escalate == true`
* `tier1.category == "other_uncertain"`

Additional server-side uncertainty checks (confidence/margin/entropy) may also trigger Tier-2.
Android does not implement these rules; it only consumes final outputs.

### 5.2 Tier-2 output requirements

Tier-2 may output refined categories beyond the Tier-1 label set. However, it must provide:

* `category` (short user-facing)
* `category_id` (taxonomy id; recommended)
* `recyclable` boolean
* `instruction` + `instructions` (actionable disposal steps; include conditional steps like “if food residue then rinse”)
* optional: `bin_type`/`disposal_method`

If Tier-2 remains uncertain, it must return followup questions rather than guessing.

### 5.3 Strict schema enforcement

Tier-2 adapter must:

* require JSON-only responses from the expert model
* validate against a strict schema
* reject/repair outputs that violate schema (fallback to “uncertain + followup”)

## 6. Error handling contract

### 6.1 Error response (non-200)

```json
{
  "status": "error",
  "code": "INVALID_IMAGE",
  "message": "User-friendly error message."
}
```

**Recommended HTTP status codes:**

* 400 INVALID_IMAGE, MISSING_IMAGE
* 413 IMAGE_TOO_LARGE
* 415 UNSUPPORTED_MEDIA_TYPE
* 429 RATE_LIMITED
* 503 TIER2_UNAVAILABLE
* 504 TIER2_TIMEOUT
* 500 INTERNAL_ERROR

### 6.2 Recommended graceful degradation (Tier-2 failure)

If Tier-2 is unavailable or times out, the system should prefer returning HTTP 200 with:

* `decision.used_tier2=false`
* `final.category` = “Uncertain” (or similar)
* `followup.needs_confirmation=true` with 2–3 targeted questions

This avoids blocking the user flow and supports a consistent UI path.

## 7. Security and secrets management

### 7.1 Client-side secrets

API keys for Tier-2 providers must not be embedded in Android APK. Obfuscation does not provide real secrecy against reverse engineering and traffic replay.

### 7.2 Server-side secrets

Tier-2 provider keys must be stored server-side as environment variables or secret manager entries. The scan service performs the Tier-2 call on behalf of the client.

### 7.3 Basic protection recommendations

* Rate limits per IP/user/session at the scan service boundary
* Request size limits
* Response caching keyed by image hash (optional) to reduce repeated Tier-2 costs
* Structured logging with `request_id` correlation

## 8. Observability and QA

### 8.1 Logging (recommended fields)

`request_id`, `timestamp`, `image_size`, `tier1.category`, `tier1.confidence`, `tier1.escalate`, `used_tier2`, `tier2_latency`, `final.bin_type`, `final.category_id`, `error_code` (if any)

### 8.2 Metrics

* Tier-2 invocation rate
* Tier-2 timeout rate
* End-to-end latency percentiles
* Distribution of final categories and “uncertain + followup” rate

### 8.3 Parity testing

Maintain a parity check between Android preprocessing/inference and server fallback inference to detect drift. This aligns with the ML team’s parity self-test approach.

## 9. Versioning and backward compatibility

### 9.1 Schema version

`meta.schema_version` indicates contract version.
v0.1 guarantees that `data.final` critical fields remain stable.

### 9.2 Forward-compatible additions

New optional fields may be added without breaking Android if Android ignores unknown fields.
Avoid renaming/removing any field marked Critical in `data.final`.

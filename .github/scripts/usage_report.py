#!/usr/bin/env python3
"""Reports the last 24h of Cloud Run/Cloud Build usage for karata0, plus real billing cost once
the BigQuery billing export (enabled 2026-09-09) has started populating data - it can take
several hours to appear after being enabled, and never backfills usage from before that, so early
reports are expected to just show the usage-proxy numbers until then.

Auth: a short-lived access token from Workload Identity Federation (see usage-report.yml), read
via ACCESS_TOKEN - the service account behind it (github-actions-reporter@karata0) only has
read-only monitoring/build/BigQuery roles, nothing that can modify infrastructure.
"""

import json
import os
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone

ACCESS_TOKEN = os.environ["ACCESS_TOKEN"]
PROJECT = "karata0"
REGION = "southamerica-east1"
SERVICE = "preprod-karata"
BQ_DATASET = "karata0_dataset"

# Our actual deployed config (see deploy-gcloud.sh / the gcloud run deploy calls): 1 vCPU, 1GiB
# memory, request-based billing (no --no-cpu-throttling / always-allocated CPU set), so Cloud Run
# only bills CPU+memory while actually processing a request - exactly what billable_instance_time
# measures. Published on-demand list prices (not this project's actual negotiated/discounted
# rate, doesn't account for the monthly free tier, and doesn't vary these by region even though
# real prices do) - always good enough for an order-of-magnitude estimate, not a substitute for
# the real BigQuery billing export once that has data.
CLOUD_RUN_VCPU_SECOND = 0.000024
CLOUD_RUN_MEMORY_GIB_SECOND = 0.0000025
CLOUD_RUN_MILLION_REQUESTS = 0.40
CLOUD_RUN_VCPUS = 1
CLOUD_RUN_MEMORY_GIB = 1
CLOUD_BUILD_MINUTE = 0.003  # default E2_MEDIUM machine type


def api(url, method="GET", body=None):
    headers = {"Authorization": f"Bearer {ACCESS_TOKEN}"}
    data = None
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())


def cloud_build_minutes():
    since = datetime.now(timezone.utc) - timedelta(hours=24)
    status, body = api(
        f"https://cloudbuild.googleapis.com/v1/projects/{PROJECT}/locations/{REGION}/builds"
        "?pageSize=100"
    )
    if status != 200:
        return None, None, f"failed to list ({status}: {body})"
    since_str = since.strftime("%Y-%m-%dT%H:%M:%SZ")
    recent = [b for b in body.get("builds", []) if b.get("createTime", "") >= since_str]
    total_seconds = 0.0
    for b in recent:
        ct, ft = b.get("createTime"), b.get("finishTime")
        if ct and ft:
            t0 = datetime.strptime(ct[:19], "%Y-%m-%dT%H:%M:%S")
            t1 = datetime.strptime(ft[:19], "%Y-%m-%dT%H:%M:%S")
            total_seconds += (t1 - t0).total_seconds()
    return len(recent), total_seconds / 60.0, None


def _monitoring_sum(metric_type):
    now = datetime.now(timezone.utc)
    since = now - timedelta(hours=24)
    params = {
        "filter": (
            f'metric.type="{metric_type}" '
            'resource.type="cloud_run_revision" '
            f'resource.label.service_name="{SERVICE}"'
        ),
        "interval.startTime": since.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "interval.endTime": now.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "aggregation.alignmentPeriod": "86400s",
        "aggregation.perSeriesAligner": "ALIGN_SUM",
        "aggregation.crossSeriesReducer": "REDUCE_SUM",
    }
    url = (
        f"https://monitoring.googleapis.com/v3/projects/{PROJECT}/timeSeries?"
        + urllib.parse.urlencode(params)
    )
    status, body = api(url)
    if status != 200:
        return None, f"failed to query ({status}: {body})"
    total = 0.0
    for s in body.get("timeSeries", []):
        for p in s.get("points", []):
            v = p["value"]
            total += float(v.get("int64Value", v.get("doubleValue", 0)))
    return total, None


def cloud_run_requests():
    return _monitoring_sum("run.googleapis.com/request_count")


def cloud_run_billable_seconds():
    return _monitoring_sum("run.googleapis.com/container/billable_instance_time")


def find_billing_table():
    status, body = api(
        f"https://bigquery.googleapis.com/bigquery/v2/projects/{PROJECT}/datasets/{BQ_DATASET}/tables"
    )
    if status != 200:
        return None
    for t in body.get("tables", []):
        table_id = t["tableReference"]["tableId"]
        if table_id.startswith("gcp_billing_export"):
            return table_id
    return None


def report_billing():
    table = find_billing_table()
    if table is None:
        return (
            "- **Billing**: BigQuery export is enabled but no table has appeared yet "
            "(can take several hours after enabling, and never backfills usage from before "
            "it was enabled) - check a later report.\n"
        )
    since = datetime.now(timezone.utc) - timedelta(hours=24)
    query = (
        "SELECT SUM(cost) AS total_cost, currency "
        f"FROM `{PROJECT}.{BQ_DATASET}.{table}` "
        f"WHERE usage_start_time >= TIMESTAMP('{since.strftime('%Y-%m-%dT%H:%M:%SZ')}') "
        "GROUP BY currency"
    )
    status, body = api(
        f"https://bigquery.googleapis.com/bigquery/v2/projects/{PROJECT}/queries",
        method="POST",
        body={"query": query, "useLegacySql": False},
    )
    if status != 200:
        return f"- **Billing**: query failed ({status}: {body})\n"
    rows = body.get("rows", [])
    if not rows:
        return "- **Billing**: export table exists but has no rows yet for the last 24h.\n"
    lines = ["- **Billing** (last 24h):"]
    for row in rows:
        cost, currency = row["f"][0]["v"], row["f"][1]["v"]
        lines.append(f"  - {cost} {currency}")
    return "\n".join(lines) + "\n"


def main():
    billable_seconds, run_seconds_err = cloud_run_billable_seconds()
    requests, run_requests_err = cloud_run_requests()
    build_count, build_minutes, build_err = cloud_build_minutes()

    run_cost = None
    if billable_seconds is not None and requests is not None:
        run_cost = (
            billable_seconds * CLOUD_RUN_VCPUS * CLOUD_RUN_VCPU_SECOND
            + billable_seconds * CLOUD_RUN_MEMORY_GIB * CLOUD_RUN_MEMORY_GIB_SECOND
            + (requests / 1_000_000) * CLOUD_RUN_MILLION_REQUESTS
        )
    build_cost = build_minutes * CLOUD_BUILD_MINUTE if build_minutes is not None else None

    lines = [
        f"# karata0 usage report - {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}",
        "",
        "## 💰 Estimated cost (last 24h)",
        "*List-price estimate from usage metrics - ignores the monthly free tier, discounts, and "
        "egress, and is not a substitute for the real billing figure below once it has data. "
        "Actual bill may be $0 if this stays within the free tier.*",
        "",
    ]
    if run_cost is not None:
        lines.append(
            f"- Cloud Run: **${run_cost:.4f}** "
            f"({billable_seconds:.0f}s billable instance time, {requests:.0f} requests)"
        )
    else:
        lines.append(f"- Cloud Run: could not estimate ({run_seconds_err or run_requests_err})")
    if build_cost is not None:
        lines.append(f"- Cloud Build: **${build_cost:.4f}** ({build_minutes:.1f} build-minutes)")
    else:
        lines.append(f"- Cloud Build: could not estimate ({build_err})")
    if run_cost is not None and build_cost is not None:
        lines.append(f"- **Total estimate: ${run_cost + build_cost:.4f}**")
    lines.append("")

    lines.append("## Real billing (BigQuery export)")
    lines.append(report_billing())

    lines.append("## Raw usage")
    if requests is not None:
        lines.append(f"- **Cloud Run** (`{SERVICE}`): {requests:.0f} requests in the last 24h")
    if build_count is not None:
        lines.append(f"- **Cloud Build**: {build_count} builds in the last 24h")

    text = "\n".join(lines)
    print(text)
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a") as f:
            f.write(text + "\n")


if __name__ == "__main__":
    main()

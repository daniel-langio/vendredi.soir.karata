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


def report_cloud_build():
    since = datetime.now(timezone.utc) - timedelta(hours=24)
    status, body = api(
        f"https://cloudbuild.googleapis.com/v1/projects/{PROJECT}/locations/{REGION}/builds"
        "?pageSize=100"
    )
    if status != 200:
        return f"- **Cloud Build**: failed to list ({status}: {body})\n"
    since_str = since.strftime("%Y-%m-%dT%H:%M:%SZ")
    recent = [b for b in body.get("builds", []) if b.get("createTime", "") >= since_str]
    total_seconds = 0.0
    for b in recent:
        ct, ft = b.get("createTime"), b.get("finishTime")
        if ct and ft:
            t0 = datetime.strptime(ct[:19], "%Y-%m-%dT%H:%M:%S")
            t1 = datetime.strptime(ft[:19], "%Y-%m-%dT%H:%M:%S")
            total_seconds += (t1 - t0).total_seconds()
    return (
        f"- **Cloud Build**: {len(recent)} builds in the last 24h, "
        f"~{int(total_seconds // 60)} min total build time\n"
    )


def report_cloud_run_requests():
    now = datetime.now(timezone.utc)
    since = now - timedelta(hours=24)
    params = {
        "filter": (
            'metric.type="run.googleapis.com/request_count" '
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
        return f"- **Cloud Run**: failed to query ({status}: {body})\n"
    series = body.get("timeSeries", [])
    total = sum(
        int(p["value"].get("int64Value", 0)) for s in series for p in s.get("points", [])
    )
    return f"- **Cloud Run** (`{SERVICE}`): {total} requests in the last 24h\n"


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
    lines = [
        f"# karata0 usage report - {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}",
        "",
        report_billing(),
        report_cloud_run_requests(),
        report_cloud_build(),
    ]
    text = "\n".join(lines)
    print(text)
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a") as f:
            f.write(text + "\n")


if __name__ == "__main__":
    main()

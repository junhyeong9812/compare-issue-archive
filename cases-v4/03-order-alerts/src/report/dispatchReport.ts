import { DispatchRun, KIND_LABEL } from "../domain/notification";
import { CustomerRepository } from "../repository/customerRepository";
import { maskMobile, normalizeMobile, padEnd } from "../util/format";

export interface DispatchReportLine {
  orderNo: string;
  customerId: string;
  kind: string;
  status: "발송" | "미발송" | "실패";
  to: string;
}

export interface DispatchReport {
  runId: number;
  batch: number;
  planned: number;
  published: number;
  delivered: number;
  failed: number;
  skippedOptOut: number;
  lines: DispatchReportLine[];
  text: string;
}

/** 운영 화면용 회차 리포트 — 계획된 알림마다 발송 여부를 보여 준다. */
export function buildDispatchReport(run: DispatchRun, customers: CustomerRepository): DispatchReport {
  const lines: DispatchReportLine[] = run.planned.map((n) => {
    const hit = run.delivered.find((d) => d.orderNo === n.orderNo && d.kind === n.kind);
    const fail = run.failed.find((f) => f.orderNo === n.orderNo && f.kind === n.kind);
    const phone = normalizeMobile(customers.find(n.customerId)?.phone ?? "") ?? "";
    return {
      orderNo: n.orderNo,
      customerId: n.customerId,
      kind: KIND_LABEL[n.kind],
      status: hit ? "발송" : fail ? "실패" : "미발송",
      to: maskMobile(phone),
    };
  });
  const title = `회차 ${run.runId} (배치 ${run.batch}) 계획 ${run.planned.length} / 발행 ${run.published} / 발송 ${run.delivered.length} / 실패 ${run.failed.length} / 수신거부 ${run.skippedOptOut}`;
  const body = lines.map((l) => `${padEnd(l.orderNo, 22)} ${padEnd(l.kind, 10)} ${padEnd(l.status, 6)} ${l.to}`);
  return {
    runId: run.runId,
    batch: run.batch,
    planned: run.planned.length,
    published: run.published,
    delivered: run.delivered.length,
    failed: run.failed.length,
    skippedOptOut: run.skippedOptOut,
    lines,
    text: [title, ...body].join("\n"),
  };
}

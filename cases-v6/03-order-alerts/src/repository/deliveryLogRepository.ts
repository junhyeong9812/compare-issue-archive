import { DeliveredAlert, DispatchRun } from "../domain/notification";

export class DeliveryLogRepository {
  private readonly delivered: DeliveredAlert[] = [];
  private readonly runs = new Map<number, DispatchRun>();

  append(a: DeliveredAlert): void {
    this.delivered.push({ ...a });
  }

  byRun(runId: number): DeliveredAlert[] {
    return this.delivered.filter((d) => d.runId === runId).map((d) => ({ ...d }));
  }

  byCustomer(customerId: string): DeliveredAlert[] {
    return this.delivered.filter((d) => d.customerId === customerId).map((d) => ({ ...d }));
  }

  saveRun(run: DispatchRun): void {
    this.runs.set(run.runId, run);
  }

  findRun(runId: number): DispatchRun | undefined {
    return this.runs.get(runId);
  }
}

import { JobView } from "../domain/job";

export class JobViewStore {
  private readonly views = new Map<string, JobView>();

  get(id: string): JobView | undefined {
    const v = this.views.get(id);
    return v ? { ...v, log: [...v.log] } : undefined;
  }

  put(view: JobView): void {
    this.views.set(view.id, { ...view, log: [...view.log] });
  }

  delete(id: string): boolean {
    return this.views.delete(id);
  }

  /** Newest first. */
  list(): JobView[] {
    return [...this.views.values()]
      .sort((a, b) => b.startedAt - a.startedAt || (a.id < b.id ? -1 : 1))
      .map((v) => ({ ...v, log: [...v.log] }));
  }
}

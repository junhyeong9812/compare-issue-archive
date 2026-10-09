import { Customer } from "../domain/customer";

export class CustomerRepository {
  private readonly rows = new Map<string, Customer>();

  save(c: Customer): Customer {
    this.rows.set(c.id, { ...c });
    return c;
  }

  find(id: string): Customer | undefined {
    const c = this.rows.get(id);
    return c ? { ...c } : undefined;
  }

  all(): Customer[] {
    return [...this.rows.values()].map((c) => ({ ...c }));
  }
}

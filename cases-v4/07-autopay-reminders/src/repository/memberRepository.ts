import { Member } from "../domain/member";

export class MemberRepository {
  private readonly rows = new Map<string, Member>();

  save(m: Member): Member {
    this.rows.set(m.id, { ...m });
    return m;
  }

  find(id: string): Member | undefined {
    const m = this.rows.get(id);
    return m ? { ...m } : undefined;
  }

  all(): Member[] {
    return [...this.rows.values()].map((m) => ({ ...m }));
  }
}

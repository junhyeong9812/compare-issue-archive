export interface Customer {
  id: string;
  name: string;
  phone: string;
  /** 알림 문자 수신 동의 */
  smsOptIn: boolean;
}

export function validateCustomer(input: unknown): Customer {
  const c = input as Partial<Customer>;
  if (!c || typeof c.id !== "string" || !/^C\d{3,8}$/.test(c.id)) throw new Error("invalid customer id");
  if (typeof c.name !== "string" || c.name.trim() === "") throw new Error("invalid customer name");
  if (typeof c.phone !== "string") throw new Error("invalid phone");
  return { id: c.id, name: c.name.trim(), phone: c.phone, smsOptIn: c.smsOptIn !== false };
}

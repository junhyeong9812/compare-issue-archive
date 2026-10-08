import { ProcessHandle } from "../runtime/process";
import { Supervisor } from "../stream/supervisor";

export type ProcessFactory = (command: string) => ProcessHandle;

export class Launcher {
  private counter = 0;

  constructor(private readonly supervisor: Supervisor, private readonly spawn: ProcessFactory) {}

  launch(name: string, command: string): string {
    if (!name.trim()) throw new Error("job name is required");
    const id = `job-${String(++this.counter).padStart(4, "0")}`;
    this.supervisor.attach(id, name.trim(), this.spawn(command));
    return id;
  }
}

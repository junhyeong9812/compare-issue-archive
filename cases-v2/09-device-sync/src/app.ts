import { DevicesHandler } from "./api/devicesHandler";
import { Router } from "./api/router";
import { SyncConfig, defaultConfig } from "./config";
import { ImportService } from "./service/importService";
import { ProfileService } from "./service/profileService";
import { SyncStatusService } from "./service/syncStatusService";
import { DesiredStore } from "./store/desiredStore";
import { RevisionStore } from "./store/revisionStore";
import { Clock, systemClock } from "./util/clock";

export function createApp(clock: Clock = systemClock, overrides: Partial<SyncConfig> = {}): Router {
  const config = { ...defaultConfig, ...overrides };
  const revisions = new RevisionStore();
  const desired = new DesiredStore();
  const profiles = new ProfileService(revisions, clock, config);
  const sync = new SyncStatusService(desired, revisions, config);
  const importer = new ImportService(profiles);
  return new Router(new DevicesHandler(profiles, revisions, desired, sync, importer, config));
}

import { DeviceProfile, validateProfile } from "../domain/profile";
import { ProfileService } from "./profileService";

export interface ImportReport {
  created: string[];
  unchanged: string[];
  rejected: { index: number; errors: string[] }[];
}

/** Bulk import from a fleet export; devices whose configuration did not change are left alone. */
export class ImportService {
  constructor(private readonly profiles: ProfileService) {}

  importAll(items: unknown[]): ImportReport {
    const report: ImportReport = { created: [], unchanged: [], rejected: [] };
    items.forEach((item, index) => {
      const errors = validateProfile(item);
      if (errors.length > 0) {
        report.rejected.push({ index, errors });
        return;
      }
      const res = this.profiles.apply(item as DeviceProfile, "import");
      (res.changed ? report.created : report.unchanged).push(res.revision.deviceId);
    });
    return report;
  }
}

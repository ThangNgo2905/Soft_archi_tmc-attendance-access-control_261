export interface IAttendanceRepository {
  save(transaction: any): Promise<void>;
  findLatestByEmployee(employeeId: string): Promise<any | null>;
}
export interface ITurnstileCommandPort {
  unlockGate(gateId: string, transactionId: string): Promise<{ success: boolean; status: string }>;
}
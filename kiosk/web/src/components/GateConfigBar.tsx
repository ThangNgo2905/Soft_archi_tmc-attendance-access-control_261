import { GateConfig, GateDirection } from '../types';

interface GateConfigBarProps {
  gate: GateConfig;
  onChange: (next: GateConfig) => void;
  simulatedOffline: boolean;
  onToggleOffline: () => void;
}

const FACTORY_OPTIONS = ['Binh Duong', 'Long An'];

export function GateConfigBar({
  gate,
  onChange,
  simulatedOffline,
  onToggleOffline,
}: GateConfigBarProps) {
  return (
    <section className="card gate-config">
      <div className="config-fields">
        <label className="field">
          <span>Nhà máy (Factory)</span>
          <select
            value={gate.factoryId}
            onChange={(e) => {
              const nextFactory = e.target.value;
              const prefix = nextFactory === 'Long An' ? 'LA' : 'BD';
              onChange({
                ...gate,
                factoryId: nextFactory,
                gateId: `GATE_${prefix}_${gate.direction}_01`,
              });
            }}
          >
            {FACTORY_OPTIONS.map((f) => (
              <option key={f} value={f}>
                {f}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Mã cổng (gateId)</span>
          <input
            type="text"
            value={gate.gateId}
            onChange={(e) => onChange({ ...gate, gateId: e.target.value })}
            placeholder="GATE_BD_IN_01"
          />
        </label>

        <label className="field">
          <span>Chiều quét (Direction)</span>
          <select
            value={gate.direction}
            onChange={(e) => {
              const nextDir = e.target.value as GateDirection;
              onChange({
                ...gate,
                direction: nextDir,
              });
            }}
          >
            <option value="IN">IN (Vào ca)</option>
            <option value="OUT">OUT (Tan ca)</option>
          </select>
        </label>
      </div>

      <div className="offline-toggle-wrap">
        <button
          type="button"
          className={`btn ${simulatedOffline ? 'btn-warning' : 'btn-outline'}`}
          onClick={onToggleOffline}
        >
          {simulatedOffline
            ? 'Đang giả lập mất mạng (Bật lại mạng)'
            : 'Giả lập mất mạng Backend'}
        </button>
      </div>
    </section>
  );
}

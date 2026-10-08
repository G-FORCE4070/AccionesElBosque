import { Card } from "../../../components/ui/Card";
import { Button } from "../../../components/ui/Button";
import { ToggleRow } from "./ToggleRow";
import { useProfileStore } from "../store/profileStore";

export function AutoReportsCard() {
  const monthlyReport = useProfileStore((s) => s.notifChannels.monthlyReport);

  return (
    <Card>
      <div className="flex items-center gap-3 mb-2">
        <h2 className="text-base font-semibold text-gold">Reportes Automáticos</h2>
        <span className="text-[10px] text-text-muted border border-border-default rounded-full px-2 py-0.5">
          Próximamente
        </span>
      </div>
      <ToggleRow
        label={<span className="font-medium">Recibir reporte mensual detallado</span>}
        subLabel="Incluye ganancias, comisiones y desglose por comisionista."
        checked={monthlyReport}
        onChange={() => {}}
        disabled
      />
      <p className="text-xs text-text-muted mt-3">
        El reporte se enviará automáticamente el primer día de cada mes a tu correo registrado.
      </p>
      <div className="flex items-center justify-end gap-3 mt-4 pt-3 border-t border-border-subtle/30">
        <Button size="sm" variant="secondary" disabled>
          Guardar
        </Button>
      </div>
    </Card>
  );
}

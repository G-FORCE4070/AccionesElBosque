import { Card } from "../../../components/ui/Card";
import { Button } from "../../../components/ui/Button";
import { useProfileStore } from "../store/profileStore";

const FIELDS = [
  { key: "name",          label: "Nombre" },
  { key: "specialty",     label: "Especialidad" },
  { key: "since",         label: "Desde" },
  { key: "commissionPct", label: "Comisión" },
] as const;

export function BrokerInfoCard() {
  const broker = useProfileStore((s) => s.broker);

  return (
    <Card>
      <h2 className="text-base font-semibold text-gold">Comisionista Asignado</h2>
      <div className="border-t border-border-subtle/40 my-4" />

      {broker ? (
        <>
          <div className="grid grid-cols-2 gap-x-12 gap-y-6">
            {FIELDS.map((f) => (
              <div key={f.key}>
                <p className="text-xs uppercase tracking-wide text-text-muted mb-2">{f.label}</p>
                <p className="text-sm text-text-secondary border-b border-border-subtle/30 pb-3">
                  {broker[f.key]}
                </p>
              </div>
            ))}
          </div>
          <div className="mt-6">
            <Button variant="secondary" size="sm">
              Solicitar cambio de comisionista
            </Button>
          </div>
        </>
      ) : (
        <div className="flex flex-col gap-3">
          <p className="text-sm text-text-muted">No tienes un comisionista asignado actualmente.</p>
          <Button size="sm">Asignar comisionista</Button>
        </div>
      )}
    </Card>
  );
}

import { AuthShell } from "../components/AuthShell";
import { MfaCard } from "../components/MfaCard";

export function MfaPage() {
  return (
    <AuthShell breadcrumb="Autenticación">
      <MfaCard />
    </AuthShell>
  );
}

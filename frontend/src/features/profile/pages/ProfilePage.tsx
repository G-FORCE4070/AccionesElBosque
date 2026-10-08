import { useEffect } from "react";
import { PageHeader } from "../../../components/layout/PageHeader";
import { PersonalInfoCard } from "../components/PersonalInfoCard";
import { PreferencesCard } from "../components/PreferencesCard";
import { WatchlistManagerCard } from "../components/WatchlistManagerCard";
import { BrokerInfoCard } from "../components/BrokerInfoCard";
import { SubscriptionCard } from "../components/SubscriptionCard";
import { AutoReportsCard } from "../components/AutoReportsCard";
import { useProfileStore } from "../store/profileStore";
import { useSessionStore } from "../../../store/sessionStore";

export function ProfilePage() {
  const { loadProfile, loaded } = useProfileStore();
  const token = useSessionStore((s) => s.token);

  useEffect(() => {
    if (token && !loaded) {
      loadProfile(token);
    }
  }, [token, loaded, loadProfile]);

  return (
    <>
      <PageHeader
        title="Gestión de Perfil"
        subtitle="Configura tus preferencias, seguridad y suscripción"
      />
      <div className="space-y-6 pb-8">
        <PersonalInfoCard />
        <PreferencesCard />
        <WatchlistManagerCard />
        <BrokerInfoCard />
        <SubscriptionCard />
        <AutoReportsCard />
      </div>
    </>
  );
}

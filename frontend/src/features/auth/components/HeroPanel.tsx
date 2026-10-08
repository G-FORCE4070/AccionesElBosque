import { Logo } from "../../../components/ui/Logo";

export function HeroPanel() {
  return (
    <div className="flex flex-col justify-center max-w-lg px-8">
      <Logo iconSize={32} />
      <div className="mt-6">
        <h1 className="font-display text-5xl font-bold leading-tight">
          <span className="text-text-primary">Opera los</span>
          <br />
          <span className="text-text-primary">mercados con</span>
          <br />
          <span className="text-gold">datos en tiempo real</span>
        </h1>
      </div>
      <p className="mt-6 text-base text-text-muted max-w-[400px]">
        Plataforma profesional de day trading con acceso a NYSE, NASDAQ, LSE, TSE
        y ASX. Ejecución en tiempo real, transparencia total y cumplimiento
        normativo garantizado.
      </p>
    </div>
  );
}

import { useState } from "react";
import { ArrowRight, Eye, EyeOff, Mail } from "lucide-react";
import {
  Button, Input, Card, Badge, Toggle, Select,
  Checkbox, Avatar, Tabs, Logo, Table,
} from "../components/ui";

const sampleData = [
  { id: "1", ticker: "AAPL", name: "Apple Inc.", price: "$182.63", change: "+1.24%", status: "success" as const },
  { id: "2", ticker: "TSLA", name: "Tesla Inc.", price: "$218.10", change: "-2.05%", status: "danger" as const },
  { id: "3", ticker: "MSFT", name: "Microsoft Corp.", price: "$415.22", change: "+0.87%", status: "success" as const },
];

const selectOptions = [
  { value: "nyse", label: "NYSE" },
  { value: "nasdaq", label: "NASDAQ" },
  { value: "lse", label: "LSE" },
  { value: "tse", label: "TSE" },
  { value: "asx", label: "ASX" },
];

export function DevGallery() {
  const [toggle, setToggle] = useState(false);
  const [checkbox, setCheckbox] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [selectVal, setSelectVal] = useState("");
  const [activeTab, setActiveTab] = useState("history");

  return (
    <div className="min-h-screen bg-bg-primary p-8">
      <div className="max-w-5xl mx-auto space-y-12">

        {/* Header */}
        <div className="space-y-2">
          <Logo iconSize={32} />
          <h1 className="font-display text-4xl text-text-primary mt-4">
            Design System — Galería de componentes
          </h1>
          <p className="text-text-muted">Sprint 00 — Acciones ElBosque</p>
        </div>

        {/* Buttons */}
        <section>
          <h2 className="text-2xl font-display text-text-primary mb-6">Botones</h2>
          <div className="flex flex-wrap gap-3 mb-4">
            <Button variant="primary">Primario</Button>
            <Button variant="primary" rightIcon={<ArrowRight size={16} />}>Con icono</Button>
            <Button variant="primary" loading>Cargando</Button>
            <Button variant="primary" disabled>Deshabilitado</Button>
          </div>
          <div className="flex flex-wrap gap-3 mb-4">
            <Button variant="secondary">Secundario</Button>
            <Button variant="secondary" leftIcon={<Mail size={16} />}>Con icono</Button>
          </div>
          <div className="flex flex-wrap gap-3 mb-4">
            <Button variant="ghost">Ghost</Button>
            <Button variant="danger">Peligro</Button>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button size="sm">Pequeño</Button>
            <Button size="md">Mediano</Button>
            <Button size="lg">Grande</Button>
          </div>
        </section>

        {/* Inputs */}
        <section>
          <h2 className="text-2xl font-display text-text-primary mb-6">Inputs</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input placeholder="Sin label" />
            <Input label="Correo electrónico" placeholder="usuario@correo.com" leftIcon={<Mail size={16} />} />
            <Input
              label="Contraseña"
              type={showPassword ? "text" : "password"}
              placeholder="••••••••"
              rightIcon={
                <button type="button" onClick={() => setShowPassword((s) => !s)}>
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              }
            />
            <Input label="Con error" placeholder="Ticker" error="El ticker no existe en el mercado." />
          </div>
        </section>

        {/* Select */}
        <section>
          <h2 className="text-2xl font-display text-text-primary mb-6">Select</h2>
          <div className="max-w-xs">
            <Select
              label="Mercado"
              options={selectOptions}
              value={selectVal}
              onChange={setSelectVal}
              placeholder="Seleccionar mercado..."
            />
          </div>
        </section>

        {/* Badges */}
        <section>
          <h2 className="text-2xl font-display text-text-primary mb-6">Badges (estados de orden)</h2>
          <div className="flex flex-wrap gap-3">
            <Badge variant="pending">Pendiente</Badge>
            <Badge variant="sent">Enviada</Badge>
            <Badge variant="signed">Firmada por Comisionista</Badge>
            <Badge variant="executing">En Ejecución</Badge>
            <Badge variant="success">Confirmada</Badge>
            <Badge variant="danger">Cancelada</Badge>
            <Badge variant="danger">Rechazada</Badge>
            <Badge variant="danger">Fallida</Badge>
            <Badge variant="neutral">Expirada</Badge>
            <Badge variant="neutral">Detenida</Badge>
            <Badge variant="neutral">En Revisión</Badge>
            <Badge variant="premium">Premium</Badge>
          </div>
        </section>

        {/* Toggle & Checkbox */}
        <section>
          <h2 className="text-2xl font-display text-text-primary mb-6">Toggle & Checkbox</h2>
          <div className="flex items-center gap-8">
            <div className="flex items-center gap-3">
              <Toggle checked={toggle} onChange={setToggle} />
              <span className="text-text-secondary text-sm">
                Notificaciones: {toggle ? "activas" : "inactivas"}
              </span>
            </div>
            <Checkbox
              checked={checkbox}
              onChange={setCheckbox}
              label="Acepto los términos y condiciones"
            />
          </div>
        </section>

        {/* Avatar */}
        <section>
          <h2 className="text-2xl font-display text-text-primary mb-6">Avatar</h2>
          <div className="flex items-center gap-4">
            <Avatar name="Carlos Rodríguez" size="sm" />
            <Avatar name="María López" size="md" />
            <Avatar name="Juan Pablo Torres" size="lg" />
          </div>
        </section>

        {/* Tabs */}
        <section>
          <h2 className="text-2xl font-display text-text-primary mb-6">Tabs</h2>
          <Tabs
            tabs={[
              { key: "history", label: "Historial de órdenes", badge: 12 },
              { key: "proposals", label: "Propuestas del comisionista", badge: 3 },
            ]}
            active={activeTab}
            onChange={setActiveTab}
          />
        </section>

        {/* Cards */}
        <section>
          <h2 className="text-2xl font-display text-text-primary mb-6">Cards</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Card>
              <p className="text-text-muted text-sm">Valor del portafolio</p>
              <p className="text-3xl font-sans tabular-nums text-text-primary mt-1">$24,580.00</p>
              <Badge variant="success" className="mt-3">+5.2% hoy</Badge>
            </Card>
            <Card interactive>
              <p className="text-text-muted text-sm">Card interactiva</p>
              <p className="text-text-secondary text-sm mt-2">Hover para ver el efecto de borde dorado.</p>
            </Card>
            <Card className="border-gold/40">
              <Logo />
              <p className="text-text-muted text-xs mt-3">Card con borde personalizado</p>
            </Card>
          </div>
        </section>

        {/* Table */}
        <section>
          <h2 className="text-2xl font-display text-text-primary mb-6">Tabla</h2>
          <Card className="p-0">
            <Table
              data={sampleData}
              keyExtractor={(r) => r.id}
              columns={[
                { key: "ticker", header: "Ticker", render: (r) => <span className="font-semibold text-text-primary">{r.ticker}</span> },
                { key: "name", header: "Nombre", render: (r) => r.name },
                { key: "price", header: "Precio", render: (r) => r.price },
                { key: "change", header: "Cambio", render: (r) => (
                  <span className={r.status === "success" ? "text-success" : "text-danger"}>
                    {r.change}
                  </span>
                )},
                { key: "status", header: "Estado", render: (r) => <Badge variant={r.status}>{r.status === "success" ? "Alza" : "Baja"}</Badge> },
              ]}
            />
          </Card>
        </section>

        {/* Typography */}
        <section className="pb-16">
          <h2 className="text-2xl font-display text-text-primary mb-6">Tipografía</h2>
          <div className="space-y-4">
            <p className="font-display text-4xl text-text-primary">H1 — Playfair Display 36px</p>
            <p className="font-display text-3xl text-text-primary">H2 — Playfair Display 30px</p>
            <p className="font-display text-2xl text-text-primary">H3 — Playfair Display 24px</p>
            <p className="font-sans text-lg text-text-secondary">Texto largo — Inter 18px. Los mercados financieros ofrecen oportunidades únicas.</p>
            <p className="font-sans text-base text-text-secondary">Cuerpo — Inter 16px. Compra y vende acciones en NYSE, NASDAQ, LSE, TSE y ASX.</p>
            <p className="font-sans text-sm text-text-muted">Tabla / Label — Inter 14px. Comisión: 0.5% por operación.</p>
            <p className="font-sans text-xs uppercase tracking-wide text-text-subtle">Caption — Inter 12px uppercase</p>
          </div>
        </section>

      </div>
    </div>
  );
}

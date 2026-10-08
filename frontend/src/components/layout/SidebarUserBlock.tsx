import { useState } from "react";
import { LogOut } from "lucide-react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import { Avatar } from "../ui/Avatar";
import { useSessionStore } from "../../store/sessionStore";

function LogoutDialog({ onClose, onConfirm }: { readonly onClose: () => void; readonly onConfirm: () => void }) {
  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
      role="presentation"
      onClick={onClose}
      onKeyDown={(e) => { if (e.key === 'Escape') onClose(); }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="logout-dialog-title"
        className="relative bg-bg-elevated border border-border-subtle rounded-2xl p-6 w-full max-w-[380px] shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <p id="logout-dialog-title" className="font-display text-2xl text-text-primary mb-2">¿Cerrar sesión?</p>
        <p className="text-sm text-text-muted mb-6">
          Se cerrará tu sesión actual y serás redirigido a la pantalla de inicio.
        </p>
        <div className="flex gap-3">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 py-2.5 rounded-xl border border-border-default text-text-secondary text-sm font-medium hover:bg-bg-tertiary transition-colors duration-150"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="flex-1 py-2.5 rounded-xl border border-danger text-danger text-sm font-bold hover:bg-danger/10 transition-colors duration-150"
          >
            Cerrar sesión
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}

export function SidebarUserBlock() {
  const { user, clearSession } = useSessionStore();
  const navigate = useNavigate();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const handleLogout = () => {
    clearSession();
    navigate("/login", { replace: true });
  };

  const name = user ? `${user.firstName} ${user.lastName}` : "Usuario";
  const plan = user?.plan
    ? `Plan ${user.plan.charAt(0).toUpperCase() + user.plan.slice(1)}`
    : "Plan Básico";

  return (
    <>
      <div className="pt-4 border-t border-border-subtle">
        <div className="flex items-center gap-3">
          <Avatar name={name} size="md" />
          <div className="min-w-0">
            <p className="text-sm text-text-secondary font-medium truncate">{name}</p>
            <p className="text-xs text-text-muted">{plan}</p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => setConfirmOpen(true)}
          className="mt-3 w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border border-border-subtle text-xs text-text-muted hover:border-danger/40 hover:text-danger transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-danger/40"
        >
          <LogOut size={13} />
          Cerrar sesión
        </button>
      </div>

      {confirmOpen && (
        <LogoutDialog
          onClose={() => setConfirmOpen(false)}
          onConfirm={handleLogout}
        />
      )}
    </>
  );
}

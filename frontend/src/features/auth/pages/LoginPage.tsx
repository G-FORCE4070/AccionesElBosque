import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { CheckCircle, AlertCircle } from "lucide-react";
import { AuthShell } from "../components/AuthShell";
import { HeroPanel } from "../components/HeroPanel";
import { LoginCard } from "../components/LoginCard";

export function LoginPage() {
  const location = useLocation();
  const [toast, setToast] = useState<string>("");
  const [toastType, setToastType] = useState<"success" | "warning">("success");

  useEffect(() => {
    const redirectMsg = sessionStorage.getItem("aeb_redirect_msg");
    if (redirectMsg) {
      sessionStorage.removeItem("aeb_redirect_msg");
      setToast(redirectMsg);
      setToastType("warning");
      const t = setTimeout(() => setToast(""), 5000);
      return () => clearTimeout(t);
    }
    const msg: string = location.state?.successMessage ?? "";
    if (msg) {
      setToast(msg);
      setToastType("success");
      const t = setTimeout(() => setToast(""), 5000);
      return () => clearTimeout(t);
    }
  }, [location.state]);

  return (
    <AuthShell breadcrumb="Iniciar Sesión">
      <div className="flex flex-col items-center w-full max-w-5xl gap-4">
        {toast && toastType === "success" && (
          <div className="flex items-center gap-2 px-4 py-3 bg-success/10 border border-success/30 rounded-xl text-sm text-success">
            <CheckCircle size={16} className="shrink-0" />
            {toast}
          </div>
        )}
        {toast && toastType === "warning" && (
          <div className="flex items-center gap-2 px-4 py-3 bg-warning/10 border border-warning/30 rounded-xl text-sm text-warning">
            <AlertCircle size={16} className="shrink-0" />
            {toast}
          </div>
        )}

        <div className="flex items-center w-full">
          {/* Left — hero */}
          <div className="hidden md:flex flex-1 justify-center">
            <HeroPanel />
          </div>

          {/* Divider */}
          <div className="hidden md:flex items-center self-stretch mx-6">
            <div className="w-px h-[50vh] bg-gradient-to-b from-transparent via-gold/30 to-transparent" />
          </div>

          {/* Right — login card */}
          <div className="flex-1 flex justify-center">
            <LoginCard />
          </div>
        </div>
      </div>
    </AuthShell>
  );
}

import { create } from "zustand";
import { persist } from "zustand/middleware";

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  plan: string;
}

interface SessionState {
  token: string | null;
  user: User | null;
  setSession: (token: string, user: User) => void;
  clearSession: () => void;
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setSession: (token, user) => set({ token, user }),
      clearSession: () => set({ token: null, user: null }),
    }),
    {
      name: "aeb-session",
      partialize: (state) => ({ token: state.token, user: state.user }),
    }
  )
);

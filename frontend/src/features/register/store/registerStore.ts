import { create } from "zustand";
import type {
  Step1Data,
  Step2Data,
  Step3Data,
  Step4Data,
  Step5Data,
  Step6Data,
} from "../schemas/registerSchema";

export type StoreData = {
  access?: Step1Data;
  identity?: Step2Data;
  financial?: Step3Data;
  compliance?: Step4Data;
  preferences?: Step5Data;
  plan?: Step6Data;
};

type RegisterStore = {
  currentStep: number;
  data: StoreData;
  goNext: () => void;
  goBack: () => void;
  goTo: (step: number) => void;
  setStepData: <K extends keyof StoreData>(key: K, value: StoreData[K]) => void;
  reset: () => void;
};

export const useRegisterStore = create<RegisterStore>((set) => ({
  currentStep: 1,
  data: {},
  goNext: () => set((s) => ({ currentStep: Math.min(s.currentStep + 1, 6) })),
  goBack: () => set((s) => ({ currentStep: Math.max(s.currentStep - 1, 1) })),
  goTo: (step) => set({ currentStep: Math.max(1, Math.min(6, step)) }),
  setStepData: (key, value) =>
    set((s) => ({ data: { ...s.data, [key]: value } })),
  reset: () => set({ currentStep: 1, data: {} }),
}));

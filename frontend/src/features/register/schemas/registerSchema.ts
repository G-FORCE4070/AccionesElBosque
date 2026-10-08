import { z } from "zod";

export const step1Schema = z
  .object({
    firstName: z.string().min(2, "Mínimo 2 caracteres"),
    lastName: z.string().min(2, "Mínimo 2 caracteres"),
    email: z.string().email("Correo electrónico inválido"),
    phone: z.string().min(7, "Teléfono inválido"),
    password: z
      .string()
      .min(8, "Mínimo 8 caracteres")
      .regex(/[A-Z]/, "Debe contener al menos una mayúscula")
      .regex(/[0-9]/, "Debe contener al menos un número")
      .regex(/[^A-Za-z0-9]/, "Debe contener al menos un símbolo"),
    confirmPassword: z.string().min(1, "Confirma tu contraseña"),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: "Las contraseñas no coinciden",
    path: ["confirmPassword"],
  });

export const step2Schema = z.object({
  birthDate: z.string()
    .min(1, "Requerido")
    .refine((val) => {
      const birth = new Date(val);
      const today = new Date();
      let age = today.getFullYear() - birth.getFullYear();
      const m = today.getMonth() - birth.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
      return age >= 18;
    }, "Debes tener al menos 18 años para registrarte"),
  docType: z.string().min(1, "Seleccione un tipo de documento"),
  docNumber: z.string().min(1, "Requerido"),
  nationality: z.string().min(1, "Seleccione su nacionalidad"),
  country: z.string().min(1, "Requerido"),
  address: z.string().min(1, "Requerido"),
  city: z.string().min(1, "Requerido"),
  postalCode: z.string().min(1, "Requerido"),
});

export const step3Schema = z.object({
  incomeSource: z.string().min(1, "Seleccione una opción"),
  annualIncome: z.string().min(1, "Seleccione una opción"),
  netWorth: z.string().min(1, "Seleccione una opción"),
  riskTolerance: z.string().min(1, "Seleccione una opción"),
  tradingExperience: z.string().min(1, "Seleccione una opción"),
  investmentGoal: z.string().min(1, "Seleccione una opción"),
});

export const step4Schema = z.object({
  isPep: z.boolean(),
  isFinraAffiliated: z.boolean(),
  acceptsTerms: z
    .boolean()
    .refine((v) => v === true, "Debes aceptar los Términos y Condiciones"),
  acceptsPrivacy: z
    .boolean()
    .refine((v) => v === true, "Debes aceptar la Política de Privacidad"),
  authorizeDataUse: z.boolean(),
});

export const step5Schema = z.object({
  markets: z.string().min(1, "Selecciona un mercado"),
  sectors: z.string().min(1, "Selecciona un sector"),
  notifEmail: z.boolean(),
  notifSms: z.boolean(),
  notifWhatsapp: z.boolean(),
  monthlyReport: z.boolean(),
});

export const step6Schema = z
  .object({
    plan: z.enum(["basic", "premium"]),
    billingCycle: z.enum(["monthly", "annual"]).optional(),
  })
  .refine(
    (data) => data.plan === "basic" || data.billingCycle !== undefined,
    {
      message: "Selecciona el ciclo de facturación",
      path: ["billingCycle"],
    }
  );

export type Step1Data = z.infer<typeof step1Schema>;
export type Step2Data = z.infer<typeof step2Schema>;
export type Step3Data = z.infer<typeof step3Schema>;
export type Step4Data = z.infer<typeof step4Schema>;
export type Step5Data = z.infer<typeof step5Schema>;
export type Step6Data = z.infer<typeof step6Schema>;

import { z } from "zod";

export const otpSchema = z.object({
  code: z.string().regex(/^\d{6}$/, "El código debe tener 6 dígitos"),
});

export type OtpFormData = z.infer<typeof otpSchema>;

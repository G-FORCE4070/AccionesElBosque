import { create } from "zustand";
import { mockPersonal } from "../data/mockProfile";
import { profileService, type UpdatePersonalPayload, type UpdatePreferencesPayload } from "../services/profileService";

interface Personal {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  birthDate: string;
  nationality: string;
  docType: string;
  docNumber: string;
  country: string;
  city: string;
  address: string;
  postalCode: string;
  incomeSource: string;
  annualIncome: string;
  netWorth: string;
  riskTolerance: string;
  tradingExperience: string;
  investmentGoal: string;
}

interface NotificationPrefs {
  marketOpen: boolean;
  priceAlerts: boolean;
  dailySummary: boolean;
  orderConfirm: boolean;
}

interface NotifChannels {
  notifEmail: boolean;
  notifSms: boolean;
  notifWhatsapp: boolean;
  monthlyReport: boolean;
}

interface PortfolioPrefs {
  viewType: string;
  defaultOrder: string;
  showLowWeight: boolean;
}

interface OperationsPrefs {
  defaultOrderType: string;
  dailyLimit: number;
  requireDoubleAuth: boolean;
}

interface Broker {
  name: string;
  specialty: string;
  since: string;
  commissionPct: string;
}

interface Subscription {
  plan: "basic" | "premium";
  features: string[];
}

interface ProfileState {
  personal: Personal;
  notifications: NotificationPrefs;
  notifChannels: NotifChannels;
  portfolio: PortfolioPrefs;
  operations: OperationsPrefs;
  market: string;
  sector: string;
  watchlist: string[];
  broker: Broker | null;
  subscription: Subscription;
  loaded: boolean;

  loadProfile: (token: string) => Promise<void>;
  updatePersonal: (token: string, payload: UpdatePersonalPayload) => Promise<void>;
  updatePreferences: (token: string, payload: UpdatePreferencesPayload) => Promise<void>;

  // Local setters (for controlled inputs before save)
  setNotifications: (p: Partial<NotificationPrefs>) => void;
  setNotifChannels: (p: Partial<NotifChannels>) => void;
  setPortfolio: (p: Partial<PortfolioPrefs>) => void;
  setOperations: (p: Partial<OperationsPrefs>) => void;
  setMarket: (v: string) => void;
  setSector: (v: string) => void;

  // kept for WatchlistManagerCard + SubscriptionCard compatibility
  toggleWatchlistSymbol: (symbol: string) => void;
  addWatchlistSymbol: (symbol: string) => void;
  updateSubscription: (plan: "basic" | "premium") => void;
}

export const useProfileStore = create<ProfileState>((set, get) => ({
  personal: {
    firstName: mockPersonal.firstName,
    lastName: mockPersonal.lastName,
    email: mockPersonal.email,
    phone: mockPersonal.phone,
    birthDate: mockPersonal.birthDate,
    nationality: mockPersonal.nationality,
    docType: mockPersonal.docType,
    docNumber: mockPersonal.docNumber,
    country: mockPersonal.country,
    city: mockPersonal.city,
    address: mockPersonal.address,
    postalCode: mockPersonal.postalCode,
    incomeSource: mockPersonal.incomeSource,
    annualIncome: mockPersonal.annualIncome,
    netWorth: mockPersonal.netWorth,
    riskTolerance: mockPersonal.riskTolerance,
    tradingExperience: mockPersonal.tradingExperience,
    investmentGoal: mockPersonal.investmentGoal,
  },
  notifications: { marketOpen: true, priceAlerts: false, dailySummary: false, orderConfirm: true },
  notifChannels: { notifEmail: true, notifSms: false, notifWhatsapp: false, monthlyReport: false },
  portfolio: { viewType: "list", defaultOrder: "totalValue", showLowWeight: false },
  operations: { defaultOrderType: "market", dailyLimit: 10, requireDoubleAuth: true },
  market: mockPersonal.market,
  sector: mockPersonal.sector,
  watchlist: ["AAPL", "MSFT", "NVDA"],
  broker: null,
  subscription: { plan: "basic", features: [] },
  loaded: false,

  loadProfile: async (token) => {
    try {
      const data = await profileService.getProfile(token);
      const p = data.preferences;
      set({
        personal: {
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email,
          phone: data.phone,
          birthDate: data.birthDate ?? "",
          nationality: data.nationality ?? "",
          docType: data.docType ?? "",
          docNumber: data.docNumber ?? "",
          country: data.country ?? "",
          city: data.city ?? "",
          address: data.address ?? "",
          postalCode: data.postalCode ?? "",
          incomeSource: data.incomeSource ?? "",
          annualIncome: data.annualIncome ?? "",
          netWorth: data.netWorth ?? "",
          riskTolerance: data.riskTolerance ?? "",
          tradingExperience: data.tradingExperience ?? "",
          investmentGoal: data.investmentGoal ?? "",
        },
        notifications: {
          marketOpen: p.marketOpen,
          priceAlerts: p.priceAlerts,
          dailySummary: p.dailySummary,
          orderConfirm: p.orderConfirm,
        },
        notifChannels: {
          notifEmail: p.notifEmail,
          notifSms: p.notifSms,
          notifWhatsapp: p.notifWhatsapp,
          monthlyReport: p.monthlyReport,
        },
        portfolio: {
          viewType: p.viewType ?? "list",
          defaultOrder: p.defaultOrder ?? "totalValue",
          showLowWeight: p.showLowWeight,
        },
        operations: {
          defaultOrderType: p.defaultOrderType ?? "market",
          dailyLimit: p.dailyLimit ?? 0,
          requireDoubleAuth: p.requireDoubleAuth,
        },
        market: p.market ?? "",
        sector: p.sector ?? "",
        broker: data.broker ?? null,
        subscription: {
          plan: (data.plan?.toLowerCase() as "basic" | "premium") ?? "basic",
          features: data.plan === "PREMIUM"
            ? ["Watchlist personalizada", "Alertas de precio"]
            : [],
        },
        loaded: true,
      });
    } catch {
      // Keep defaults if the API is unavailable
    }
  },

  updatePersonal: async (token, payload) => {
    await profileService.updatePersonal(token, payload);
    set((s) => ({
      personal: {
        ...s.personal,
        ...(payload.address !== undefined && { address: payload.address }),
        ...(payload.city !== undefined && { city: payload.city }),
        ...(payload.country !== undefined && { country: payload.country }),
        ...(payload.postalCode !== undefined && { postalCode: payload.postalCode }),
        ...(payload.incomeSource !== undefined && { incomeSource: payload.incomeSource }),
        ...(payload.annualIncome !== undefined && { annualIncome: payload.annualIncome }),
        ...(payload.netWorth !== undefined && { netWorth: payload.netWorth }),
        ...(payload.riskTolerance !== undefined && { riskTolerance: payload.riskTolerance }),
        ...(payload.tradingExperience !== undefined && { tradingExperience: payload.tradingExperience }),
        ...(payload.investmentGoal !== undefined && { investmentGoal: payload.investmentGoal }),
      },
    }));
  },

  updatePreferences: async (token, payload) => {
    await profileService.updatePreferences(token, payload);
    set((s) => ({
      notifications: {
        marketOpen: payload.marketOpen ?? s.notifications.marketOpen,
        priceAlerts: payload.priceAlerts ?? s.notifications.priceAlerts,
        dailySummary: payload.dailySummary ?? s.notifications.dailySummary,
        orderConfirm: payload.orderConfirm ?? s.notifications.orderConfirm,
      },
      notifChannels: {
        notifEmail: payload.notifEmail ?? s.notifChannels.notifEmail,
        notifSms: payload.notifSms ?? s.notifChannels.notifSms,
        notifWhatsapp: payload.notifWhatsapp ?? s.notifChannels.notifWhatsapp,
        monthlyReport: payload.monthlyReport ?? s.notifChannels.monthlyReport,
      },
      portfolio: {
        viewType: payload.viewType ?? s.portfolio.viewType,
        defaultOrder: payload.defaultOrder ?? s.portfolio.defaultOrder,
        showLowWeight: payload.showLowWeight ?? s.portfolio.showLowWeight,
      },
      operations: {
        defaultOrderType: payload.defaultOrderType ?? s.operations.defaultOrderType,
        dailyLimit: payload.dailyLimit ?? s.operations.dailyLimit,
        requireDoubleAuth: payload.requireDoubleAuth ?? s.operations.requireDoubleAuth,
      },
      market: payload.market ?? s.market,
      sector: payload.sector ?? s.sector,
    }));
  },

  setNotifications: (p) => set((s) => ({ notifications: { ...s.notifications, ...p } })),
  setNotifChannels: (p) => set((s) => ({ notifChannels: { ...s.notifChannels, ...p } })),
  setPortfolio: (p) => set((s) => ({ portfolio: { ...s.portfolio, ...p } })),
  setOperations: (p) => set((s) => ({ operations: { ...s.operations, ...p } })),
  setMarket: (v) => set({ market: v }),
  setSector: (v) => set({ sector: v }),

  toggleWatchlistSymbol: (sym) =>
    set((s) => ({
      watchlist: s.watchlist.includes(sym)
        ? s.watchlist.filter((x) => x !== sym)
        : [...s.watchlist, sym],
    })),
  addWatchlistSymbol: (sym) =>
    set((s) => ({
      watchlist: s.watchlist.includes(sym) ? s.watchlist : [...s.watchlist, sym],
    })),
  updateSubscription: (plan) =>
    set((s) => ({ subscription: { ...s.subscription, plan } })),
}));

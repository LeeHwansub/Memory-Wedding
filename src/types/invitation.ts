export type AccountEntry = {
  relation: string;
  bankName: string;
  accountNumber: string;
};

export type Invitation = {
  id: number;
  projectId: number;
  title?: string | null;
  greetingMessage?: string | null;
  published: boolean;
  venueName?: string | null;
  venueAddress?: string | null;
  mapUrl?: string | null;
  accounts: AccountEntry[];
  groomName: string;
  brideName: string;
  weddingAt: string;
  slug: string;
  guestPath: string;
};

export type PublicInvitation = {
  title?: string | null;
  greetingMessage?: string | null;
  mapUrl?: string | null;
  accounts: AccountEntry[];
  groomName: string;
  brideName: string;
  weddingAt: string;
  venueName?: string | null;
  venueAddress?: string | null;
  slug: string;
};

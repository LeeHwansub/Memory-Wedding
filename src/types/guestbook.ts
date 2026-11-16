export type GuestbookEntry = {
  id: number;
  guestName: string;
  message: string;
  likeCount: number;
  likedByMe: boolean;
  mine: boolean;
  createdAt: string;
};

export type GuestbookPage = {
  content: GuestbookEntry[];
  myEntry: GuestbookEntry | null;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export const GUESTBOOK_PAGE_SIZES = [5, 10, 20] as const;
export type GuestbookPageSize = (typeof GUESTBOOK_PAGE_SIZES)[number];

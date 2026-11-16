export type BankOption = {
  code: string;
  name: string;
  short: string;
  color: string;
};

/** 국내 주요 은행 — 토스형 선택 UI용 */
export const BANKS: BankOption[] = [
  { code: "kb", name: "KB국민은행", short: "KB", color: "#756C4E" },
  { code: "shinhan", name: "신한은행", short: "신한", color: "#0046FF" },
  { code: "woori", name: "우리은행", short: "우리", color: "#0083CA" },
  { code: "hana", name: "하나은행", short: "하나", color: "#009591" },
  { code: "nh", name: "NH농협은행", short: "농협", color: "#1FAF4B" },
  { code: "ibk", name: "IBK기업은행", short: "기업", color: "#0055A5" },
  { code: "kakao", name: "카카오뱅크", short: "카뱅", color: "#FFE812" },
  { code: "toss", name: "토스뱅크", short: "토스", color: "#0064FF" },
  { code: "kbank", name: "케이뱅크", short: "케이", color: "#1A1A1A" },
  { code: "sc", name: "SC제일은행", short: "SC", color: "#0072CE" },
  { code: "citi", name: "씨티은행", short: "씨티", color: "#003B70" },
  { code: "post", name: "우체국", short: "우체", color: "#E60012" },
  { code: "saemaul", name: "새마을금고", short: "새마", color: "#1E9B5A" },
  { code: "shinhyup", name: "신협", short: "신협", color: "#0072BC" },
  { code: "suhyup", name: "수협은행", short: "수협", color: "#0066B3" },
  { code: "busan", name: "부산은행", short: "부산", color: "#8C2B3F" },
  { code: "daegu", name: "대구은행", short: "대구", color: "#0078C9" },
  { code: "kwangju", name: "광주은행", short: "광주", color: "#E87722" },
  { code: "jeonbuk", name: "전북은행", short: "전북", color: "#0054A6" },
  { code: "gyeongnam", name: "경남은행", short: "경남", color: "#004B87" },
  { code: "jeju", name: "제주은행", short: "제주", color: "#F15A22" },
];

export function findBankByName(name: string): BankOption | undefined {
  return BANKS.find((bank) => bank.name === name);
}

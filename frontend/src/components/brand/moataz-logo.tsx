import { cn } from "@/lib/utils";

export function MoatazMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 128 128"
      fill="none"
      aria-hidden="true"
      className={cn("shrink-0", className)}
    >
      <defs>
        <linearGradient id="moataz-mark-gradient" x1="18" y1="16" x2="110" y2="112" gradientUnits="userSpaceOnUse">
          <stop stopColor="#7857FF" />
          <stop offset="0.52" stopColor="#4E6BFF" />
          <stop offset="1" stopColor="#25D0E8" />
        </linearGradient>
      </defs>
      <rect x="8" y="8" width="112" height="112" rx="34" className="fill-[#0B1020] dark:fill-[#090D18]" />
      <path d="M33 87V43L51.5 66L64 49L76.5 66L95 43V87" stroke="url(#moataz-mark-gradient)" strokeWidth="10" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M29 34C46 20 82 16 103 36" stroke="url(#moataz-mark-gradient)" strokeWidth="5" strokeLinecap="round" opacity="0.82" />
      <circle cx="101" cy="37" r="6" fill="#55E6B5" />
    </svg>
  );
}

export function MoatazBrand({
  className,
  markClassName,
  showTagline = false,
}: {
  className?: string;
  markClassName?: string;
  showTagline?: boolean;
}) {
  return (
    <span className={cn("inline-flex min-w-0 items-center gap-2.5", className)} aria-label="moataz ai">
      <MoatazMark className={cn("size-8", markClassName)} />
      <span className="min-w-0 leading-none">
        <span className="block truncate text-[1.02rem] font-semibold tracking-[-0.025em]">moataz <span className="moataz-gradient-text">ai</span></span>
        {showTagline && (
          <span className="text-muted-foreground mt-1 block truncate text-[0.66rem] font-medium tracking-[0.04em]">intelligence in motion</span>
        )}
      </span>
    </span>
  );
}

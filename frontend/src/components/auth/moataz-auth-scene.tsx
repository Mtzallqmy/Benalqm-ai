import { MoatazBrand } from "@/components/brand/moataz-logo";

export function MoatazAuthScene({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-background px-4 py-10">
      <div className="moataz-grid pointer-events-none absolute inset-0 opacity-55" />
      <div className="pointer-events-none absolute left-1/2 top-[-14rem] h-[34rem] w-[34rem] -translate-x-1/2 rounded-full bg-[radial-gradient(circle,rgba(120,87,255,.28),rgba(37,208,232,.08)_46%,transparent_70%)] blur-3xl" />
      <div data-moataz-surface="glass" className="relative z-10 w-full max-w-md space-y-6 rounded-[2rem] p-7 sm:p-8">
        <div className="text-center">
          <MoatazBrand className="justify-center" markClassName="size-10" />
          <h1 className="mt-5 text-2xl font-semibold tracking-[-0.03em]">{title}</h1>
          {description && <p className="text-muted-foreground mt-2 text-sm leading-6">{description}</p>}
        </div>
        {children}
      </div>
    </div>
  );
}

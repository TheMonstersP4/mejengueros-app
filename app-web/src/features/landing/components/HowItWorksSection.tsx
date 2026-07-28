import { landingSteps } from '../data/landing-content';

export function HowItWorksSection() {
  return (
    <section id="como-funciona" className="px-6 pb-16 md:px-14 md:pb-[72px]">
      <div className="mb-10 h-1 w-full rounded bg-lime shadow-[0_0_16px_rgba(195,244,0,.5)]" />
      <div className="flex flex-col gap-3 md:flex-row md:items-baseline md:justify-between">
        <h2 className="font-display text-[40px] uppercase text-ink">Cómo funciona</h2>
        <span className="text-sm font-semibold uppercase tracking-[.06em] text-faint">
          Del sofá a la cancha
        </span>
      </div>
      <div className="mt-9 grid gap-6 md:grid-cols-3 md:gap-0">
        {landingSteps.map((step, index) => (
          <article
            key={step.number}
            className={[
              'rounded-2xl border border-white/10 bg-surface/70 p-6 md:rounded-none md:border-y-0 md:border-l-0 md:bg-transparent',
              index < landingSteps.length - 1 ? 'md:border-r' : 'md:border-r-0',
            ].join(' ')}
          >
            <span className="font-display text-[44px] text-lime">{step.number}</span>
            <h3 className="mt-2 text-[22px] font-bold text-ink">{step.title}</h3>
            <p className="mt-1 text-[15px] leading-6 text-muted">{step.body}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

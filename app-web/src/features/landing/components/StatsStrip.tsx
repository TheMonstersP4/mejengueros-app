import { landingStats } from '../data/landing-content';

export function StatsStrip() {
  return (
    <dl className="mx-auto mt-12 flex max-w-2xl flex-col items-center justify-center gap-6 pb-14 text-center sm:flex-row sm:gap-10 md:pb-16">
      {landingStats.map((stat, index) => (
        <div
          key={stat.label}
          className={[
            'min-w-28',
            index > 0 ? 'sm:border-l sm:border-white/15 sm:pl-10' : '',
          ].join(' ')}
        >
          <dt
            className={[
              'font-display text-4xl md:text-[40px]',
              stat.accent ? 'text-lime' : 'text-ink',
            ].join(' ')}
          >
            {stat.value}
          </dt>
          <dd className="mt-1 text-[13px] uppercase tracking-[.06em] text-muted">
            {stat.label}
          </dd>
        </div>
      ))}
    </dl>
  );
}

import { Icon } from '../../../shared/components/Icon';
import { ownerBenefits, playerBenefits } from '../data/landing-content';

type BenefitItem = {
  icon: string;
  text: string;
};

type BenefitsColumnProps = {
  kicker: string;
  accentClassName: string;
  items: readonly BenefitItem[];
};

function BenefitsColumn({ kicker, accentClassName, items }: BenefitsColumnProps) {
  return (
    <section aria-labelledby={`${kicker}-title`}>
      <h2
        id={`${kicker}-title`}
        className={`mb-4 text-[13px] font-bold uppercase tracking-[.14em] ${accentClassName}`}
      >
        {kicker}
      </h2>
      <ul className="flex flex-col gap-3">
        {items.map((item) => (
          <li key={item.text} className="flex items-center gap-3 rounded-xl bg-surfaceAlt p-4">
            <Icon name={item.icon} size={22} className={accentClassName} />
            <span className="text-base text-ink">{item.text}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export function BenefitsSection() {
  return (
    <div className="grid gap-6 px-6 pb-16 md:grid-cols-2 md:px-14 md:pb-[72px]">
      <BenefitsColumn
        kicker="Beneficios · jugadores"
        accentClassName="text-lime"
        items={playerBenefits}
      />
      <BenefitsColumn
        kicker="Beneficios · dueños"
        accentClassName="text-owner"
        items={ownerBenefits}
      />
    </div>
  );
}

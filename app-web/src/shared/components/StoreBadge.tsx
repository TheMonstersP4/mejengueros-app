import { Icon } from './Icon';

type StoreBadgeProps = {
  icon: string;
  kicker: string;
  label: string;
  href: string;
};

export function StoreBadge({ icon, kicker, label, href }: StoreBadgeProps) {
  return (
    <a
      href={href}
      className="inline-flex h-14 items-center gap-3 rounded-xl bg-limeInk px-7 text-left no-underline transition hover:-translate-y-0.5 hover:bg-black focus:outline-none focus-visible:ring-2 focus-visible:ring-lime focus-visible:ring-offset-2 focus-visible:ring-offset-lime"
    >
      <Icon name={icon} size={28} className="material-symbols-rounded text-white" />
      <span className="leading-none">
        <span className="block text-[11px] text-lime">{kicker}</span>
        <strong className="block text-lg text-white">{label}</strong>
      </span>
    </a>
  );
}

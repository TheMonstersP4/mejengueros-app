import type { AnchorHTMLAttributes, ButtonHTMLAttributes, PropsWithChildren } from 'react';

type BaseButtonProps = PropsWithChildren<{
  variant?: 'primary' | 'secondary';
  size?: 'md' | 'lg';
  className?: string;
}>;

type ButtonProps = BaseButtonProps & ButtonHTMLAttributes<HTMLButtonElement>;
type ButtonLinkProps = BaseButtonProps & AnchorHTMLAttributes<HTMLAnchorElement>;

const sizeClassName = {
  md: 'h-11 px-6 text-[15px]',
  lg: 'h-14 px-8 text-[17px]',
};

const variantClassName = {
  primary: 'border-transparent bg-lime text-limeInk shadow-lime hover:brightness-110',
  secondary:
    'border-white/25 bg-black/30 text-ink hover:border-lime/70 hover:text-lime',
};

function getButtonClassName({
  variant = 'primary',
  size = 'md',
  className = '',
}: BaseButtonProps) {
  return [
    'inline-flex items-center justify-center gap-2 rounded-full border font-display uppercase tracking-[.02em] transition duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-lime focus-visible:ring-offset-2 focus-visible:ring-offset-pitch',
    sizeClassName[size],
    variantClassName[variant],
    className,
  ]
    .filter(Boolean)
    .join(' ');
}

export function Button({ variant, size, className, children, ...props }: ButtonProps) {
  return (
    <button className={getButtonClassName({ variant, size, className })} {...props}>
      {children}
    </button>
  );
}

export function ButtonLink({ variant, size, className, children, ...props }: ButtonLinkProps) {
  return (
    <a className={getButtonClassName({ variant, size, className })} {...props}>
      {children}
    </a>
  );
}

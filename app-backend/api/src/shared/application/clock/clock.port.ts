export interface IClock {
  now(): Date;
}

export const CLOCK = Symbol('CLOCK');

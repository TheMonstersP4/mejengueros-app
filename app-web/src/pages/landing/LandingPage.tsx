import { AudiencePathsSection } from '../../features/landing/components/AudiencePathsSection';
import { BenefitsSection } from '../../features/landing/components/BenefitsSection';
import { DownloadCta } from '../../features/landing/components/DownloadCta';
import { HeroSection } from '../../features/landing/components/HeroSection';
import { HowItWorksSection } from '../../features/landing/components/HowItWorksSection';
import { LandingFooter } from '../../features/landing/components/LandingFooter';

export function LandingPage() {
  return (
    <main className="min-h-screen bg-pitch p-4 text-ink md:p-8 lg:p-14">
      <div className="mx-auto max-w-[1280px] overflow-hidden rounded-[26px] border border-white/10 bg-pitch shadow-panel">
        <HeroSection />
        <AudiencePathsSection />
        <HowItWorksSection />
        <BenefitsSection />
        <DownloadCta />
        <LandingFooter />
      </div>
    </main>
  );
}

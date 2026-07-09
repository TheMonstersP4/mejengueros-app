import type { Config } from 'tailwindcss';

const config = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        pitch: '#08090A',
        surface: '#111415',
        surfaceAlt: '#191C1D',
        ink: '#E1E3E4',
        muted: '#C4C9AC',
        faint: '#8E9379',
        lime: '#C3F400',
        limeInk: '#161E00',
        owner: '#C4C5DD',
      },
      fontFamily: {
        display: ['Anton', 'system-ui', 'sans-serif'],
        body: ['Archivo Narrow', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        lime: '0 0 30px rgba(195, 244, 0, .42)',
        panel: '0 40px 120px rgba(0, 0, 0, .65)',
      },
      keyframes: {
        live: {
          '0%': { boxShadow: '0 0 0 0 rgba(195,244,0,.55)' },
          '70%': { boxShadow: '0 0 0 8px rgba(195,244,0,0)' },
          '100%': { boxShadow: '0 0 0 0 rgba(195,244,0,0)' },
        },
      },
      animation: {
        live: 'live 1.6s ease infinite',
      },
    },
  },
  plugins: [],
} satisfies Config;

export default config;

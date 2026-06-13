// SiseRadar — merge this into the frontend's tailwind.config.{js,ts}.
// Enables class-based dark mode and exposes the brand palette/fonts as utilities
// (e.g. text-siseradar-teal, bg-up, font-mono-ticker, rounded-sr).
/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ['class', '[data-theme="dark"]'],
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        siseradar: {
          navy: '#0E1B2E',
          'navy-soft': '#13243B',
          teal: '#14C2B2',
          'teal-deep': '#0B8276',
          'teal-bright': '#39E0CE',
          'teal-tint': '#BDEDE7',
          slate: '#5B6B7F',
          mist: '#EAF0F4',
          cloud: '#F6F9FB',
        },
        // price direction — Korean convention: up = red, down = blue
        up: '#E5484D',
        down: '#2F6FED',
      },
      fontFamily: {
        sans: ['Pretendard', 'Apple SD Gothic Neo', 'sans-serif'],
        'mono-ticker': ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
      borderRadius: {
        sr: '12px',
        'sr-lg': '16px',
      },
    },
  },
  plugins: [],
};

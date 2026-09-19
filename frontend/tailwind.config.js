/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        background: 'hsl(0 0% 100%)',
        foreground: 'hsl(222 47% 11%)',
        muted: { DEFAULT: 'hsl(210 40% 96%)', foreground: 'hsl(215 16% 47%)' },
        border: 'hsl(214 32% 91%)',
        primary: { DEFAULT: 'hsl(221 83% 53%)', foreground: 'hsl(0 0% 100%)' },
        accent: { DEFAULT: 'hsl(210 40% 96%)', foreground: 'hsl(222 47% 11%)' },
        // Calm Learning AI palette: warm ivory surfaces, deep forest identity.
        cream: {
          DEFAULT: '#F5F1E8',
          soft: '#FAF7F0',
          deep: '#ECE7D9',
          card: '#FFFDF7',
        },
        forest: {
          50: '#F2F6F1',
          100: '#E0EBDD',
          200: '#C2D7BE',
          300: '#99BC94',
          400: '#6F9D6B',
          500: '#4F7F4C',
          600: '#3D653C',
          700: '#2F4F30',
          800: '#273F28',
          900: '#1F3320',
          950: '#101B11',
        },
        sage: {
          50: '#F1F5F0',
          100: '#E2ECE1',
          200: '#C5D9C4',
          300: '#A0BFA2',
          400: '#7AA37E',
          500: '#5F8764',
          600: '#4C6C51',
          700: '#3E5843',
          800: '#344838',
          900: '#2C3C2F',
        },
        moss: {
          50: '#F4F7F2',
          100: '#E5EEE2',
          200: '#CBDDC7',
        },
        teal: {
          soft: '#E4F0EE',
        },
        clay: {
          soft: '#F3E9DC',
        },
        ink: {
          DEFAULT: '#29261F',
          soft: '#4A463B',
          faint: '#7C7666',
        },
        bark: {
          DEFAULT: '#1B1713',
          soft: '#26211B',
          card: '#2A241D',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', '"Segoe UI"', 'Roboto', '"Helvetica Neue"', 'Arial', 'sans-serif'],
      },
      fontSize: {
        page: ['1.9rem', { lineHeight: '2.4rem', letterSpacing: '-0.02em', fontWeight: '650' }],
        section: ['1.1rem', { lineHeight: '1.6rem', letterSpacing: '-0.01em', fontWeight: '600' }],
      },
      boxShadow: {
        soft: '0 1px 2px rgba(62, 53, 40, 0.05), 0 10px 28px -16px rgba(62, 53, 40, 0.18)',
        lift: '0 2px 4px rgba(62, 53, 40, 0.06), 0 16px 36px -16px rgba(62, 53, 40, 0.22)',
        composer: '0 2px 6px rgba(47, 79, 48, 0.08), 0 18px 44px -18px rgba(47, 79, 48, 0.28)',
        card: '0 1px 2px rgba(62, 53, 40, 0.04), 0 12px 32px -20px rgba(62, 53, 40, 0.25)',
      },
      borderRadius: { lg: '0.625rem', md: '0.5rem', sm: '0.375rem' },
    },
  },
  plugins: [],
};

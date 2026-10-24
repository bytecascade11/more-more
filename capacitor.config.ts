import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.moremore.shield',
  appName: 'Shield',
  webDir: 'dist',
  android: {
    allowMixedContent: false,
  },
};

export default config;

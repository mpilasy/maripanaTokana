const path = require('path');
const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');

// The shared/i18n directory lives outside react-native/ at the project root.
// Metro needs to be told to watch and resolve modules from there.
const projectRoot = __dirname;
const sharedRoot = path.resolve(__dirname, '..');

const config = {
  watchFolders: [sharedRoot],
  resolver: {
    // Allow Metro to resolve modules from the project root (for shared/i18n)
    nodeModulesPaths: [
      path.resolve(projectRoot, 'node_modules'),
    ],
    // Ensure shared/ is included in the module resolution
    extraNodeModules: {
      shared: path.resolve(sharedRoot, 'shared'),
    },
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);

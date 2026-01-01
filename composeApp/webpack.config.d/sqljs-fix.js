const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');

config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                from: path.resolve(__dirname, '../../node_modules/sql.js/dist/sql-wasm.wasm'),
                to: 'sql-wasm.wasm'
            },
            {
                from: path.resolve(__dirname, '../../node_modules/sql.js/dist/sql-wasm.wasm'),
                to: 'sqljs.worker.js/sql-wasm.wasm'
            }
        ]
    })
);

config.resolve.fallback = {
    "fs": false,
    "path": false,
    "crypto": false
};

// Suppress "Critical dependency: the request of a dependency is an expression"
// which comes from sql.js dynamic loading
if (!config.ignoreWarnings) config.ignoreWarnings = [];
config.ignoreWarnings.push(/Critical dependency: the request of a dependency is an expression/);
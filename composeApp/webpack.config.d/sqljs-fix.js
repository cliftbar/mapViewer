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
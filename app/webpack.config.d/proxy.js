if (config.devServer) {
    config.devServer.proxy = {
        '/api/clover': {
            target: 'https://apisandbox.dev.clover.com',
            pathRewrite: { '^/api/clover': '' },
            changeOrigin: true,
            secure: false
        }
    };
}

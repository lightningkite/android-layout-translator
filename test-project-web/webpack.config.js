const path = require('path');
const webpack = require('webpack')
const CopyPlugin = require("copy-webpack-plugin");
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

module.exports = {
  // plugins: [
  //   new BundleAnalyzerPlugin()
  // ],
  entry: './src/index.ts',
  devtool: 'inline-source-map',
  plugins: [
    new webpack.EnvironmentPlugin({
      API_URL: 'http://localhost:8000/',
      WEBSOCKET_URL: 'ws://localhost:8000/api/v2/ws/?token=',
    }),
    new CopyPlugin({
      patterns: [
        {
          context: 'src/',
          from: "**/*.{html,css,jpg,png,svg,js,mp3}",
          to: "[path][name][ext]",
          // globOptions: {
          //   ignore: 'index.html'
          // }
        },
      ],
    }),
  ],
  module: {
    rules: [
      // all files with a `.ts` or `.tsx` extension will be handled by `ts-loader`
      { test: /\.tsx?$/, loader: "ts-loader" },
      {
        test: /\.s[ac]ss$/i,
        use: [
          // 'resolve-url-loader',
          'style-loader',
          'css-loader',
          'sass-loader',
        ],
      },
      {
        test: /\.(png|jpe?g|gif)$/i,
        use: [
          {
            loader: 'file-loader',
          },
        ],
      },
      {
        test: /\.html$/i,
        loader: 'html-loader',
        options: {
          // Disables attributes processing
          sources: false,
        },
      },
      {
        test: /\.(yaml|ogg|mp3|png|jpg|gif|ttf|eot|svg|woff(2)?)(\?[a-z0-9]+)?$/,
        exclude: /node_modules/,
        loader: 'file-loader',
      },
    ],
  },
  resolve: {
    extensions: ['.ts', '.js', '.html', '.scss']
  },
  output: {
    filename: 'index.js',
    path: path.resolve(__dirname, 'dist'),
  },
  devServer: {
    contentBase: './dist',
    publicPath: process.env.DEV_PUBLIC_PATH || '/',
    public: process.env.DEV_PUBLIC,// || 'dirs.localhost',
    disableHostCheck: process.env.DEV_HOST_CHECK || true,
    host: process.env.DEV_HOST || '127.0.0.1',
    port: process.env.DEV_PORT || '8080',
    historyApiFallback: true,
  }
};

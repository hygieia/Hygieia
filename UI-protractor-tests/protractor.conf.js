
exports.config = {

    baseUrl: 'https://hygieia-qa.cloud.capitalone.com/',

    allScriptsTimeout: 110000,

    disableChecks: true,

    directConnect: true,

    ignoreUncaughtExceptions: true,

    framework: 'custom',

    frameworkPath: require.resolve('serenity-js'),

    specs: [ 'src/main/js/features/**/*.feature' ],

    cucumberOpts: {
        require:    [ 'src/main/js/step_definitions/*.steps.js' ],
        format:     'pretty'
    },

    capabilities: {
        browserName: 'chrome',

        // firefoxOptions: {
        //     binary: '/usr/bin/firefox'
        // }
        chromeOptions: {
            binary: 'node_modules/protractor/node_modules/webdriver-manager/selenium/chromedriver_2.28',
            // args: [ "--headless", "--disable-gpu", "--window-size=1600,1200" ]
            // args: [ "--start-maximized" ]
        }
    }
};

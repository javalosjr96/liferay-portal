import {spawn} from 'child_process';
import {test} from '@playwright/test';

const antDir = __dirname.toString() + "/ant/";

async function runAntTask(buildFile, taskName) {
    return new Promise((resolve, reject) => {
        const antProcess = spawn('ant',
            ['-f',antDir + buildFile,taskName], {
            cwd: process.cwd(),
            env: process.env,
        });

        antProcess.stdout.on('data', (data) => {
            console.log(`${data.toString()}`);
        });

        antProcess.stderr.on('data', (data) => {
            console.error(`${data.toString()}`);
        });

        antProcess.on('close', (code) => {
            if (code === 0) {
                resolve(`Ant Task "${taskName}" completed successfully.`);
            } else {
                reject(new Error(`Ant Task "${taskName}" failed with exit code: ${code}`));
            }
        });
    });
}

test('CheckUpgradeClientAdditionalSettings', async ({page}) => {

    const envVars = process.env;

    // Loop through each key-value pair and print
    for (const [key, value] of Object.entries(envVars)) {
        console.log(`${key}: ${value}`);
    }

    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-client-additional-settings');
});

test('CheckUpgradeClientCustomLog', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-client-custom-log');
});

test('CheckUpgradeClientGogoShell', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'execute-upgrade-client-gogoshell');
});

test('CheckUpgradeClientGogoShellCommandOutput', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-client-gogoshell-command-output');
});

test('CheckUpgradeClientGogoShellHelpOutput', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-client-gogoshell-help-output');
});

test('CheckUpgradeClientHelp', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-client-help');
});

test('CheckUpgradeClientSecondProcess', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-client-second-process');
});
test('CheckUpgradeClientShDisconnect', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-client-sh-disconnect');
});
test('CheckUpgradeClientZipContents', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-client-zip-content');
});
test('CheckUpgradeDebugOptions', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-debug-options');
});
test('CheckUpgradePropertiesAppDBSet', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-properties-app-db-set');
});
test('CheckUpgradePropertiesAppExtSet', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-properties-app-ext-set');
});
test('CheckUpgradePropertiesDBExtSet', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-properties-db-ext-set');
});
test('CheckUpgradePropertiesNoneSet', async ({page}) => {
    await runAntTask(
         'build-test-db-upgrade-client.xml',
        'check-upgrade-properties-none-set');
});
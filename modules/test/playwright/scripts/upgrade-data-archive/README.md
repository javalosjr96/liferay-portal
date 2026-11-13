# Upgrade Data Archives

An upgrade data archive is a database dump plus the `data` folder of the bundle that produced it. The upgrade projects under `tests/portal-db-infrastructure` restore an archive, upgrade it to the current portal, and check that the data survived. This folder generates archives through the headless API instead of a Poshi run, so a team adds its data by writing TypeScript against `ApiHelpers`.

## Layout

```
scripts/upgrade-data-archive
├── archives
│   ├── index.ts         # the registry: one entry per archive
│   └── <name>/index.ts  # one archive: feature flags, populate, verify
├── generate.sh          # empties data, starts the bundle, runs run.ts, stops the bundle
├── run.ts               # enables flags, populates, writes the manifest, dumps and zips
└── runner               # shared code; an archive never needs to change it
```

## Add an Archive

1. Create `archives/<name>/index.ts` and export an `UpgradeDataArchive` (`runner/upgradeDataArchive.ts`) as its default:

	| Field | Meaning |
	| --- | --- |
	| `name` | The archive name. The zip is `data-archive-<name>-<database type>.zip`. |
	| `featureFlags` | Keys enabled before `populate`. Each one is checked with `is-enabled`, and the run fails if the portal did not apply it. |
	| `populate(apiHelpers, manifest)` | Creates the data with `ApiHelpers` and records every entity with `manifest.add(kind, {externalReferenceCode, ...})`. Do not clean up: the data is the archive. |
	| `verify(apiHelpers, manifestData)` | Runs on the upgraded portal and reads every recorded entity back by its external reference code. |

1. Add it to the list in `archives/index.ts`.

1. Add an upgrade project, `tests/portal-db-infrastructure/upgrade-data-archive-<name>`, with a `config.ts` whose `testDir` is `tests/portal-db-infrastructure/upgrade-data-archive` and an `env/set_up.sh` that sets `DATA_ARCHIVE_TYPE="data-archive-<name>"`. Register it in `playwright.config.ts`. The shared spec needs no change: it finds the archive through the manifest the archive carries.

	When the archive also needs its own spec, such as a check that its pages render in a browser, put the spec in the project folder and set `testDir` to `tests/portal-db-infrastructure` with a `testMatch` that names both folders, as `upgrade-data-archive-content-pages/config.ts` does.

`archives/cms` is the reference: it creates a CMS space and a content page with a draft and an approved page specification.

## Generate an Archive

The bundle must point at an empty database, and the portal must be stopped. `generate.sh` deletes `${LIFERAY_HOME}/data`, so the archive holds only the files the new database references.

```bash
export APP_SERVER_TYPE=tomcat
export LIFERAY_HOME=<bundles>
export LIFERAY_PORTAL_URL=http://localhost:8080

scripts/upgrade-data-archive/generate.sh \
	--database-container <container> \
	--database-name lportal \
	--out <dir> \
	--type cms
```

`--database-type` defaults to `postgresql`, the only type dumped so far, and `--database-user` to `root`. The dump uses the same `docker exec` command as the `export-database` target in the legacy repository, while the portal is still up, and the zip has the layout `rebuild-legacy-database` reads: `<database type>.sql` at the root plus `data/**`, without `data/elasticsearch*`.

## Run the Upgrade Project Against a Local Archive

`rebuild_legacy_database` passes `LEGACY_DATABASE_FILE` to ant as `legacy.database.file`, so a local zip replaces the one in the legacy repository:

```bash
export LEGACY_DATABASE_FILE=<dir>/data-archive-cms-postgresql.zip
export PLAYWRIGHT_PROJECT_NAME=portal-db-infrastructure.upgrade-data-archive-cms

tests/portal-db-infrastructure/upgrade-data-archive-cms/env/set_up.sh

yarn test --project=portal-db-infrastructure.upgrade-data-archive-cms
```

`set_up.sh` fails on any `ERROR` or `FATAL` line in the upgrade log. The spec then fails when a table has fewer rows after the upgrade than before, as the upgrade report records them, unless the table is in `ROW_LOSS_ALLOWED_TABLE_NAMES`, and it runs the archive's `verify`. A table that an upgrade step drops counts as a loss too, so a table that is dropped on purpose belongs in that list. When the archive is already at the current schema, the upgrade runs no upgrade processes and writes no report, and the row check is skipped.

## Manifest Compatibility

An archive is generated once, on the release it represents, and verified by every later master. The manifest is what lets an old archive and a new `verify` agree. It is written to `data/upgrade-data-archive/<name>.json`, so it travels inside the zip and restores with the data.

```json
{
	"entities": {
		"space": [{"externalReferenceCode": "UPGRADE_DATA_ARCHIVE_CMS_SPACE", "name": "Upgrade Data Archive CMS"}]
	},
	"formatVersion": 1,
	"generatedAt": "2026-11-13T00:00:00.000Z",
	"portalBuildNumber": 7413,
	"type": "cms"
}
```

1. **Fields are only added.** A field is never removed or renamed. A rename is a new field, and the old one stays readable.

1. **Every field added after the first version is optional in `verify`.** An archive generated before the field existed does not have it.

1. **`verify` ignores what it does not know**: unknown entity kinds and unknown fields. An unknown archive `type` is different: it fails the test, because an archive whose `verify` was removed would otherwise pass while checking nothing.

1. **`externalReferenceCode` is required on every entity**, since it is how `verify` finds the entity after the upgrade. `manifest.add` refuses an entity without one.

1. **`formatVersion` changes only on a breaking change** to the shape above. `readManifests` refuses a version newer than it knows, and master keeps a reader for every version still present in a stored archive.
